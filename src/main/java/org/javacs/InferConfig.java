package org.javacs;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

class InferConfig
{
    private static final Logger LOG = Logger.getLogger("main");

    /** Root of the workspace that is currently open in VSCode */
    private final Path workspaceRoot;
    /** External dependencies specified manually by the user */
    private final Collection<String> externalDependencies;
    /** Location of the maven repository, usually ~/.m2 */
    private final Path mavenHome;
    /** Location of the gradle cache, usually ~/.gradle */
    private final Path gradleHome;

    InferConfig(Path workspaceRoot, Collection<String> externalDependencies, Path mavenHome, Path gradleHome) {
        this.workspaceRoot = workspaceRoot;
        this.externalDependencies = externalDependencies;
        this.mavenHome = mavenHome;
        this.gradleHome = gradleHome;
    }

    InferConfig(Path workspaceRoot, Collection<String> externalDependencies) {
        this(workspaceRoot, externalDependencies, defaultMavenHome(), defaultGradleHome());
    }

    InferConfig(Path workspaceRoot) {
        this(workspaceRoot, Collections.emptySet(), defaultMavenHome(), defaultGradleHome());
    }

    private static Path defaultMavenHome() {
        return Paths.get(System.getProperty("user.home")).resolve(".m2");
    }

    private static Path defaultGradleHome() {
        return Paths.get(System.getProperty("user.home")).resolve(".gradle");
    }

    /** Find .jar files for external dependencies, for examples maven dependencies in ~/.m2 or jars in bazel-genfiles */
    Set<Path> classPath() {
        // externalDependencies
        if (!externalDependencies.isEmpty()) {
            var result = new HashSet<Path>();
            for (var id : externalDependencies) {
                var a = Artifact.parse(id);
                var found = findAnyJar(a, false);
                if (found == NOT_FOUND) {
                    LOG.warning(String.format("Couldn't find jar for %s in %s or %s", a, mavenHome, gradleHome));
                    continue;
                }
                result.add(found);
            }
            return result;
        }

        // Maven
        var pomXml = workspaceRoot.resolve("pom.xml");
        if (Files.exists(pomXml)) {
            return mvnDependencies(pomXml, "dependency:list");
        }

        return Collections.emptySet();
    }

    /** Find source .jar files in local maven repository. */
    Set<Path> buildDocPath() {
        // externalDependencies
        if (!externalDependencies.isEmpty()) {
            var result = new HashSet<Path>();
            for (var id : externalDependencies) {
                var a = Artifact.parse(id);
                var found = findAnyJar(a, true);
                if (found == NOT_FOUND) {
                    LOG.warning(String.format("Couldn't find doc jar for %s in %s or %s", a, mavenHome, gradleHome));
                    continue;
                }
                result.add(found);
            }
            return result;
        }

        // Maven
        var pomXml = workspaceRoot.resolve("pom.xml");
        if (Files.exists(pomXml)) {
            return mvnDependencies(pomXml, "dependency:sources");
        }

        return Collections.emptySet();
    }

    private Path findAnyJar(Artifact artifact, boolean source) {
        Path maven = findMavenJar(artifact, source);
        return maven; // even if NOT_FOUND

        /*if (maven != NOT_FOUND) {
            return maven;
        } else return findGradleJar(artifact, source);*/
    }

    Path findMavenJar(Artifact artifact, boolean source) {
        var jar =
                mavenHome
                        .resolve("repository")
                        .resolve(artifact.groupId.replace('.', File.separatorChar))
                        .resolve(artifact.artifactId)
                        .resolve(artifact.version)
                        .resolve(fileName(artifact, source));
        if (!Files.exists(jar)) {
            LOG.warning(jar + " does not exist");
            return NOT_FOUND;
        }
        return jar;
    }

    // private Path findGradleJar(Artifact artifact, boolean source) {
    //     LOG.info("LSP - INFERCONFIG - findGradleJar");
    //     // Search for caches/modules-*/files-*/groupId/artifactId/version/*/artifactId-version[-sources].jar
    //     var base = gradleHome.resolve("caches");
    //     var pattern =
    //             "glob:"
    //                     + String.join(
    //                             File.separator,
    //                             base.toString(),
    //                             "modules-*",
    //                             "files-*",
    //                             artifact.groupId,
    //                             artifact.artifactId,
    //                             artifact.version,
    //                             "*",
    //                             fileName(artifact, source));
    //     var match = FileSystems.getDefault().getPathMatcher(pattern);

    //     try {
    //         return Files.walk(base, 7).filter(match::matches).findFirst().orElse(NOT_FOUND);
    //     } catch (IOException e) {
    //         throw new RuntimeException(e);
    //     }
    // }

    private String fileName(Artifact artifact, boolean source) {
        return artifact.artifactId + '-' + artifact.version + (source ? "-sources" : "") + ".jar";
    }

    static Set<Path> mvnDependencies(Path pomXml, String goal) {
        Objects.requireNonNull(pomXml, "pom.xml path is null");
        try {
            // TODO consider using mvn valide dependency:copy-dependencies -DoutputDirectory=??? instead
            // Run maven as a subprocess
            String[] command = {
                getMvnCommand(),
                "--batch-mode", // Turns off ANSI control sequences
                "validate",
                goal,
                "-DincludeScope=test",
                "-DoutputAbsoluteArtifactFilename=true",
            };
            var output = Files.createTempFile("java-language-server-maven-output", ".txt");
            LOG.info("Running " + String.join(" ", command) + " ...");
            var workingDirectory = pomXml.toAbsolutePath().getParent().toFile();
            var process =
                    new ProcessBuilder()
                            .command(command)
                            .directory(workingDirectory)
                            .redirectError(ProcessBuilder.Redirect.INHERIT)
                            .redirectOutput(output.toFile())
                            .start();
            // Wait for process to exit
            var result = process.waitFor();
            if (result != 0) {
                LOG.severe("`" + String.join(" ", command) + "` returned " + result);
                return Set.of();
            }
            // Read output
            var dependencies = new HashSet<Path>();
            for (var line : Files.readAllLines(output)) {
                var jar = readDependency(line);
                if (jar != NOT_FOUND) {
                    dependencies.add(jar);
                }
            }
            return dependencies;
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Pattern DEPENDENCY =
            Pattern.compile("^\\[INFO\\]\\s+(.*:.*:.*:.*:.*):(/.*?)( -- module .*)?$");

    static Path readDependency(String line) {
        var match = DEPENDENCY.matcher(line);
        if (!match.matches()) {
            return NOT_FOUND;
        }
        var artifact = match.group(1);
        var path = match.group(2);
        LOG.info(String.format("...%s => %s", artifact, path));
        return Paths.get(path);
    }

    static String getMvnCommand() {
        var mvnCommand = "mvn";
        if (File.separatorChar == '\\') {
            mvnCommand = findExecutableOnPath("mvn.cmd");
            if (mvnCommand == null) {
                mvnCommand = findExecutableOnPath("mvn.bat");
            }
        }
        return mvnCommand;
    }

    private static String findExecutableOnPath(String name) {
        for (var dirname : System.getenv("PATH").split(File.pathSeparator)) {
            var file = new File(dirname, name);
            if (file.isFile() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    private static final Path NOT_FOUND = Paths.get("");
}

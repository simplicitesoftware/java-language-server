![Simplicit&eacute; Software](https://platform.simplicite.io/logos/logo250.png)
***

Java Language Server
====================

Build
-----

	rm -fr target
	mvn clean package

Test
----

**TODO**

Publish
-------

	mvn deploy

**Note**: before publishing make sure you have defined the `central` server with credentials in yous Maven's `setting.xml`, e.g.:

```text
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
(...)
  <servers>
    <server>
      <id>central</id>
      <username><your user token name></username>
      <password><your user token></password>
    </server>
  </servers>
(...)
</settings>
```

Such credentials can be generated on your [Sonatype account page](https://central.sonatype.com/account)

If everything works fine you should see the deployement on your [Sonatype publishing page](https://central.sonatype.com/publishing/deployment]
and you can publish it.
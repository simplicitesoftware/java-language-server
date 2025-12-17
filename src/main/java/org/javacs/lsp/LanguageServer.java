package org.javacs.lsp;

import java.util.List;
import java.util.Optional;

public class LanguageServer {
    public InitializeResult initialize(InitializeParams params) {
        throw new RuntimeException("Unimplemented");
    }

    public void initialized() {
        throw new RuntimeException("Unimplemented");
    }

    public void shutdown() {
        throw new RuntimeException("Unimplemented");
    }

    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        throw new RuntimeException("Unimplemented");
    }

    public void didOpenTextDocument(DidOpenTextDocumentParams params) {
        throw new RuntimeException("Unimplemented");
    }

    public void didChangeTextDocument(DidChangeTextDocumentParams params) {
        throw new RuntimeException("Unimplemented");
    }

    public void didSaveTextDocument(DidSaveTextDocumentParams params) {
        throw new RuntimeException("Unimplemented");
    }

    public void didCloseTextDocument(DidCloseTextDocumentParams params) {
        throw new RuntimeException("Unimplemented");
    }

    public Optional<CompletionList> completion(TextDocumentPositionParams params) {
        throw new RuntimeException("Unimplemented");
    }

    public CompletionItem resolveCompletionItem(CompletionItem params) {
        throw new RuntimeException("Unimplemented");
    }

    public Optional<Hover> hover(TextDocumentPositionParams params) {
        throw new RuntimeException("Unimplemented");
    }

    public Optional<SignatureHelp> signatureHelp(TextDocumentPositionParams params) {
        throw new RuntimeException("Unimplemented");
    }

    public List<CodeAction> codeAction(CodeActionParams params) {
        throw new RuntimeException("Unimplemented");
    }

    public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
        throw new RuntimeException("Unimplemented");
    }

    public void doAsyncWork() {}
}

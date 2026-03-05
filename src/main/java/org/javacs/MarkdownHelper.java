package org.javacs;

import java.nio.CharBuffer;
import java.util.StringJoiner;
import java.util.logging.Logger;
import org.javacs.lsp.MarkupContent;
import org.javacs.lsp.MarkupKind;

import com.sun.source.doctree.DocCommentTree;

public class MarkdownHelper {

	public static MarkupContent asMarkupContent(DocCommentTree comment) {
		var content = new MarkupContent();
		content.kind = MarkupKind.Markdown;
		content.value = asMarkdown(comment);
		return content;
	}

	public static String asMarkdown(DocCommentTree comment) {
		try {
			var join = new StringJoiner("");
			for (var l : comment.getFullBody()) {
				join.add(l.toString());
			}
			return normalizeJavadocHtml(replaceTags(join.toString()));
		} catch (RuntimeException e) {
			// Return raw content instead of error message
			return comment.getFullBody().stream()
				.map(Object::toString)
				.collect(java.util.stream.Collectors.joining("\n"));
		}
	}

	private static String normalizeJavadocHtml(String html) {
		html = html.replaceAll("<div[^>]*>", "").replaceAll("</div>", "");

		html = html.replaceAll("<br\\s*/?>", "\n");

		html = html.replaceAll("(?s)<li>(.*?)</li>", "\n- $1");
		html = html.replaceAll("\\s*</?[uUoO][lL]>\\s*", "\n\n");

		html = html.replaceAll("(?m)^[ \\t]+$", "");
		html = html.replaceAll("\\n{3,}", "\n\n");

		return html.strip();
	}

	private static void check(CharBuffer in, char expected) {
		var head = in.get();
		if (head != expected) {
			throw new RuntimeException(String.format("want `%s` got `%s`", expected, head));
		}
	}

	private static boolean empty(CharBuffer in) {
		return in.position() == in.limit();
	}

	private static char peek(CharBuffer in) {
		return in.get(in.position());
	}

	private static String parseTag(CharBuffer in) {
		check(in, '@');
		var tag = new StringBuilder();
		while (!empty(in) && Character.isAlphabetic(peek(in))) {
			tag.append(in.get());
		}
		return tag.toString();
	}

	private static void parseBlock(CharBuffer in, StringBuilder out) {
		check(in, '{');
		if (peek(in) == '@') {
			var tag = parseTag(in);
			if (peek(in) == ' ') {
				in.get();
			}
			switch (tag) {
			case "code":
			case "link":
			case "linkplain":
				out.append("`");
				parseInner(in, out);
				out.append("`");
				break;
			case "literal":
				parseInner(in, out);
				break;
			default:
				LOG.warning(String.format("Unknown tag `@%s`", tag));
				parseInner(in, out);
			}
		} else {
			parseInner(in, out);
		}
		check(in, '}');
	}

	private static void parseInner(CharBuffer in, StringBuilder out) {
		while (!empty(in)) {
			switch (peek(in)) {
			case '{':
				parseBlock(in, out);
				break;
			case '}':
				return;
			default:
				out.append(in.get());
			}
		}
	}

	private static void parse(CharBuffer in, StringBuilder out) {
		while (!empty(in)) {
			parseInner(in, out);
		}
	}

	private static String replaceTags(String in) {
		var out = new StringBuilder();
		parse(CharBuffer.wrap(in), out);
		return out.toString();
	}

	private static final Logger LOG = Logger.getLogger("main");
}

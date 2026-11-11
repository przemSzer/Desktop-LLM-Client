package dev.local.ai.ui.chat.controls;

import java.util.ArrayList;
import java.util.List;

/**
 * Lifts TeX out of Markdown before flexmark runs, then puts delimiters KaTeX
 * auto-render understands back into the HTML.
 * <p>
 * A single {@code $...$} pair that looks like a symbol ({@code $r$}, {@code $V_k$},
 * {@code $\rightarrow$}) is rewritten to {@code \(...\)}. A bare amount such as
 * {@code $100$} stays in the text.
 */
final class KatexMathProtector {

    private static final String TOKEN_PREFIX = "KATEXBLOCK";
    private static final String TOKEN_SUFFIX = "END";

    private KatexMathProtector() {
    }

    record Protection(String markdown, List<String> blocks) {
    }

    static Protection protect(String markdown) {
        List<String> blocks = new ArrayList<>();
        StringBuilder out = new StringBuilder(markdown.length());
        int i = 0;
        int length = markdown.length();
        while (i < length) {
            if (isFenceOpen(markdown, i)) {
                int fenceEnd = findClosingFence(markdown, i);
                int end = fenceEnd < 0 ? length : fenceEnd;
                out.append(markdown, i, end);
                i = end;
                continue;
            }
            if (markdown.charAt(i) == '`') {
                int end = skipInlineCode(markdown, i);
                out.append(markdown, i, end);
                i = end;
                continue;
            }
            String opener = openerAt(markdown, i);
            if (opener != null) {
                String closer = closerFor(opener);
                int end = markdown.indexOf(closer, i + opener.length());
                if (end >= 0) {
                    remember(blocks, out, markdown.substring(i, end + closer.length()));
                    i = end + closer.length();
                    continue;
                }
            }
            int inlineEnd = protectSingleDollar(markdown, i, out, blocks);
            if (inlineEnd >= 0) {
                i = inlineEnd;
                continue;
            }
            out.append(markdown.charAt(i));
            i++;
        }
        return new Protection(out.toString(), List.copyOf(blocks));
    }

    static String restore(String html, List<String> blocks) {
        String result = html;
        for (int i = 0; i < blocks.size(); i++) {
            result = result.replace(TOKEN_PREFIX + i + TOKEN_SUFFIX, escapeHtml(blocks.get(i)));
        }
        return result;
    }

    /**
     * @return index after the closing {@code $}, or {@code -1} when this dollar is not inline math
     */
    private static int protectSingleDollar(String markdown, int index, StringBuilder out, List<String> blocks) {
        if (markdown.charAt(index) != '$') {
            return -1;
        }
        if (index > 0 && markdown.charAt(index - 1) == '\\') {
            return -1;
        }
        if (index + 1 < markdown.length() && markdown.charAt(index + 1) == '$') {
            return -1;
        }
        int close = markdown.indexOf('$', index + 1);
        if (close < 0) {
            return -1;
        }
        int newline = markdown.indexOf('\n', index + 1);
        if (newline >= 0 && newline < close) {
            return -1;
        }
        if (close + 1 < markdown.length() && markdown.charAt(close + 1) == '$') {
            return -1;
        }
        String body = markdown.substring(index + 1, close);
        if (!isSymbolMath(body)) {
            out.append(markdown, index, close + 1);
            return close + 1;
        }
        remember(blocks, out, "\\(" + body + "\\)");
        return close + 1;
    }

    /**
     * Symbol or command, not a price. {@code $r$} and {@code $\text{ zł}$} qualify.
     * {@code $100$} and {@code $nominalna stopa$} do not.
     */
    private static boolean isSymbolMath(String body) {
        String trimmed = body.trim();
        if (trimmed.isEmpty() || trimmed.matches("[0-9][0-9\\s.,]*")) {
            return false;
        }
        if (trimmed.indexOf('\\') >= 0) {
            return true;
        }
        return trimmed.indexOf(' ') < 0 && trimmed.indexOf('\t') < 0;
    }

    private static void remember(List<String> blocks, StringBuilder out, String latex) {
        blocks.add(latex);
        out.append(TOKEN_PREFIX).append(blocks.size() - 1).append(TOKEN_SUFFIX);
    }

    private static String openerAt(String markdown, int index) {
        if (markdown.startsWith("$$", index)) {
            return "$$";
        }
        if (markdown.startsWith("\\[", index)) {
            return "\\[";
        }
        if (markdown.startsWith("\\(", index)) {
            return "\\(";
        }
        return null;
    }

    private static String closerFor(String opener) {
        return switch (opener) {
            case "$$" -> "$$";
            case "\\[" -> "\\]";
            case "\\(" -> "\\)";
            default -> throw new IllegalArgumentException(opener);
        };
    }

    private static boolean isFenceOpen(String markdown, int index) {
        if (!markdown.startsWith("```", index)) {
            return false;
        }
        return index == 0 || markdown.charAt(index - 1) == '\n';
    }

    /** Index just after the closing fence line, or {@code -1} when the fence is unclosed. */
    private static int findClosingFence(String markdown, int open) {
        int lineEnd = markdown.indexOf('\n', open);
        int searchFrom = lineEnd < 0 ? markdown.length() : lineEnd + 1;
        for (int j = searchFrom; j < markdown.length(); j++) {
            if (markdown.startsWith("```", j) && markdown.charAt(j - 1) == '\n') {
                int end = markdown.indexOf('\n', j);
                return end < 0 ? markdown.length() : end + 1;
            }
        }
        return -1;
    }

    private static int skipInlineCode(String markdown, int index) {
        int ticks = 0;
        while (index + ticks < markdown.length() && markdown.charAt(index + ticks) == '`') {
            ticks++;
        }
        int close = markdown.indexOf("`".repeat(ticks), index + ticks);
        if (close < 0) {
            return markdown.length();
        }
        return close + ticks;
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}

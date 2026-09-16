package dev.local.ai.ui.chat.controls;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts lightweight inline LaTeX (common in Gemma and similar models) to Unicode
 * before Markdown rendering. Full math typesetting is not supported in chat WebView.
 */
final class InlineLatexSimplifier {

    private static final Map<String, String> SYMBOLS = Map.ofEntries(
            Map.entry("rightarrow", "→"),
            Map.entry("leftarrow", "←"),
            Map.entry("leftrightarrow", "↔"),
            Map.entry("longrightarrow", "→"),
            Map.entry("longleftarrow", "←"),
            Map.entry("Rightarrow", "⇒"),
            Map.entry("Leftarrow", "⇐"),
            Map.entry("Leftrightarrow", "⇔"),
            Map.entry("to", "→"),
            Map.entry("gets", "←"),
            Map.entry("mapsto", "↦"),
            Map.entry("leq", "≤"),
            Map.entry("le", "≤"),
            Map.entry("geq", "≥"),
            Map.entry("ge", "≥"),
            Map.entry("neq", "≠"),
            Map.entry("ne", "≠"),
            Map.entry("approx", "≈"),
            Map.entry("sim", "∼"),
            Map.entry("equiv", "≡"),
            Map.entry("infty", "∞"),
            Map.entry("times", "×"),
            Map.entry("cdot", "·"),
            Map.entry("pm", "±"),
            Map.entry("mp", "∓"),
            Map.entry("ldots", "…"),
            Map.entry("dots", "…"),
            Map.entry("checkmark", "✓"),
            Map.entry("bullet", "•"),
            Map.entry("degree", "°"),
            Map.entry("textdegree", "°"),
            Map.entry("sum", "∑"),
            Map.entry("prod", "∏"),
            Map.entry("sqrt", "√"),
            Map.entry("alpha", "α"),
            Map.entry("beta", "β"),
            Map.entry("gamma", "γ"),
            Map.entry("delta", "δ"),
            Map.entry("Delta", "Δ"),
            Map.entry("lambda", "λ"),
            Map.entry("mu", "μ"),
            Map.entry("pi", "π"),
            Map.entry("sigma", "σ"),
            Map.entry("omega", "ω")
    );

    private static final Pattern INLINE_DOLLAR = Pattern.compile("\\$(.+?)\\$");
    private static final Pattern INLINE_PAREN = Pattern.compile("\\\\\\((.+?)\\\\\\)");
    private static final Pattern LATEX_TEXT = Pattern.compile("\\\\(?:text|mathrm|mathbf|textit|textrm)\\{([^}]*)\\}");
    private static final Pattern LATEX_CMD = Pattern.compile("\\\\([a-zA-Z]+)");

    private InlineLatexSimplifier() {
    }

    static String simplify(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return markdown;
        }
        if (!markdown.contains("$") && !markdown.contains("\\(")) {
            return markdown;
        }
        String result = replaceInlineMath(INLINE_DOLLAR, markdown);
        return replaceInlineMath(INLINE_PAREN, result);
    }

    private static String replaceInlineMath(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder out = new StringBuilder(input.length());
        while (matcher.find()) {
            String body = matcher.group(1).trim();
            String converted;
            if (body.contains("\\")) {
                converted = convertLatexBody(body);
            } else if (isLikelyInlineMath(body)) {
                converted = body;
            } else {
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(converted));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String convertLatexBody(String body) {
        String text = LATEX_TEXT.matcher(body).replaceAll("$1");
        Matcher cmd = LATEX_CMD.matcher(text);
        StringBuilder out = new StringBuilder();
        while (cmd.find()) {
            String name = cmd.group(1);
            String replacement = SYMBOLS.get(name);
            if (replacement != null) {
                cmd.appendReplacement(out, Matcher.quoteReplacement(replacement));
            } else {
                cmd.appendReplacement(out, Matcher.quoteReplacement(cmd.group(0)));
            }
        }
        cmd.appendTail(out);
        return out.toString().trim();
    }

    /**
     * Gemma often wraps numeric tuples/vectors in {@code $...$} without LaTeX commands.
     * Plain {@code $100$} (integer only) is left alone so it may still mean currency.
     */
    private static boolean isLikelyInlineMath(String body) {
        if (body.isEmpty()) {
            return false;
        }
        if (body.matches("^\\d+$")) {
            return false;
        }
        if (body.indexOf('[') >= 0 || body.indexOf('(') >= 0) {
            return true;
        }
        if (body.indexOf('_') >= 0 || body.indexOf('^') >= 0) {
            return true;
        }
        if (body.indexOf(',') >= 0 && body.matches(".*\\d.*")) {
            return true;
        }
        if (body.matches("^[0-9.\\s]+$") && body.indexOf('.') >= 0) {
            return true;
        }
        return body.matches(".*[=<>±×÷].*") && body.matches(".*\\d.*");
    }
}

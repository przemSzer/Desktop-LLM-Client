package dev.local.ai.ui.chat.controls;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownHtmlConverterTest {

    private final MarkdownHtmlConverter converter = new MarkdownHtmlConverter();

    @Test
    void wrapsEmojiInHeadingsForWebKit() {
        String html = converter.convertToHtml("## 💡 TEN SAM PRZYKŁAD");

        assertThat(html)
                .contains("<h2>")
                .contains("class=\"emoji-img\"")
                .contains("emoji/1f4a1.png")
                .contains("TEN SAM PRZYKŁAD");
    }

    @Test
    void keepsSymbolDollarsForKatex() {
        String html = converter.convertToHtml("- $r$ – **Nominalna stopa**");

        assertThat(html)
                .contains("\\(r\\)")
                .contains("<strong>Nominalna stopa</strong>")
                .doesNotContain("$r$");
    }

    @Test
    void keepsInlineCommandForKatex() {
        String html = converter.convertToHtml("Krok 1 $\\rightarrow$ Krok 2");

        assertThat(html)
                .contains("\\(\\rightarrow\\)")
                .doesNotContain("$");
    }

    @Test
    void keepsDisplayMathForKatex() {
        String html = converter.convertToHtml("$$\\mathbf{3738,19\\text{ zł}}$$");

        assertThat(html)
                .contains("$$\\mathbf{3738,19\\text{ zł}}$$")
                .doesNotContain("<em>")
                .doesNotContain("KATEXBLOCK");
    }

    @Test
    void keepsSubscriptInsideDisplayMath() {
        String html = converter.convertToHtml("$$x_i$$");

        assertThat(html)
                .contains("$$x_i$$")
                .doesNotContain("<em>");
    }

    @Test
    void leavesMermaidFenceForThePage() {
        String html = converter.convertToHtml("""
                ```mermaid
                graph TD
                  A["$r$"] --> B
                ```
                """);

        assertThat(html)
                .contains("language-mermaid")
                .contains("$r$")
                .doesNotContain("\\(r\\)");
    }

    @Test
    void doesNotWrapInsideHtmlTags() {
        String html = converter.convertToHtml("**bold**");

        assertThat(html).doesNotContain("emoji-img");
    }
}

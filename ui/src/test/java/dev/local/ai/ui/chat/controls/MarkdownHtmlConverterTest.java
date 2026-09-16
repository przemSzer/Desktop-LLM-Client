package dev.local.ai.ui.chat.controls;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownHtmlConverterTest {

    private final MarkdownHtmlConverter converter = new MarkdownHtmlConverter();

    @Test
    void wrapsEmojiInHeadingsForWebKit() {
        String html = converter.convertToHtml("## 💡 TEN SAM PRZYKŁAD");

        assertThat(html).contains("<h2>");
        assertThat(html).contains("class=\"emoji-img\"");
        assertThat(html).contains("emoji/1f4a1.png");
        assertThat(html).contains("TEN SAM PRZYKŁAD");
    }

    @Test
    void simplifiesInlineLatexFromGemma() {
        String html = converter.convertToHtml("Krok 1 $\\rightarrow$ Krok 2");

        assertThat(html).contains("→");
        assertThat(html).doesNotContain("rightarrow");
    }

    @Test
    void doesNotWrapInsideHtmlTags() {
        String html = converter.convertToHtml("**bold**");

        assertThat(html).doesNotContain("emoji-img");
    }
}

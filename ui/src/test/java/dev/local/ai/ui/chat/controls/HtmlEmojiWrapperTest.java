package dev.local.ai.ui.chat.controls;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlEmojiWrapperTest {

    @Test
    void twemojiAssetIdForLightBulb() {
        assertThat(HtmlEmojiWrapper.twemojiAssetId("💡")).isEqualTo("1f4a1");
    }

    @Test
    void twemojiAssetIdStripsVariationSelector() {
        assertThat(HtmlEmojiWrapper.twemojiAssetId("\u26A0\uFE0F")).isEqualTo("26a0");
    }
}

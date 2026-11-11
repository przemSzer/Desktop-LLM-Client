package dev.local.ai.ui.chat.controls;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KatexMathProtectorTest {

    @Test
    void protectsDisplayMathAndRestoresDelimiters() {
        var protection = KatexMathProtector.protect("wynik $$x_i$$ dalej");

        assertThat(protection.markdown())
                .doesNotContain("$$")
                .contains("KATEXBLOCK0END");
        assertThat(protection.blocks())
                .containsExactly("$$x_i$$");
        assertThat(KatexMathProtector.restore("<p>" + protection.markdown() + "</p>", protection.blocks()))
                .isEqualTo("<p>wynik $$x_i$$ dalej</p>");
    }

    @Test
    void protectsBracketAndParenDelimiters() {
        var protection = KatexMathProtector.protect("\\[a\\] i \\(b\\)");

        assertThat(protection.blocks())
                .containsExactly("\\[a\\]", "\\(b\\)");
    }

    @Test
    void leavesUnclosedDisplayMathInPlace() {
        assertThat(KatexMathProtector.protect("$$jeszcze nie").markdown())
                .isEqualTo("$$jeszcze nie");
    }

    @Test
    void rewritesSymbolDollarsToInlineDelimiters() {
        var protection = KatexMathProtector.protect("stopa $r$, razy $n$, przez $t$");

        assertThat(protection.blocks())
                .containsExactly("\\(r\\)", "\\(n\\)", "\\(t\\)");
        assertThat(KatexMathProtector.restore(protection.markdown(), protection.blocks()))
                .isEqualTo("stopa \\(r\\), razy \\(n\\), przez \\(t\\)");
    }

    @Test
    void leavesBareAmountDollars() {
        String markdown = "koszt $100$ i $100.50$";
        var protection = KatexMathProtector.protect(markdown);

        assertThat(protection.blocks())
                .isEmpty();
        assertThat(protection.markdown())
                .isEqualTo(markdown);
    }

    @Test
    void keepsAmountAndRewritesFollowingSymbol() {
        var protection = KatexMathProtector.protect("cena $100$ i $n$ razy");

        assertThat(protection.blocks())
                .containsExactly("\\(n\\)");
        assertThat(KatexMathProtector.restore(protection.markdown(), protection.blocks()))
                .isEqualTo("cena $100$ i \\(n\\) razy");
    }

    @Test
    void leavesMathInsideAFence() {
        String markdown = """
                ```
                $$not math$$
                ```
                """;

        assertThat(KatexMathProtector.protect(markdown).blocks())
                .isEmpty();
    }
}

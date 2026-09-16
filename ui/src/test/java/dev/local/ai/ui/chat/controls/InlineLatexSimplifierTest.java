package dev.local.ai.ui.chat.controls;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InlineLatexSimplifierTest {

    @Test
    void simplifiesDollarWrappedArrow() {
        assertThat(InlineLatexSimplifier.simplify("A $\\rightarrow$ B")).isEqualTo("A → B");
    }

    @Test
    void simplifiesParenWrappedArrow() {
        assertThat(InlineLatexSimplifier.simplify("A \\( \\rightarrow \\) B")).isEqualTo("A → B");
    }

    @Test
    void leavesPlainDollarAmountsUntouched() {
        assertThat(InlineLatexSimplifier.simplify("cost is $100$")).isEqualTo("cost is $100$");
    }

    @Test
    void unwrapsDollarWrappedNumericVector() {
        assertThat(InlineLatexSimplifier.simplify("weights $[0.25, 0.75, 0]$ here"))
                .isEqualTo("weights [0.25, 0.75, 0] here");
    }
}

package com.testsusmp.subs.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingCalculatorTest {

    private static final PromotionTerms LAUNCH = new PromotionTerms(
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31), new BigDecimal("25"), 3);

    @Test
    void signupInsidePromotionWindowGetsDiscount() {
        PriceQuote quote = PricingCalculator.quote(new BigDecimal("10000"), LAUNCH, LocalDate.of(2026, 11, 11));

        assertThat(quote.amount()).isEqualByComparingTo("7500.00");
        assertThat(quote.baseAmount()).isEqualByComparingTo("10000");
        assertThat(quote.promoApplied()).isTrue();
        assertThat(quote.promoEndsOn()).isEqualTo(LocalDate.of(2027, 2, 11));
    }

    @Test
    void lastDayOfDecemberStillQualifiesAndRoundsToCents() {
        PriceQuote quote = PricingCalculator.quote(new BigDecimal("8999.99"), LAUNCH, LocalDate.of(2026, 12, 31));

        assertThat(quote.promoApplied()).isTrue();
        assertThat(quote.amount()).isEqualByComparingTo("6749.99");
    }

    @Test
    void signupAfterPromotionPaysFullPrice() {
        PriceQuote quote = PricingCalculator.quote(new BigDecimal("10000"), LAUNCH, LocalDate.of(2027, 1, 1));

        assertThat(quote.amount()).isEqualByComparingTo("10000");
        assertThat(quote.promoApplied()).isFalse();
        assertThat(quote.promoEndsOn()).isNull();
    }

    @Test
    void signupBeforePromotionPaysFullPrice() {
        PriceQuote quote = PricingCalculator.quote(new BigDecimal("10000"), LAUNCH, LocalDate.of(2026, 10, 31));

        assertThat(quote.promoApplied()).isFalse();
    }

    @Test
    void appliesDiscountPercentRoundingToCents() {
        assertThat(PricingCalculator.applyDiscount(new BigDecimal("20000"), new BigDecimal("25")))
                .isEqualByComparingTo("15000.00");
        assertThat(PricingCalculator.applyDiscount(new BigDecimal("999.99"), new BigDecimal("33")))
                .isEqualByComparingTo("669.99");
    }

    @Test
    void withoutPromotionPaysFullPrice() {
        PriceQuote quote = PricingCalculator.quote(new BigDecimal("5000"), null, LocalDate.of(2026, 11, 11));

        assertThat(quote.amount()).isEqualByComparingTo("5000");
        assertThat(quote.promoApplied()).isFalse();
    }
}

package com.testsusmp.subs.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public final class PricingCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private PricingCalculator() {
    }

    public static PriceQuote quote(BigDecimal basePrice, PromotionTerms promo, LocalDate signupDate) {
        if (promo == null || !promo.isValidOn(signupDate)) {
            return new PriceQuote(basePrice, basePrice, false, null);
        }
        BigDecimal amount = applyDiscount(basePrice, promo.discountPercent());
        return new PriceQuote(amount, basePrice, true, signupDate.plusMonths(promo.durationMonths()));
    }

    public static BigDecimal applyDiscount(BigDecimal basePrice, BigDecimal discountPercent) {
        BigDecimal factor = HUNDRED.subtract(discountPercent).divide(HUNDRED);
        return basePrice.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}

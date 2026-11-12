package com.testsusmp.subs.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Precio a cobrar. promoEndsOn indica cuando volver al precio de lista (null si no hay promo). */
public record PriceQuote(BigDecimal amount, BigDecimal baseAmount, boolean promoApplied, LocalDate promoEndsOn) {
}

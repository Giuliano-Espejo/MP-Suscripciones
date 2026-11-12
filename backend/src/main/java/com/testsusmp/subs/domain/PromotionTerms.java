package com.testsusmp.subs.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Condiciones de una promocion: ventana de alta (inclusive), % de descuento y meses que dura. */
public record PromotionTerms(LocalDate validFrom, LocalDate validTo, BigDecimal discountPercent, int durationMonths) {

    public boolean isValidOn(LocalDate date) {
        return !date.isBefore(validFrom) && !date.isAfter(validTo);
    }
}

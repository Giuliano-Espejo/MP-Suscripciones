package com.testsusmp.subs.promotion;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PromotionRequest(
        @NotBlank String name,
        @NotNull LocalDate validFrom,
        @NotNull LocalDate validTo,
        @NotNull @DecimalMin("1") @DecimalMax("100") BigDecimal discountPercent,
        @Min(1) @Max(24) int durationMonths,
        Boolean active) {

    @AssertTrue(message = "validTo debe ser igual o posterior a validFrom")
    public boolean isRangeValid() {
        return validFrom == null || validTo == null || !validTo.isBefore(validFrom);
    }
}

package com.testsusmp.subs.plan;

import com.testsusmp.subs.domain.PermanencePolicy;
import com.testsusmp.subs.domain.Tier;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record PlanRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotNull Tier tier,
        @NotNull @DecimalMin(value = "1.00") BigDecimal price,
        @Min(0) @Max(744) int monthlyHours,
        @Min(PermanencePolicy.DEFAULT_MIN_MONTHS) @Max(36) Integer minPermanenceMonths,
        List<@NotBlank @Size(max = 200) String> benefits) {

    /** La permanencia minima por defecto es de 3 meses y nunca puede ser menor. */
    public int effectiveMinPermanenceMonths() {
        return minPermanenceMonths == null ? PermanencePolicy.DEFAULT_MIN_MONTHS : minPermanenceMonths;
    }
}

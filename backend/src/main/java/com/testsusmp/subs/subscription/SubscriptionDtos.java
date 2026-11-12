package com.testsusmp.subs.subscription;

import com.testsusmp.subs.domain.PlanChange;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Requests y responses de la API de suscripciones. */
public final class SubscriptionDtos {

    private SubscriptionDtos() {
    }

    public record CreateSubscriptionRequest(@NotBlank @Size(max = 150) String fullName,
                                            @NotBlank @Email @Size(max = 150) String email,
                                            @NotNull Long planId) {
    }

    public record ChangePlanRequest(@NotNull Long planId) {
    }

    public record ConsumptionRequest(@NotNull @DecimalMin(value = "0.25") BigDecimal hours,
                                     @Size(max = 200) String description,
                                     LocalDate date) {
    }

    public record SimulateRequest(@NotBlank String status) {
    }

    public record CancelResult(boolean immediate, LocalDate effectiveDate, SubscriptionView subscription) {
    }

    public record ChangePlanResult(PlanChange change, SubscriptionView subscription) {
    }

    public record ConsumptionItem(Long id, LocalDate date, BigDecimal hours, String description) {
    }

    public record ConsumptionSummary(Long subscriptionId, LocalDate periodFrom, LocalDate periodTo,
                                     int hoursIncluded, BigDecimal hoursUsed, BigDecimal hoursAvailable,
                                     List<String> benefits, List<ConsumptionItem> items) {
    }

    public record JobsResult(int cancelled, int promosExpired) {
    }
}

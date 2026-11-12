package com.testsusmp.subs.subscription;

import com.testsusmp.subs.domain.SubscriptionStatus;
import com.testsusmp.subs.domain.Tier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SubscriptionView(
        Long id,
        Long memberId,
        String memberName,
        String memberEmail,
        Long planId,
        String planName,
        Tier tier,
        int tierLevel,
        SubscriptionStatus status,
        BigDecimal amount,
        BigDecimal basePrice,
        boolean promoActive,
        String promotionName,
        LocalDate promoEndsOn,
        LocalDate startDate,
        LocalDate minEndDate,
        boolean permanenceFulfilled,
        LocalDate scheduledCancelDate,
        LocalDate cancelledAt,
        String gatewayId,
        String initPoint,
        int hoursIncluded,
        BigDecimal hoursUsed,
        BigDecimal hoursAvailable,
        Instant createdAt) {
}

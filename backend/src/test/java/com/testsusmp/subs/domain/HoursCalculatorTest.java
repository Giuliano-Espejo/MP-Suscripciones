package com.testsusmp.subs.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HoursCalculatorTest {

    @Test
    void periodStartsOnTheMonthlyAnniversaryOfTheSubscription() {
        BillingPeriod period = HoursCalculator.currentPeriod(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 11, 11));

        assertThat(period.from()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(period.toExclusive()).isEqualTo(LocalDate.of(2026, 12, 10));
    }

    @Test
    void dayBeforeAnniversaryBelongsToPreviousPeriod() {
        BillingPeriod period = HoursCalculator.currentPeriod(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 11, 9));

        assertThat(period.from()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(period.toExclusive()).isEqualTo(LocalDate.of(2026, 11, 10));
    }

    @Test
    void firstPeriodStartsOnStartDate() {
        BillingPeriod period = HoursCalculator.currentPeriod(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 11));

        assertThat(period.from()).isEqualTo(LocalDate.of(2026, 11, 11));
        assertThat(period.toExclusive()).isEqualTo(LocalDate.of(2026, 12, 11));
    }

    @Test
    void availableHoursSubtractsConsumption() {
        assertThat(HoursCalculator.available(10, new BigDecimal("3.5"))).isEqualByComparingTo("6.5");
    }

    @Test
    void availableHoursNeverGoesBelowZero() {
        assertThat(HoursCalculator.available(4, new BigDecimal("6"))).isEqualByComparingTo("0");
    }

    @Test
    void consumingMoreThanAvailableIsRejected() {
        assertThatThrownBy(() -> HoursCalculator.assertCanConsume(10, new BigDecimal("8"), new BigDecimal("2.5")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2");
    }

    @Test
    void consumingExactlyTheRemainingHoursIsAllowed() {
        HoursCalculator.assertCanConsume(10, new BigDecimal("8"), new BigDecimal("2"));
    }

    @Test
    void consumingNonPositiveHoursIsRejected() {
        assertThatThrownBy(() -> HoursCalculator.assertCanConsume(10, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(BusinessException.class);
    }
}

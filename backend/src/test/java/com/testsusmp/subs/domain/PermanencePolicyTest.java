package com.testsusmp.subs.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PermanencePolicyTest {

    @Test
    void minimumEndDateIsThreeMonthsAfterStart() {
        assertThat(PermanencePolicy.minimumEndDate(LocalDate.of(2026, 11, 15), 3))
                .isEqualTo(LocalDate.of(2027, 2, 15));
    }

    @Test
    void minimumEndDateClampsToMonthEnd() {
        assertThat(PermanencePolicy.minimumEndDate(LocalDate.of(2026, 11, 30), 3))
                .isEqualTo(LocalDate.of(2027, 2, 28));
    }

    @Test
    void cancellationBeforeMinimumIsScheduledAtMinimumEndDate() {
        CancellationDecision decision = PermanencePolicy.decide(
                LocalDate.of(2026, 11, 1), 3, LocalDate.of(2026, 12, 10));

        assertThat(decision.immediate()).isFalse();
        assertThat(decision.effectiveDate()).isEqualTo(LocalDate.of(2027, 2, 1));
    }

    @Test
    void cancellationOnTheMinimumEndDateIsImmediate() {
        CancellationDecision decision = PermanencePolicy.decide(
                LocalDate.of(2026, 11, 1), 3, LocalDate.of(2027, 2, 1));

        assertThat(decision.immediate()).isTrue();
        assertThat(decision.effectiveDate()).isEqualTo(LocalDate.of(2027, 2, 1));
    }

    @Test
    void cancellationAfterMinimumIsImmediate() {
        CancellationDecision decision = PermanencePolicy.decide(
                LocalDate.of(2026, 1, 1), 3, LocalDate.of(2026, 11, 11));

        assertThat(decision.immediate()).isTrue();
        assertThat(decision.effectiveDate()).isEqualTo(LocalDate.of(2026, 11, 11));
    }

    @Test
    void neverActivatedSubscriptionCancelsImmediately() {
        CancellationDecision decision = PermanencePolicy.decide(null, 3, LocalDate.of(2026, 11, 11));

        assertThat(decision.immediate()).isTrue();
        assertThat(decision.effectiveDate()).isEqualTo(LocalDate.of(2026, 11, 11));
    }
}

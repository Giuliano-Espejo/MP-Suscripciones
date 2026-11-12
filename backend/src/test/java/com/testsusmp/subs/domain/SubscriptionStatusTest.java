package com.testsusmp.subs.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionStatusTest {

    @Test
    void mapsMercadoPagoPreapprovalStatuses() {
        assertThat(SubscriptionStatus.fromMercadoPago("pending")).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(SubscriptionStatus.fromMercadoPago("authorized")).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(SubscriptionStatus.fromMercadoPago("paused")).isEqualTo(SubscriptionStatus.PAUSED);
        assertThat(SubscriptionStatus.fromMercadoPago("cancelled")).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    void acceptsAlternativeSpellingAndCase() {
        assertThat(SubscriptionStatus.fromMercadoPago("CANCELED")).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    void unknownStatusFallsBackToPending() {
        assertThat(SubscriptionStatus.fromMercadoPago("whatever")).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(SubscriptionStatus.fromMercadoPago(null)).isEqualTo(SubscriptionStatus.PENDING);
    }

    @Test
    void mapsBackToMercadoPagoValues() {
        assertThat(SubscriptionStatus.ACTIVE.toMercadoPago()).isEqualTo("authorized");
        assertThat(SubscriptionStatus.PAUSED.toMercadoPago()).isEqualTo("paused");
        assertThat(SubscriptionStatus.CANCELLED.toMercadoPago()).isEqualTo("cancelled");
    }
}

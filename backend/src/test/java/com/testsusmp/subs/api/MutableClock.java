package com.testsusmp.subs.api;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Reloj de test que se puede adelantar para probar permanencia, promos y jobs. */
public class MutableClock extends Clock {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private Instant now;

    public MutableClock(LocalDate today) {
        set(today);
    }

    public final void set(LocalDate today) {
        this.now = today.atTime(12, 0).atZone(ZONE).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return ZONE;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}

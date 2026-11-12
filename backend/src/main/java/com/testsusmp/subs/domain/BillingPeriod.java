package com.testsusmp.subs.domain;

import java.time.LocalDate;

public record BillingPeriod(LocalDate from, LocalDate toExclusive) {
}

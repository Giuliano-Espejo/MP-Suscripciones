package com.testsusmp.subs.domain;

import java.time.LocalDate;

/** immediate=false significa baja programada para effectiveDate. */
public record CancellationDecision(boolean immediate, LocalDate effectiveDate) {
}

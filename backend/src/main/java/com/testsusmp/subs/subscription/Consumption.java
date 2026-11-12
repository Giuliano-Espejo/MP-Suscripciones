package com.testsusmp.subs.subscription;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Uso de horas incluidas en la membresia (ej. sala, clase, consultoria). */
@Entity
@Table(name = "consumptions")
public class Consumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Subscription subscription;

    private LocalDate consumedOn;
    private BigDecimal hours;
    private String description;
    private Instant createdAt;

    protected Consumption() {
    }

    public Consumption(Subscription subscription, LocalDate consumedOn, BigDecimal hours, String description,
                       Instant now) {
        this.subscription = subscription;
        this.consumedOn = consumedOn;
        this.hours = hours;
        this.description = description;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getConsumedOn() {
        return consumedOn;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public String getDescription() {
        return description;
    }
}

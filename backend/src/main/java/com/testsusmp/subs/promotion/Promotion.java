package com.testsusmp.subs.promotion;

import com.testsusmp.subs.domain.PromotionTerms;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "promotions")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private LocalDate validFrom;
    private LocalDate validTo;
    private BigDecimal discountPercent;
    private int durationMonths;
    private boolean active;

    protected Promotion() {
    }

    public Promotion(PromotionRequest req) {
        apply(req);
    }

    public void apply(PromotionRequest req) {
        this.name = req.name().trim();
        this.validFrom = req.validFrom();
        this.validTo = req.validTo();
        this.discountPercent = req.discountPercent();
        this.durationMonths = req.durationMonths();
        this.active = req.active() == null || req.active();
    }

    public PromotionTerms terms() {
        return new PromotionTerms(validFrom, validTo, discountPercent, durationMonths);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public int getDurationMonths() {
        return durationMonths;
    }

    public boolean isActive() {
        return active;
    }
}

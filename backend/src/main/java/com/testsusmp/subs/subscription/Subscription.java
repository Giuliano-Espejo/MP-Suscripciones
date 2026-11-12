package com.testsusmp.subs.subscription;

import com.testsusmp.subs.domain.BusinessException;
import com.testsusmp.subs.domain.PermanencePolicy;
import com.testsusmp.subs.domain.SubscriptionStatus;
import com.testsusmp.subs.plan.Plan;
import com.testsusmp.subs.promotion.Promotion;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    /** Monto que se cobra hoy (puede tener descuento promocional). */
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    private Promotion promotion;

    private boolean promoActive;
    private LocalDate promoEndsOn;
    private LocalDate startDate;
    private LocalDate minEndDate;
    private LocalDate scheduledCancelDate;
    private LocalDate cancelledAt;
    private String gatewayId;
    private String initPoint;
    private Instant createdAt;

    protected Subscription() {
    }

    public Subscription(Member member, Plan plan, BigDecimal amount, Promotion promotion, Instant now) {
        this.member = member;
        this.plan = plan;
        this.amount = amount;
        this.promotion = promotion;
        this.promoActive = promotion != null;
        this.status = SubscriptionStatus.PENDING;
        this.createdAt = now;
    }

    public String externalReference() {
        return "SUB-" + id;
    }

    public void linkGateway(String gatewayId, String initPoint) {
        this.gatewayId = gatewayId;
        if (initPoint != null) {
            this.initPoint = initPoint;
        }
    }

    /** Aplica un estado informado por la pasarela. La primera activacion inicia la permanencia minima. */
    public void applyStatus(SubscriptionStatus newStatus, LocalDate today) {
        if (newStatus == SubscriptionStatus.ACTIVE && startDate == null) {
            startDate = today;
            minEndDate = PermanencePolicy.minimumEndDate(today, plan.getMinPermanenceMonths());
            if (promoActive && promotion != null) {
                promoEndsOn = today.plusMonths(promotion.getDurationMonths());
            }
        }
        if (newStatus == SubscriptionStatus.CANCELLED && status != SubscriptionStatus.CANCELLED) {
            cancelledAt = today;
            scheduledCancelDate = null;
        }
        this.status = newStatus;
    }

    public void scheduleCancellation(LocalDate date) {
        this.scheduledCancelDate = date;
    }

    public void undoScheduledCancellation() {
        if (scheduledCancelDate == null) {
            throw new BusinessException("La suscripcion no tiene una baja programada");
        }
        this.scheduledCancelDate = null;
    }

    public void changePlan(Plan newPlan, BigDecimal newAmount) {
        this.plan = newPlan;
        this.amount = newAmount;
    }

    public void endPromotion() {
        this.promoActive = false;
        this.amount = plan.getPrice();
    }

    public boolean isLive() {
        return status != SubscriptionStatus.CANCELLED;
    }

    public void requireStatus(SubscriptionStatus expected, String action) {
        if (status != expected) {
            throw new BusinessException("No se puede " + action + " una suscripcion en estado " + status);
        }
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public Plan getPlan() {
        return plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public boolean isPromoActive() {
        return promoActive;
    }

    public LocalDate getPromoEndsOn() {
        return promoEndsOn;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getMinEndDate() {
        return minEndDate;
    }

    public LocalDate getScheduledCancelDate() {
        return scheduledCancelDate;
    }

    public LocalDate getCancelledAt() {
        return cancelledAt;
    }

    public String getGatewayId() {
        return gatewayId;
    }

    public String getInitPoint() {
        return initPoint;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

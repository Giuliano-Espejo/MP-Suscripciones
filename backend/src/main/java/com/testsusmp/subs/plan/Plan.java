package com.testsusmp.subs.plan;

import com.testsusmp.subs.domain.Tier;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @Enumerated(EnumType.STRING)
    private Tier tier;

    private BigDecimal price;
    private String currency;
    private int monthlyHours;
    private int minPermanenceMonths;
    private boolean active;
    private String gatewayPlanId;
    private String gatewayInitPoint;
    private Instant createdAt;
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "plan_benefits", joinColumns = @JoinColumn(name = "plan_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "benefit")
    private List<String> benefits = new ArrayList<>();

    protected Plan() {
    }

    public Plan(String currency, Instant now) {
        this.currency = currency;
        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void apply(PlanRequest req, Instant now) {
        this.name = req.name().trim();
        this.description = req.description();
        this.tier = req.tier();
        this.price = req.price();
        this.monthlyHours = req.monthlyHours();
        this.minPermanenceMonths = req.effectiveMinPermanenceMonths();
        this.benefits.clear();
        if (req.benefits() != null) {
            req.benefits().stream().map(String::trim).filter(b -> !b.isEmpty()).forEach(this.benefits::add);
        }
        this.updatedAt = now;
    }

    /** Plan creado desde el panel de Mercado Pago: se importa con valores por defecto para lo que MP no conoce. */
    public static Plan importFromGateway(String name, BigDecimal price, String currency, Tier tier, boolean active,
                                         String gatewayPlanId, String initPoint, Instant now) {
        Plan plan = new Plan(currency, now);
        plan.name = name;
        plan.description = "Importado desde Mercado Pago";
        plan.tier = tier;
        plan.price = price;
        plan.monthlyHours = 0;
        plan.minPermanenceMonths = com.testsusmp.subs.domain.PermanencePolicy.DEFAULT_MIN_MONTHS;
        plan.active = active;
        plan.linkGateway(gatewayPlanId, initPoint);
        return plan;
    }

    /** Aplica nombre y precio vigentes en Mercado Pago. Devuelve true si algo cambio. */
    public boolean syncFromGateway(String gatewayName, BigDecimal gatewayPrice, Instant now) {
        boolean changed = false;
        if (gatewayName != null && !gatewayName.equals(name)) {
            name = gatewayName;
            changed = true;
        }
        if (gatewayPrice != null && (price == null || gatewayPrice.compareTo(price) != 0)) {
            price = gatewayPrice;
            changed = true;
        }
        if (changed) {
            updatedAt = now;
        }
        return changed;
    }

    public void linkGateway(String gatewayPlanId, String initPoint) {
        this.gatewayPlanId = gatewayPlanId;
        this.gatewayInitPoint = initPoint;
    }

    public void setActive(boolean active, Instant now) {
        this.active = active;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Tier getTier() {
        return tier;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public int getMonthlyHours() {
        return monthlyHours;
    }

    public int getMinPermanenceMonths() {
        return minPermanenceMonths;
    }

    public boolean isActive() {
        return active;
    }

    public String getGatewayPlanId() {
        return gatewayPlanId;
    }

    public String getGatewayInitPoint() {
        return gatewayInitPoint;
    }

    public List<String> getBenefits() {
        return benefits;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

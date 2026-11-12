package com.testsusmp.subs.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubscriptionApiTest extends ApiTestSupport {

    @Test
    void subscribingDuringLaunchPromoCreatesPendingSubscriptionWithDiscountAndPaymentLink() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 20);

        postJson("/api/subscriptions", """
                {"fullName":"Ana Perez","email":"ana@example.com","planId":%d}
                """.formatted(planId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(7500.0))
                .andExpect(jsonPath("$.basePrice").value(10000.0))
                .andExpect(jsonPath("$.promoActive").value(true))
                .andExpect(jsonPath("$.initPoint", notNullValue()))
                .andExpect(jsonPath("$.memberName").value("Ana Perez"));
    }

    @Test
    void subscribingAfterPromoWindowPaysFullPrice() throws Exception {
        clock.set(LocalDate.of(2027, 1, 5));
        long planId = createPlan("Plus", "PLUS", "10000", 20);

        postJson("/api/subscriptions", """
                {"fullName":"Ana Perez","email":"ana@example.com","planId":%d}
                """.formatted(planId))
                .andExpect(jsonPath("$.amount").value(10000.0))
                .andExpect(jsonPath("$.promoActive").value(false));
    }

    @Test
    void memberCannotHoldTwoLiveSubscriptions() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 20);
        subscribe("Ana Perez", "ana@example.com", planId);

        postJson("/api/subscriptions", """
                {"fullName":"Ana Perez","email":"ana@example.com","planId":%d}
                """.formatted(planId))
                .andExpect(status().isConflict());
    }

    @Test
    void cannotSubscribeToInactivePlan() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 20);
        mvc.perform(delete("/api/plans/" + planId));

        postJson("/api/subscriptions", """
                {"fullName":"Ana Perez","email":"ana@example.com","planId":%d}
                """.formatted(planId))
                .andExpect(status().isConflict());
    }

    @Test
    void authorizationActivatesSubscriptionAndStartsMinimumPermanence() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 20);
        long subId = subscribe("Ana Perez", "ana@example.com", planId);

        authorize(subId);

        mvc.perform(get("/api/subscriptions/" + subId))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startDate").value("2026-11-11"))
                .andExpect(jsonPath("$.minEndDate").value("2027-02-11"))
                .andExpect(jsonPath("$.promoEndsOn").value("2027-02-11"))
                .andExpect(jsonPath("$.permanenceFulfilled").value(false));
        mvc.perform(get("/api/members/active"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].memberEmail").value("ana@example.com"));
    }

    @Test
    void cancellationBeforeThreeMonthsIsScheduledAndExecutedByDailyJob() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 20);
        long subId = subscribe("Ana Perez", "ana@example.com", planId);
        authorize(subId);

        mvc.perform(post("/api/subscriptions/" + subId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.immediate").value(false))
                .andExpect(jsonPath("$.effectiveDate").value("2027-02-11"))
                .andExpect(jsonPath("$.subscription.status").value("ACTIVE"))
                .andExpect(jsonPath("$.subscription.scheduledCancelDate").value("2027-02-11"));

        clock.set(LocalDate.of(2027, 2, 10));
        mvc.perform(post("/api/admin/jobs/run")).andExpect(jsonPath("$.cancelled").value(0));

        clock.set(LocalDate.of(2027, 2, 11));
        mvc.perform(post("/api/admin/jobs/run")).andExpect(jsonPath("$.cancelled").value(1));
        mvc.perform(get("/api/subscriptions/" + subId))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").value("2027-02-11"));
        org.assertj.core.api.Assertions.assertThat(gateway.statusOf(gatewayIdOf(subId))).isEqualTo("cancelled");
    }

    @Test
    void scheduledCancellationCanBeUndone() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 20);
        long subId = subscribe("Ana Perez", "ana@example.com", planId);
        authorize(subId);
        mvc.perform(post("/api/subscriptions/" + subId + "/cancel"));

        mvc.perform(delete("/api/subscriptions/" + subId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduledCancelDate").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void cancellationAfterMinimumPermanenceIsImmediate() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 20);
        long subId = subscribe("Ana Perez", "ana@example.com", planId);
        authorize(subId);
        clock.set(LocalDate.of(2027, 3, 1));

        mvc.perform(post("/api/subscriptions/" + subId + "/cancel"))
                .andExpect(jsonPath("$.immediate").value(true))
                .andExpect(jsonPath("$.subscription.status").value("CANCELLED"));
    }

    @Test
    void pendingSubscriptionCanBeCancelledRightAway() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 20);
        long subId = subscribe("Ana Perez", "ana@example.com", planId);

        mvc.perform(post("/api/subscriptions/" + subId + "/cancel"))
                .andExpect(jsonPath("$.immediate").value(true))
                .andExpect(jsonPath("$.subscription.status").value("CANCELLED"));
    }

    @Test
    void pauseAndResume() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 20);
        long subId = subscribe("Ana Perez", "ana@example.com", planId);
        authorize(subId);

        mvc.perform(post("/api/subscriptions/" + subId + "/pause")).andExpect(jsonPath("$.status").value("PAUSED"));
        org.assertj.core.api.Assertions.assertThat(gateway.statusOf(gatewayIdOf(subId))).isEqualTo("paused");
        mvc.perform(get("/api/members/active")).andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(post("/api/subscriptions/" + subId + "/resume")).andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void syncPullsCurrentStatusFromMercadoPagoWithoutWebhook() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 20);
        long a = subscribe("Ana Perez", "ana@example.com", planId);
        long b = subscribe("Beto Gomez", "beto@example.com", planId);
        gateway.simulate(gatewayIdOf(a), "authorized");
        gateway.simulate(gatewayIdOf(b), "cancelled");

        mvc.perform(post("/api/subscriptions/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(2))
                .andExpect(jsonPath("$.changed").value(2));

        mvc.perform(get("/api/subscriptions/" + a)).andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(get("/api/subscriptions/" + b)).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cannotPausePendingSubscription() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 20);
        long subId = subscribe("Ana Perez", "ana@example.com", planId);

        mvc.perform(post("/api/subscriptions/" + subId + "/pause")).andExpect(status().isConflict());
    }

    @Test
    void upgradeToHigherTierKeepsPromoDiscountAndUpdatesGatewayAmount() throws Exception {
        long basico = createPlan("Basico", "BASICO", "8000", 8);
        long premium = createPlan("Premium", "PREMIUM", "20000", 40);
        long subId = subscribe("Ana Perez", "ana@example.com", basico);
        authorize(subId);

        mvc.perform(put("/api/subscriptions/" + subId + "/plan").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":" + premium + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.change").value("UPGRADE"))
                .andExpect(jsonPath("$.subscription.tier").value("PREMIUM"))
                .andExpect(jsonPath("$.subscription.amount").value(15000.0))
                .andExpect(jsonPath("$.subscription.startDate").value("2026-11-11"));
        org.assertj.core.api.Assertions.assertThat(gateway.amountOf(gatewayIdOf(subId))).isEqualByComparingTo("15000");
    }

    @Test
    void changingToSamePlanIsRejected() throws Exception {
        long plus = createPlan("Plus", "PLUS", "10000", 20);
        long subId = subscribe("Ana Perez", "ana@example.com", plus);
        authorize(subId);

        mvc.perform(put("/api/subscriptions/" + subId + "/plan").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":" + plus + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    void promoExpiryRestoresListPrice() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 20);
        long subId = subscribe("Ana Perez", "ana@example.com", planId);
        authorize(subId);

        clock.set(LocalDate.of(2027, 2, 11));
        mvc.perform(post("/api/admin/jobs/run")).andExpect(jsonPath("$.promosExpired").value(1));

        mvc.perform(get("/api/subscriptions/" + subId))
                .andExpect(jsonPath("$.amount").value(10000.0))
                .andExpect(jsonPath("$.promoActive").value(false));
        org.assertj.core.api.Assertions.assertThat(gateway.amountOf(gatewayIdOf(subId))).isEqualByComparingTo("10000");
    }

    @Test
    void listsSubscriptionsFilteredByStatus() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 20);
        long a = subscribe("Ana Perez", "ana@example.com", planId);
        subscribe("Beto Gomez", "beto@example.com", planId);
        authorize(a);

        mvc.perform(get("/api/subscriptions")).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/subscriptions?status=PENDING"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].memberName").value("Beto Gomez"));
    }

    @Test
    void dashboardSummary() throws Exception {
        long plus = createPlan("Plus", "PLUS", "10000", 20);
        long premium = createPlan("Premium", "PREMIUM", "20000", 40);
        long a = subscribe("Ana Perez", "ana@example.com", plus);
        long b = subscribe("Beto Gomez", "beto@example.com", premium);
        subscribe("Caro Diaz", "caro@example.com", premium);
        authorize(a);
        authorize(b);

        mvc.perform(get("/api/dashboard/summary"))
                .andExpect(jsonPath("$.activeMembers").value(2))
                .andExpect(jsonPath("$.pending").value(1))
                .andExpect(jsonPath("$.monthlyRevenue").value(22500.0))
                .andExpect(jsonPath("$.activeByTier.PLUS").value(1))
                .andExpect(jsonPath("$.activeByTier.PREMIUM").value(1))
                .andExpect(jsonPath("$.activeByTier.BASICO").value(0))
                .andExpect(jsonPath("$.currentPromotion.discountPercent").value(25))
                .andExpect(jsonPath("$.gatewayMode").value("demo"));
    }

    private String gatewayIdOf(long subId) throws Exception {
        return body(mvc.perform(get("/api/subscriptions/" + subId))).get("gatewayId").asText();
    }
}

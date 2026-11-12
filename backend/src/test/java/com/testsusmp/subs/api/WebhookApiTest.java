package com.testsusmp.subs.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.mercadopago.webhook-secret=secreto")
class WebhookApiTest extends ApiTestSupport {

    // HMAC-SHA256("id:123456;request-id:req-1;ts:1704908010;", "secreto")
    private static final String VALID_SIGNATURE =
            "ts=1704908010,v1=450dd2f3a8c0afa6d1a806ad891a2020b7d974fb2c992c501e559fa353bdd98c";

    @Test
    void rejectsNotificationWithInvalidSignature() throws Exception {
        mvc.perform(post("/api/webhooks/mercadopago?type=subscription_preapproval&data.id=123456")
                        .header("x-signature", "ts=1,v1=deadbeef")
                        .header("x-request-id", "req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"subscription_preapproval\",\"data\":{\"id\":\"123456\"}}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void preapprovalCreatedFromPlanLinkRegistersNewMember() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 20);
        String gatewayPlanId = body(mvc.perform(get("/api/plans/" + planId))).get("gatewayPlanId").asText();
        gateway.registerExternalPreapproval("123456", gatewayPlanId, "nuevo@example.com", "authorized");

        mvc.perform(post("/api/webhooks/mercadopago?type=subscription_preapproval&data.id=123456")
                        .header("x-signature", VALID_SIGNATURE)
                        .header("x-request-id", "req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"subscription_preapproval\",\"action\":\"created\",\"data\":{\"id\":\"123456\"}}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/members/active"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].memberEmail").value("nuevo@example.com"))
                .andExpect(jsonPath("$[0].planName").value("Plus"));
    }

    @Test
    void preapprovalFromPlanLinkGetsLaunchPromoAmountPushedToMercadoPago() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 20);
        String gatewayPlanId = body(mvc.perform(get("/api/plans/" + planId))).get("gatewayPlanId").asText();
        gateway.registerExternalPreapproval("123456", gatewayPlanId, "nuevo@example.com", "authorized");

        mvc.perform(post("/api/webhooks/mercadopago?type=subscription_preapproval&data.id=123456")
                        .header("x-signature", VALID_SIGNATURE)
                        .header("x-request-id", "req-1"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/members/active")).andExpect(jsonPath("$[0].amount").value(7500.0));
        org.assertj.core.api.Assertions.assertThat(gateway.amountOf("123456")).isEqualByComparingTo("7500");
    }

    @Test
    void planNotificationSyncsPlansFromMercadoPago() throws Exception {
        gateway.registerExternalPlan("ext-9", "Plan desde panel MP", new java.math.BigDecimal("9900"));

        // HMAC-SHA256("id:ext-9;request-id:req-3;ts:1704908012;", "secreto")
        mvc.perform(post("/api/webhooks/mercadopago?type=subscription_preapproval_plan&data.id=ext-9")
                        .header("x-signature", "ts=1704908012,v1=8d11fb6de65b7a1d6eeb5e4d284a3423ce83d23e9ec559bb9236cf58112e202a")
                        .header("x-request-id", "req-3"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/plans"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Plan desde panel MP"));
    }

    @Test
    void ignoresUnrelatedTopics() throws Exception {
        mvc.perform(post("/api/webhooks/mercadopago?type=payment&data.id=123456")
                        .header("x-signature", VALID_SIGNATURE)
                        .header("x-request-id", "req-1"))
                .andExpect(status().isOk());
    }
}

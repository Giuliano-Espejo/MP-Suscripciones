package com.testsusmp.subs.gateway;

import com.testsusmp.subs.domain.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MercadoPagoRestGatewayTest {

    private static final String BASE = "https://api.mercadopago.test";

    private MockRestServiceServer server;
    private MercadoPagoRestGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new MercadoPagoRestGateway(builder, BASE, "TEST-token");
    }

    @Test
    void createsPreapprovalPlanWithMonthlyAutoRecurring() {
        server.expect(requestTo(BASE + "/preapproval_plan"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer TEST-token"))
                .andExpect(jsonPath("$.reason").value("Plan Plus"))
                .andExpect(jsonPath("$.auto_recurring.frequency").value(1))
                .andExpect(jsonPath("$.auto_recurring.frequency_type").value("months"))
                .andExpect(jsonPath("$.auto_recurring.transaction_amount").value(15000.0))
                .andExpect(jsonPath("$.auto_recurring.currency_id").value("ARS"))
                .andExpect(jsonPath("$.back_url").value("https://club.example/gracias"))
                .andRespond(withSuccess("""
                        {"id":"2c93808","init_point":"https://www.mercadopago.com.ar/subscriptions/checkout?preapproval_plan_id=2c93808","status":"active"}
                        """, MediaType.APPLICATION_JSON));

        GatewayPlan plan = gateway.createPlan(new PlanSpec("Plan Plus", new BigDecimal("15000"), "ARS", 1,
                "https://club.example/gracias"));

        assertThat(plan.id()).isEqualTo("2c93808");
        assertThat(plan.initPoint()).contains("preapproval_plan_id=2c93808");
        server.verify();
    }

    @Test
    void listsActivePlansFollowingPagination() {
        server.expect(requestTo(BASE + "/preapproval_plan/search?status=active&limit=100&offset=0"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"paging":{"offset":0,"limit":100,"total":2},
                         "results":[
                           {"id":"p1","reason":"Plan Uno","status":"active","init_point":"https://mp/p1",
                            "auto_recurring":{"frequency":1,"frequency_type":"months","transaction_amount":12000,"currency_id":"ARS"}},
                           {"id":"p2","reason":"Plan Dos","status":"active","init_point":"https://mp/p2",
                            "auto_recurring":{"frequency":1,"frequency_type":"months","transaction_amount":15000.5,"currency_id":"ARS"}}
                         ]}
                        """, MediaType.APPLICATION_JSON));

        List<GatewayPlanInfo> plans = gateway.listActivePlans();

        assertThat(plans).extracting(GatewayPlanInfo::id).containsExactly("p1", "p2");
        assertThat(plans.get(1).reason()).isEqualTo("Plan Dos");
        assertThat(plans.get(1).amount()).isEqualByComparingTo("15000.5");
        assertThat(plans.get(1).currency()).isEqualTo("ARS");
        assertThat(plans.get(0).initPoint()).isEqualTo("https://mp/p1");
        server.verify();
    }

    @Test
    void deactivatesPlanByCancellingIt() {
        server.expect(requestTo(BASE + "/preapproval_plan/2c93808"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.status").value("cancelled"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        gateway.deactivatePlan("2c93808");

        server.verify();
    }

    @Test
    void createsPendingPreapprovalWithExternalReferenceAndReturnsInitPoint() {
        server.expect(requestTo(BASE + "/preapproval"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.external_reference").value("SUB-42"))
                .andExpect(jsonPath("$.payer_email").value("socio@example.com"))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.auto_recurring.transaction_amount").value(7500.0))
                .andRespond(withSuccess("""
                        {"id":"pre-1","init_point":"https://mp/checkout/pre-1","status":"pending","external_reference":"SUB-42","payer_email":"socio@example.com"}
                        """, MediaType.APPLICATION_JSON));

        GatewaySubscription sub = gateway.createSubscription(new SubscriptionSpec("Plan Plus", "SUB-42",
                "socio@example.com", new BigDecimal("7500"), "ARS", "https://club.example/gracias"));

        assertThat(sub.id()).isEqualTo("pre-1");
        assertThat(sub.initPoint()).isEqualTo("https://mp/checkout/pre-1");
        assertThat(sub.status()).isEqualTo("pending");
        server.verify();
    }

    @Test
    void updatesPreapprovalStatusUsingMercadoPagoValue() {
        server.expect(requestTo(BASE + "/preapproval/pre-1"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.status").value("paused"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        gateway.updateSubscriptionStatus("pre-1", SubscriptionStatus.PAUSED);

        server.verify();
    }

    @Test
    void updatesPreapprovalAmount() {
        server.expect(requestTo(BASE + "/preapproval/pre-1"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.auto_recurring.transaction_amount").value(20000.0))
                .andExpect(jsonPath("$.auto_recurring.currency_id").value("ARS"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        gateway.updateSubscriptionAmount("pre-1", new BigDecimal("20000"), "ARS");

        server.verify();
    }

    @Test
    void readsPreapprovalIncludingPlanAndReference() {
        server.expect(requestTo(BASE + "/preapproval/pre-9"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":"pre-9","status":"authorized","external_reference":null,"payer_email":"x@y.com","preapproval_plan_id":"2c93808"}
                        """, MediaType.APPLICATION_JSON));

        GatewaySubscription sub = gateway.getSubscription("pre-9");

        assertThat(sub.status()).isEqualTo("authorized");
        assertThat(sub.planId()).isEqualTo("2c93808");
        assertThat(sub.payerEmail()).isEqualTo("x@y.com");
        assertThat(sub.externalReference()).isNull();
    }

    @Test
    void wrapsMercadoPagoErrorsInGatewayException() {
        server.expect(requestTo(BASE + "/preapproval/pre-1"))
                .andRespond(withBadRequest().body("{\"message\":\"invalid status\"}").contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> gateway.updateSubscriptionStatus("pre-1", SubscriptionStatus.ACTIVE))
                .isInstanceOf(GatewayException.class)
                .hasMessageContaining("invalid status");
    }
}

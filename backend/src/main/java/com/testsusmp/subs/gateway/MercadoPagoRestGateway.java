package com.testsusmp.subs.gateway;

import com.testsusmp.subs.domain.SubscriptionStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Adaptador HTTP contra la API de suscripciones de Mercado Pago.
 * Docs: /preapproval_plan (planes) y /preapproval (suscripciones).
 */
public class MercadoPagoRestGateway implements PaymentGateway {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_PLANS = 1000;

    private final RestClient client;

    public MercadoPagoRestGateway(RestClient.Builder builder, String baseUrl, String accessToken) {
        this.client = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String mode() {
        return "mercadopago";
    }

    @Override
    public GatewayPlan createPlan(PlanSpec spec) {
        Map<String, Object> body = planBody(spec);
        Map<?, ?> res = call(() -> client.post().uri("/preapproval_plan")
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .body(body).retrieve().body(Map.class));
        return new GatewayPlan(str(res, "id"), str(res, "init_point"));
    }

    @Override
    public void updatePlan(String gatewayPlanId, PlanSpec spec) {
        Map<String, Object> body = planBody(spec);
        call(() -> client.put().uri("/preapproval_plan/{id}", gatewayPlanId).body(body).retrieve().toBodilessEntity());
    }

    @Override
    public void deactivatePlan(String gatewayPlanId) {
        call(() -> client.put().uri("/preapproval_plan/{id}", gatewayPlanId)
                .body(Map.of("status", "cancelled")).retrieve().toBodilessEntity());
    }

    @Override
    public List<GatewayPlanInfo> listActivePlans() {
        List<GatewayPlanInfo> plans = new ArrayList<>();
        for (int offset = 0; offset < MAX_PLANS; offset += PAGE_SIZE) {
            int currentOffset = offset;
            Map<?, ?> res = call(() -> client.get()
                    .uri("/preapproval_plan/search?status=active&limit={limit}&offset={offset}", PAGE_SIZE, currentOffset)
                    .retrieve().body(Map.class));
            List<?> results = res == null ? List.of() : (List<?>) res.get("results");
            if (results == null) {
                break;
            }
            for (Object item : results) {
                Map<?, ?> plan = (Map<?, ?>) item;
                Map<?, ?> recurring = plan.get("auto_recurring") instanceof Map<?, ?> m ? m : Map.of();
                Object amount = recurring.get("transaction_amount");
                plans.add(new GatewayPlanInfo(str(plan, "id"), str(plan, "reason"), str(plan, "status"),
                        amount == null ? null : new BigDecimal(amount.toString()), str(recurring, "currency_id"),
                        str(plan, "init_point")));
            }
            if (results.size() < PAGE_SIZE) {
                break;
            }
        }
        return plans;
    }

    @Override
    public GatewaySubscription createSubscription(SubscriptionSpec spec) {
        Map<String, Object> autoRecurring = new LinkedHashMap<>();
        autoRecurring.put("frequency", 1);
        autoRecurring.put("frequency_type", "months");
        autoRecurring.put("transaction_amount", spec.amount());
        autoRecurring.put("currency_id", spec.currency());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reason", spec.reason());
        body.put("external_reference", spec.externalReference());
        body.put("payer_email", spec.payerEmail());
        body.put("auto_recurring", autoRecurring);
        body.put("back_url", spec.backUrl());
        body.put("status", "pending");

        Map<?, ?> res = call(() -> client.post().uri("/preapproval")
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .body(body).retrieve().body(Map.class));
        return toSubscription(res);
    }

    @Override
    public GatewaySubscription getSubscription(String preapprovalId) {
        Map<?, ?> res = call(() -> client.get().uri("/preapproval/{id}", preapprovalId).retrieve().body(Map.class));
        return toSubscription(res);
    }

    @Override
    public void updateSubscriptionStatus(String preapprovalId, SubscriptionStatus status) {
        call(() -> client.put().uri("/preapproval/{id}", preapprovalId)
                .body(Map.of("status", status.toMercadoPago())).retrieve().toBodilessEntity());
    }

    @Override
    public void updateSubscriptionAmount(String preapprovalId, BigDecimal amount, String currency) {
        Map<String, Object> body = Map.of("auto_recurring", Map.of("transaction_amount", amount, "currency_id", currency));
        call(() -> client.put().uri("/preapproval/{id}", preapprovalId).body(body).retrieve().toBodilessEntity());
    }

    private static Map<String, Object> planBody(PlanSpec spec) {
        Map<String, Object> autoRecurring = new LinkedHashMap<>();
        autoRecurring.put("frequency", spec.frequencyMonths());
        autoRecurring.put("frequency_type", "months");
        autoRecurring.put("transaction_amount", spec.amount());
        autoRecurring.put("currency_id", spec.currency());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reason", spec.reason());
        body.put("auto_recurring", autoRecurring);
        body.put("back_url", spec.backUrl());
        return body;
    }

    private static GatewaySubscription toSubscription(Map<?, ?> res) {
        return new GatewaySubscription(str(res, "id"), str(res, "init_point"), str(res, "status"),
                str(res, "external_reference"), str(res, "payer_email"), str(res, "preapproval_plan_id"));
    }

    private static String str(Map<?, ?> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? null : value.toString();
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            throw new GatewayException("Mercado Pago respondio " + e.getStatusCode().value() + ": "
                    + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new GatewayException("No se pudo contactar a Mercado Pago: " + e.getMessage(), e);
        }
    }
}

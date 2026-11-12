package com.testsusmp.subs.api;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mercado Pago es la fuente de verdad de nombre, precio y estado de los planes. */
class PlanSyncApiTest extends ApiTestSupport {

    @Test
    void importsPlanCreatedDirectlyInMercadoPagoIntoFirstFreeTier() throws Exception {
        createPlan("Basico", "BASICO", "8000", 8);
        gateway.registerExternalPlan("ext-1", "Plan creado en MP", new BigDecimal("15000"));

        mvc.perform(post("/api/plans/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.updated").value(0))
                .andExpect(jsonPath("$.deactivated").value(0));

        mvc.perform(get("/api/plans?activeOnly=true"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].name").value("Plan creado en MP"))
                .andExpect(jsonPath("$[1].tier").value("PLUS"))
                .andExpect(jsonPath("$[1].price").value(15000.0))
                .andExpect(jsonPath("$[1].gatewayPlanId").value("ext-1"))
                .andExpect(jsonPath("$[1].minPermanenceMonths").value(3));
    }

    @Test
    void importsAsInactiveWhenAllThreeTiersAreTaken() throws Exception {
        createPlan("Basico", "BASICO", "8000", 8);
        createPlan("Plus", "PLUS", "15000", 20);
        createPlan("Premium", "PREMIUM", "25000", 40);
        gateway.registerExternalPlan("ext-4", "Cuarto plan", new BigDecimal("30000"));

        mvc.perform(post("/api/plans/sync")).andExpect(jsonPath("$.imported").value(1));

        mvc.perform(get("/api/plans")).andExpect(jsonPath("$[?(@.gatewayPlanId == 'ext-4')].active").value(false));
    }

    @Test
    void appliesNameAndPriceChangesMadeInMercadoPago() throws Exception {
        long id = createPlan("Plus", "PLUS", "15000", 20);
        String gatewayPlanId = body(mvc.perform(get("/api/plans/" + id))).get("gatewayPlanId").asText();
        gateway.editExternalPlan(gatewayPlanId, "Plus renovado", new BigDecimal("17500"));

        mvc.perform(post("/api/plans/sync")).andExpect(jsonPath("$.updated").value(1));

        mvc.perform(get("/api/plans/" + id))
                .andExpect(jsonPath("$.name").value("Plus renovado"))
                .andExpect(jsonPath("$.price").value(17500.0))
                .andExpect(jsonPath("$.monthlyHours").value(20));
    }

    @Test
    void deactivatesPlanCancelledInMercadoPago() throws Exception {
        long id = createPlan("Plus", "PLUS", "15000", 20);
        String gatewayPlanId = body(mvc.perform(get("/api/plans/" + id))).get("gatewayPlanId").asText();
        gateway.deactivatePlan(gatewayPlanId);

        mvc.perform(post("/api/plans/sync")).andExpect(jsonPath("$.deactivated").value(1));

        mvc.perform(get("/api/plans/" + id)).andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void syncIsIdempotent() throws Exception {
        createPlan("Plus", "PLUS", "15000", 20);
        gateway.registerExternalPlan("ext-1", "Externo", new BigDecimal("9000"));
        mvc.perform(post("/api/plans/sync"));

        mvc.perform(post("/api/plans/sync"))
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.updated").value(0))
                .andExpect(jsonPath("$.deactivated").value(0));
    }
}

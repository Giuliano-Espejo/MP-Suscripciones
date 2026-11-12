package com.testsusmp.subs.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlanApiTest extends ApiTestSupport {

    @Test
    void createsPlanAndRegistersItInTheGateway() throws Exception {
        postJson("/api/plans", """
                {"name":"Plus","description":"Coworking","tier":"PLUS","price":15000,"monthlyHours":20,
                 "benefits":["Sala de reuniones","Cafe libre"]}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tier").value("PLUS"))
                .andExpect(jsonPath("$.tierLevel").value(2))
                .andExpect(jsonPath("$.minPermanenceMonths").value(3))
                .andExpect(jsonPath("$.benefits", hasSize(2)))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.gatewayPlanId", startsWith("demo-plan-")));
    }

    @Test
    void rejectsInvalidPlan() throws Exception {
        postJson("/api/plans", """
                {"name":"","tier":"PLUS","price":-1,"monthlyHours":5}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyOneActivePlanPerTier() throws Exception {
        createPlan("Basico", "BASICO", "8000", 8);

        postJson("/api/plans", """
                {"name":"Otro basico","tier":"BASICO","price":9000,"monthlyHours":8}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe un plan activo para el nivel BASICO"));
    }

    @Test
    void updatesPlan() throws Exception {
        long id = createPlan("Premium", "PREMIUM", "25000", 40);

        mvc.perform(put("/api/plans/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Premium Full","tier":"PREMIUM","price":27000,"monthlyHours":50,"minPermanenceMonths":6,
                         "benefits":["Todo incluido"]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Premium Full"))
                .andExpect(jsonPath("$.price").value(27000))
                .andExpect(jsonPath("$.minPermanenceMonths").value(6))
                .andExpect(jsonPath("$.benefits", hasSize(1)));
    }

    @Test
    void deletingPlanDeactivatesItAndFreesTheTier() throws Exception {
        long id = createPlan("Plus", "PLUS", "15000", 20);

        mvc.perform(delete("/api/plans/" + id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/plans/" + id)).andExpect(jsonPath("$.active").value(false));
        createPlan("Plus 2", "PLUS", "16000", 20);
        mvc.perform(get("/api/plans")).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/plans?activeOnly=true")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void unknownPlanIs404() throws Exception {
        mvc.perform(get("/api/plans/999999")).andExpect(status().isNotFound());
    }
}

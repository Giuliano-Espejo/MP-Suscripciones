package com.testsusmp.subs.api;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConsumptionApiTest extends ApiTestSupport {

    @Test
    void consumptionsReduceAvailableHoursOfCurrentPeriod() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 10);
        long subId = subscribe("Ana Perez", "ana@example.com", planId);
        authorize(subId);

        postJson("/api/subscriptions/" + subId + "/consumptions", """
                {"hours":4,"description":"Sala de reuniones"}
                """).andExpect(status().isCreated());

        mvc.perform(get("/api/subscriptions/" + subId + "/consumptions"))
                .andExpect(jsonPath("$.hoursIncluded").value(10))
                .andExpect(jsonPath("$.hoursUsed").value(4.0))
                .andExpect(jsonPath("$.hoursAvailable").value(6.0))
                .andExpect(jsonPath("$.periodFrom").value("2026-11-11"))
                .andExpect(jsonPath("$.periodTo").value("2026-12-11"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.benefits", hasSize(2)));
        mvc.perform(get("/api/members/active")).andExpect(jsonPath("$[0].hoursAvailable").value(6.0));
    }

    @Test
    void consumingMoreThanAvailableIsRejected() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 10);
        long subId = subscribe("Ana Perez", "ana@example.com", planId);
        authorize(subId);
        postJson("/api/subscriptions/" + subId + "/consumptions", "{\"hours\":8,\"description\":\"Uso\"}");

        postJson("/api/subscriptions/" + subId + "/consumptions", "{\"hours\":3,\"description\":\"Uso\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void hoursResetOnNextPeriod() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 10);
        long subId = subscribe("Ana Perez", "ana@example.com", planId);
        authorize(subId);
        postJson("/api/subscriptions/" + subId + "/consumptions", "{\"hours\":10,\"description\":\"Uso\"}");

        clock.set(LocalDate.of(2026, 12, 11));

        mvc.perform(get("/api/subscriptions/" + subId + "/consumptions"))
                .andExpect(jsonPath("$.hoursAvailable").value(10.0))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void onlyActiveSubscriptionsCanConsume() throws Exception {
        long planId = createPlan("Plus", "PLUS", "10000", 10);
        long subId = subscribe("Ana Perez", "ana@example.com", planId);

        postJson("/api/subscriptions/" + subId + "/consumptions", "{\"hours\":1,\"description\":\"Uso\"}")
                .andExpect(status().isConflict());
    }
}

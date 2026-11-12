package com.testsusmp.subs.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testsusmp.subs.gateway.FakePaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiTestSupport.ClockConfig.class)
abstract class ApiTestSupport {

    static final LocalDate TODAY = LocalDate.of(2026, 11, 11);

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(TODAY);
        }
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    MutableClock clock;
    @Autowired
    FakePaymentGateway gateway;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void resetState() {
        clock.set(TODAY);
        gateway.reset();
        jdbc.execute("DELETE FROM consumptions");
        jdbc.execute("DELETE FROM subscriptions");
        jdbc.execute("DELETE FROM members");
        jdbc.execute("DELETE FROM plan_benefits");
        jdbc.execute("DELETE FROM plans");
    }

    JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    ResultActions postJson(String url, String content) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(content));
    }

    long createPlan(String name, String tier, String price, int hours) throws Exception {
        ResultActions result = postJson("/api/plans", """
                {"name":"%s","description":"Plan %s","tier":"%s","price":%s,"monthlyHours":%d,
                 "benefits":["Acceso al gimnasio","Wifi"]}
                """.formatted(name, name, tier, price, hours))
                .andExpect(status().isCreated());
        return body(result).get("id").asLong();
    }

    long subscribe(String fullName, String email, long planId) throws Exception {
        ResultActions result = postJson("/api/subscriptions", """
                {"fullName":"%s","email":"%s","planId":%d}
                """.formatted(fullName, email, planId))
                .andExpect(status().isCreated());
        return body(result).get("id").asLong();
    }

    void authorize(long subscriptionId) throws Exception {
        postJson("/api/demo/subscriptions/" + subscriptionId + "/simulate", "{\"status\":\"authorized\"}")
                .andExpect(status().isOk());
    }
}

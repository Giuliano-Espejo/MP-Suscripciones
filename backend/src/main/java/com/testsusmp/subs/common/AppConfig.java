package com.testsusmp.subs.common;

import com.testsusmp.subs.gateway.FakePaymentGateway;
import com.testsusmp.subs.gateway.MercadoPagoRestGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig {

    private static final String HAS_TOKEN = "'${app.mercadopago.access-token:}'.trim() != ''";

    @Bean
    Clock clock(@Value("${app.timezone}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }

    @Bean
    @ConditionalOnExpression(HAS_TOKEN)
    MercadoPagoRestGateway mercadoPagoGateway(RestClient.Builder builder, MercadoPagoProperties props) {
        return new MercadoPagoRestGateway(builder, props.baseUrl(), props.accessToken());
    }

    /** Sin access token: pasarela simulada en memoria para probar el tablero de punta a punta. */
    @Bean
    @ConditionalOnExpression("!(" + HAS_TOKEN + ")")
    FakePaymentGateway fakePaymentGateway() {
        return new FakePaymentGateway();
    }
}

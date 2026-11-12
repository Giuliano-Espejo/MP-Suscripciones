package com.testsusmp.subs.webhook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureValidatorTest {

    // Vectores generados con openssl: HMAC-SHA256 del manifest "id:{data.id};request-id:{x-request-id};ts:{ts};"
    private static final String SECRET = "secreto";

    @Test
    void acceptsValidSignature() {
        String header = "ts=1704908010,v1=450dd2f3a8c0afa6d1a806ad891a2020b7d974fb2c992c501e559fa353bdd98c";

        assertThat(WebhookSignatureValidator.isValid(header, "req-1", "123456", SECRET)).isTrue();
    }

    @Test
    void lowercasesAlphanumericDataIdAndToleratesSpaces() {
        String header = "ts=1704908011, v1=5e512491ecab9c4ec44d690edaf62b1b3d94b05bf96dcfc10a67cb7c1339baf5";

        assertThat(WebhookSignatureValidator.isValid(header, "req-2", "ABC123", SECRET)).isTrue();
    }

    @Test
    void rejectsTamperedDataId() {
        String header = "ts=1704908010,v1=450dd2f3a8c0afa6d1a806ad891a2020b7d974fb2c992c501e559fa353bdd98c";

        assertThat(WebhookSignatureValidator.isValid(header, "req-1", "999999", SECRET)).isFalse();
    }

    @Test
    void rejectsMissingOrMalformedHeader() {
        assertThat(WebhookSignatureValidator.isValid(null, "req-1", "123456", SECRET)).isFalse();
        assertThat(WebhookSignatureValidator.isValid("garbage", "req-1", "123456", SECRET)).isFalse();
    }
}

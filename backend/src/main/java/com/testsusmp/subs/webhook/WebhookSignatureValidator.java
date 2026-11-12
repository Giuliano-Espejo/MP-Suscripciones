package com.testsusmp.subs.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Valida el header x-signature de Mercado Pago ("ts=...,v1=...").
 * Manifest: "id:{data.id};request-id:{x-request-id};ts:{ts};" firmado con HMAC-SHA256 y la clave secreta.
 */
public final class WebhookSignatureValidator {

    private WebhookSignatureValidator() {
    }

    public static boolean isValid(String xSignature, String xRequestId, String dataId, String secret) {
        if (xSignature == null || secret == null || dataId == null) {
            return false;
        }
        String ts = null;
        String v1 = null;
        for (String part : xSignature.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            if (kv[0].equals("ts")) {
                ts = kv[1].trim();
            } else if (kv[0].equals("v1")) {
                v1 = kv[1].trim();
            }
        }
        if (ts == null || v1 == null) {
            return false;
        }
        String manifest = "id:" + dataId.toLowerCase(Locale.ROOT) + ";"
                + (xRequestId != null ? "request-id:" + xRequestId + ";" : "")
                + "ts:" + ts + ";";
        String expected = hmacSha256Hex(secret, manifest);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), v1.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular HMAC", e);
        }
    }
}

package com.manifestreader.common.util;

import com.manifestreader.common.exception.BizException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public final class JwtUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private JwtUtil() {
    }

    public static String generateToken(String subject, long expireSeconds, String secret) {
        long expireAt = Instant.now().getEpochSecond() + expireSeconds;
        String header = base64UrlEncode(HEADER_JSON);
        String payload = base64UrlEncode(buildPayload(subject, expireAt));
        String content = header + "." + payload;
        String signature = sign(content, secret);
        return content + "." + signature;
    }

    public static String getSubject(String token, String secret) {
        String payload = parsePayload(token, secret);
        return readValue(payload, "sub");
    }

    public static boolean isExpired(String token, String secret) {
        String payload = parsePayload(token, secret);
        String expValue = readValue(payload, "exp");
        long exp = Long.parseLong(expValue);
        return Instant.now().getEpochSecond() >= exp;
    }

    private static String parsePayload(String token, String secret) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BizException("INVALID_TOKEN", "Invalid token format");
        }
        String content = parts[0] + "." + parts[1];
        String actualSignature = sign(content, secret);
        if (!actualSignature.equals(parts[2])) {
            throw new BizException("INVALID_TOKEN", "Invalid token signature");
        }
        return new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
    }

    private static String buildPayload(String subject, long expireAt) {
        return "{\"sub\":\"" + escape(subject) + "\",\"exp\":" + expireAt + "}";
    }

    private static String readValue(String payload, String key) {
        String stringPattern = "\"" + key + "\":\"";
        int stringStart = payload.indexOf(stringPattern);
        if (stringStart >= 0) {
            int valueStart = stringStart + stringPattern.length();
            int valueEnd = payload.indexOf("\"", valueStart);
            return payload.substring(valueStart, valueEnd);
        }

        String numberPattern = "\"" + key + "\":";
        int numberStart = payload.indexOf(numberPattern);
        if (numberStart >= 0) {
            int valueStart = numberStart + numberPattern.length();
            int valueEnd = payload.indexOf(",", valueStart);
            if (valueEnd < 0) {
                valueEnd = payload.indexOf("}", valueStart);
            }
            return payload.substring(valueStart, valueEnd);
        }

        throw new BizException("INVALID_TOKEN", "Token payload missing key: " + key);
    }

    private static String sign(String content, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] signature = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception exception) {
            throw new BizException("TOKEN_SIGN_ERROR", "Failed to sign token");
        }
    }

    private static String base64UrlEncode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

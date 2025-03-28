package com.arhum.validator.config.auth;

import com.arhum.validator.exception.BaseException;
import com.arhum.validator.exception.InternalServerException;
import com.arhum.validator.exception.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

@Component
public class GlobalRequestFilter extends OncePerRequestFilter {

    private static final String SECRET_KEY = "your-very-secure-shared-secret";
    private static final long ALLOWABLE_TIMESTAMP_DRIFT = 5 * 60 * 1000; // 5 minutes -> millis

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (Objects.nonNull(authHeader)) {
            String token = authHeader.substring(7); // Remove "Bearer " prefix

            try {
                if (!validateToken(token)) {
                    throw new InternalServerException("Token is invalid or expired", 40000);
                }
            } catch (InternalServerException e) {
                throw new RuntimeException(e);
            }
        }
        filterChain.doFilter(request, response);

    }

    private boolean validateToken(String token) throws InternalServerException {

        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return false; // Invalid token format
        }

        String base64Payload = parts[0];
        String receivedSignature = parts[1];

        String expectedSignature = computeHMAC(base64Payload);

        if (!expectedSignature.equals(receivedSignature)) {
            return false;
        }

        String payloadJson = new String(Base64.getDecoder().decode(base64Payload));
        TokenPayload payload = parsePayload(payloadJson);

        long currentTime = System.currentTimeMillis();
        return Math.abs(currentTime - payload.timestamp) <= ALLOWABLE_TIMESTAMP_DRIFT; // Token expired
    }

    private String computeHMAC(String data) throws InternalServerException {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(hmacBytes);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new InternalServerException("Error computing HMAC: " + e.getMessage(), 60000);
        }
    }


    private TokenPayload parsePayload(String payloadJson) {
        // Simplified parsing; in production, use a library like Jackson or Gson
        String[] parts = payloadJson.replaceAll("[{}\"]", "").split(",");
        long timestamp = Long.parseLong(parts[0].split(":")[1]);
        String nonce = parts[1].split(":")[1];
        return new TokenPayload(timestamp, nonce);
    }

    private static class TokenPayload {
        public Long timestamp;
        public String nonce;

        public TokenPayload(Long timestamp, String nonce) {
            this.timestamp = timestamp;
            this.nonce = nonce;
        }
    }
}

package org.springframework.security.boot.jwt.authentication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import static org.junit.jupiter.api.Assertions.*;

class JacksonJwtCompatibilityTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void signedJwtJsonPreservesUnicodeAndClaimTypes() throws Exception {
        String header = encode(mapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
        String payload = encode(mapper.writeValueAsBytes(Map.of("sub", "用户", "roles", new String[]{"reader"}, "exp", 2000000000L)));
        String input = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(new byte[32], "HmacSHA256"));
        String jwt = input + "." + encode(mac.doFinal(input.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
        String[] parts = jwt.split("\\.");
        assertEquals(3, parts.length);
        assertArrayEquals(mac.doFinal(input.getBytes(java.nio.charset.StandardCharsets.US_ASCII)), Base64.getUrlDecoder().decode(parts[2]));
        JsonNode claims = mapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
        assertEquals("用户", claims.get("sub").asText());
        assertEquals("reader", claims.get("roles").get(0).asText());
        assertEquals(2000000000L, claims.get("exp").longValue());
        JwtAuthenticationToken token = new JwtAuthenticationToken(claims.get("sub").asText(), jwt);
        assertFalse(token.isAuthenticated());
        assertThrows(IllegalArgumentException.class, () -> token.setAuthenticated(true));
        token.eraseCredentials();
        assertNull(token.getCredentials());
    }

    @Test
    void rejectsMalformedClaimsAndDoesNotEnablePolymorphicTyping() throws Exception {
        assertThrows(JsonProcessingException.class, () -> mapper.readTree("{\"sub\": [}"));
        Object value = mapper.readValue("{\"@class\":\"java.lang.Runtime\",\"sub\":\"alice\"}", Object.class);
        assertInstanceOf(Map.class, value);
        assertEquals("alice", ((Map<?, ?>) value).get("sub"));
    }

    @Test
    void rejectsOverlyNestedUntrustedClaims() {
        String json = "[".repeat(1001) + "0" + "]".repeat(1001);
        assertThrows(com.fasterxml.jackson.core.exc.StreamConstraintsException.class,
                () -> mapper.readTree(json));
    }

    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

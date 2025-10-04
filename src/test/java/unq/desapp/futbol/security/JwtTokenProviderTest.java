package unq.desapp.futbol.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class JwtTokenProviderTest {

    @Test
    void generateToken_and_extractUsername_success() {
        // Arrange
        String secretBase64 = Base64.getEncoder().encodeToString(mockSecretBytes());
        JwtTokenProvider tokenProvider = new JwtTokenProvider(secretBase64, 3_600_000L);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("john.doe", "password");

        // Act
        String token = tokenProvider.generateToken(auth);
        String username = tokenProvider.getUsernameFromToken(token);

        // Assert
        assertEquals(username, "john.doe");
        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void validateToken_withInvalidToken_returnsFalse() {
        // Arrange
        String secretBase64 = Base64.getEncoder().encodeToString(mockSecretBytes());
        JwtTokenProvider tokenProvider = new JwtTokenProvider(secretBase64, 3_600_000L);
        String invalidToken = "this-is-not-a-jwt";

        // Act
        boolean isValid = tokenProvider.validateToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateToken_withExpiredToken_returnsFalse() {
        // Arrange
        String secretBase64 = Base64.getEncoder().encodeToString(mockSecretBytes());
        JwtTokenProvider expiredProvider = new JwtTokenProvider(secretBase64, -1_000L);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("alice", "pw");

        // Act
        String expiredToken = expiredProvider.generateToken(auth);

        // Assert
        assertFalse(expiredProvider.validateToken(expiredToken));
    }

    private byte[] mockSecretBytes() {
        byte[] secretBytes = new byte[32];

        for (int i = 0; i < secretBytes.length; i++) {
            secretBytes[i] = (byte) (i + 1);
        }

        return secretBytes;
    }
}

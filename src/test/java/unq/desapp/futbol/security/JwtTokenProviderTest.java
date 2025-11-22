package unq.desapp.futbol.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import unq.desapp.futbol.model.Role;
import unq.desapp.futbol.model.User;

class JwtTokenProviderTest {

    @Test
    void generateToken_and_extractUsername_success() {
        // Arrange
        String secretBase64 = Base64.getEncoder().encodeToString(mockSecretBytes());
        JwtTokenProvider tokenProvider = new JwtTokenProvider(secretBase64, 3_600_000L);
        User user = new User("john.doe", "password", "John", "Doe", Role.USER);

        // Act
        String token = tokenProvider.generateToken(user);
        String username = tokenProvider.getUsernameFromToken(token);

        // Assert
        assertEquals("john.doe", username);
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
        User user = new User("alice@example.com", "pw", "Alice", "Test", Role.USER);

        // Act
        String expiredToken = expiredProvider.generateToken(user);

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

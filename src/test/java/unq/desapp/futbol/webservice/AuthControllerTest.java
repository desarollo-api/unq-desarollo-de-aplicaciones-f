package unq.desapp.futbol.webservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import unq.desapp.futbol.model.AuthRequest;
import unq.desapp.futbol.model.AuthResponse;
import unq.desapp.futbol.security.JwtTokenProvider;

class AuthControllerTest {

    @Test
    void login_success_returnsToken_setsSecurityContext_andUsesBearerType() {
        // Arrange
        SecurityContextHolder.clearContext();

        String email = "user@example.com";
        String password = "secret";
        AuthRequest request = new AuthRequest(email, password);

        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);

        AuthenticationManager authenticationManager = Mockito.mock(AuthenticationManager.class);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);

        JwtTokenProvider jwtTokenProvider = Mockito.mock(JwtTokenProvider.class);
        String expectedToken = "header.payload.signature";
        long expectedExpiresIn = 3600L;
        when(jwtTokenProvider.generateToken(authentication)).thenReturn(expectedToken);
        when(jwtTokenProvider.getExpirationTime()).thenReturn(expectedExpiresIn);

        AuthController controller = new AuthController(authenticationManager, jwtTokenProvider);

        // Act
        ResponseEntity<AuthResponse> response = controller.login(request);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        AuthResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getToken()).isEqualTo(expectedToken);
        assertThat(body.getTokenType()).isEqualTo("Bearer");
        assertThat(body.getExpiresIn()).isEqualTo(expectedExpiresIn);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        Authentication authPassed = captor.getValue();
        assertThat(authPassed).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        UsernamePasswordAuthenticationToken upt = (UsernamePasswordAuthenticationToken) authPassed;
        assertThat(upt.getName()).isEqualTo(email);
        assertThat(String.valueOf(upt.getCredentials())).isEqualTo(password);

        verify(jwtTokenProvider).generateToken(authentication);
        verify(jwtTokenProvider).getExpirationTime();
    }

    @Test
    void login_failure_propagatesException_andDoesNotSetSecurityContext() {
        // Arrange
        SecurityContextHolder.clearContext();

        String email = "user@example.com";
        String password = "bad";
        AuthRequest request = new AuthRequest(email, password);

        AuthenticationManager authenticationManager = Mockito.mock(AuthenticationManager.class);
        when(authenticationManager.authenticate(any(Authentication.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        JwtTokenProvider jwtTokenProvider = Mockito.mock(JwtTokenProvider.class);
        AuthController controller = new AuthController(authenticationManager, jwtTokenProvider);

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> controller.login(request));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}

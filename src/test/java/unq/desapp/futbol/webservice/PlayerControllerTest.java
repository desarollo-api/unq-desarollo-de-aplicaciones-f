package unq.desapp.futbol.webservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import unq.desapp.futbol.model.PlayerPerformance;
import unq.desapp.futbol.model.Role;
import unq.desapp.futbol.model.SeasonPerformance;
import unq.desapp.futbol.model.User;
import unq.desapp.futbol.service.FootballDataService;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerController Tests")
class PlayerControllerTest {

        @Mock
        private FootballDataService footballDataService;

        private PlayerController playerController;

        private User testUser;

        @BeforeEach
        void setUp() {
                playerController = new PlayerController(footballDataService);
                testUser = new User("test@user.com", "pass", "Test", "User", Role.USER);
        }

        @Nested
        @DisplayName("getPlayerPerformance - Happy Path")
        class HappyPath {

                @Test
                @DisplayName("should return OK with player performance when player is found")
                void shouldReturnOkWithPerformanceWhenPlayerFound() {
                        // Arrange
                        String playerNameWithHyphens = "cristiano-ronaldo";
                        String expectedPlayerName = "cristiano ronaldo";

                        SeasonPerformance season = new SeasonPerformance("2022/2023", "Al-Nassr", "Saudi Pro League",
                                        30, 25, 10,
                                        8.5);
                        PlayerPerformance expectedPerformance = new PlayerPerformance(expectedPlayerName,
                                        Collections.singletonList(season));

                        when(footballDataService.getPlayerPerformance(expectedPlayerName, testUser))
                                        .thenReturn(Mono.just(expectedPerformance));

                        // Act
                        Mono<ResponseEntity<PlayerPerformance>> result = playerController
                                        .getPlayerPerformance(playerNameWithHyphens, testUser);

                        // Assert
                        StepVerifier.create(result)
                                        .assertNext(response -> {
                                                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                                                assertThat(response.getBody()).isNotNull();
                                                assertThat(response.getBody().getName()).isEqualTo(expectedPlayerName);
                                                assertThat(response.getBody().getSeasons()).hasSize(1);
                                                assertThat(response.getBody().getSeasons().get(0).season())
                                                                .isEqualTo("2022/2023");
                                        })
                                        .verifyComplete();

                        verify(footballDataService).getPlayerPerformance(expectedPlayerName, testUser);
                }

                @Test
                @DisplayName("should convert hyphens to spaces in player name")
                void shouldConvertHyphensToSpacesInPlayerName() {
                        // Arrange
                        String playerNameWithMultipleHyphens = "lionel-andres-messi";
                        String expectedPlayerName = "lionel andres messi";

                        when(footballDataService.getPlayerPerformance(expectedPlayerName, testUser))
                                        .thenReturn(Mono.just(new PlayerPerformance(expectedPlayerName,
                                                        Collections.emptyList())));

                        // Act
                        Mono<ResponseEntity<PlayerPerformance>> result = playerController
                                        .getPlayerPerformance(playerNameWithMultipleHyphens, testUser);

                        // Assert
                        StepVerifier.create(result)
                                        .expectNextCount(1)
                                        .verifyComplete();

                        verify(footballDataService).getPlayerPerformance(expectedPlayerName, testUser);
                }

                @Test
                @DisplayName("should handle player name without hyphens")
                void shouldHandlePlayerNameWithoutHyphens() {
                        // Arrange
                        String playerName = "neymar";
                        PlayerPerformance expectedPerformance = new PlayerPerformance(playerName,
                                        Collections.emptyList());

                        when(footballDataService.getPlayerPerformance(playerName, testUser))
                                        .thenReturn(Mono.just(expectedPerformance));

                        // Act
                        Mono<ResponseEntity<PlayerPerformance>> result = playerController
                                        .getPlayerPerformance(playerName, testUser);

                        // Assert
                        StepVerifier.create(result)
                                        .assertNext(response -> {
                                                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                                                assertThat(response.getBody()).isNotNull();
                                        })
                                        .verifyComplete();

                        verify(footballDataService).getPlayerPerformance(playerName, testUser);
                }
        }

        @Nested
        @DisplayName("getPlayerPerformance - Error and Not Found Cases")
        class ErrorCases {

                @Test
                @DisplayName("should return NOT_FOUND when service returns empty Mono")
                void shouldReturnNotFoundWhenServiceReturnsEmptyMono() {
                        // Arrange
                        String playerName = "unknown-player";
                        String expectedPlayerName = "unknown player";

                        when(footballDataService.getPlayerPerformance(anyString(), any(User.class)))
                                        .thenReturn(Mono.empty());

                        // Act
                        Mono<ResponseEntity<PlayerPerformance>> result = playerController
                                        .getPlayerPerformance(playerName, testUser);

                        // Assert
                        StepVerifier.create(result)
                                        .assertNext(response -> {
                                                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                                                assertThat(response.getBody()).isNull();
                                        })
                                        .verifyComplete();

                        verify(footballDataService).getPlayerPerformance(expectedPlayerName, testUser);
                }

                @Test
                @DisplayName("should propagate error when service throws exception")
                void shouldPropagateErrorWhenServiceThrowsException() {
                        // Arrange
                        String playerName = "error-player";
                        String expectedPlayerName = "error player";
                        RuntimeException expectedException = new RuntimeException("Service unavailable");

                        when(footballDataService.getPlayerPerformance(anyString(), any(User.class)))
                                        .thenReturn(Mono.error(expectedException));

                        // Act
                        Mono<ResponseEntity<PlayerPerformance>> result = playerController
                                        .getPlayerPerformance(playerName, testUser);

                        // Assert
                        StepVerifier.create(result)
                                        .expectError(RuntimeException.class)
                                        .verify();

                        verify(footballDataService).getPlayerPerformance(expectedPlayerName, testUser);
                }
        }
}

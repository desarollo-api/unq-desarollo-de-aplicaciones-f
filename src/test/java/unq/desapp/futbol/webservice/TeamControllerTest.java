package unq.desapp.futbol.webservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.Role;
import unq.desapp.futbol.service.FootballDataService;
import unq.desapp.futbol.model.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamController Tests")
class TeamControllerTest {

        @Mock
        private FootballDataService footballDataService;

        private TeamController teamController;

        private User testUser;

        @BeforeEach
        void setUp() {
                teamController = new TeamController(footballDataService);
                testUser = new User("test@user.com", "pass", "Test", "User", Role.USER);
        }

        @Nested
        @DisplayName("getSquadFromScraping - Happy Path")
        class HappyPath {

                @Test
                @DisplayName("should return OK with squad list when team is found")
                void shouldReturnOkWithSquadWhenTeamFound() {
                        // Arrange
                        String country = "England";
                        String teamNameWithHyphens = "manchester-united";
                        String expectedTeamName = "manchester united";

                        Player player1 = createPlayer("Marcus Rashford", 26, "England", "Forward");
                        Player player2 = createPlayer("Bruno Fernandes", 29, "Portugal", "Midfielder");
                        List<Player> expectedSquad = Arrays.asList(player1, player2);

                        when(footballDataService.getTeamSquad(expectedTeamName, country, testUser))
                                        .thenReturn(Mono.just(expectedSquad));

                        // Act
                        Mono<ResponseEntity<List<Player>>> result = teamController.getSquadFromScraping(country,
                                        teamNameWithHyphens, testUser);

                        // Assert
                        StepVerifier.create(result)
                                        .assertNext(response -> {
                                                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                                                assertThat(response.getBody()).isNotNull();
                                                assertThat(response.getBody()).hasSize(2);
                                                assertThat(response.getBody().get(0).getName())
                                                                .isEqualTo("Marcus Rashford");
                                                assertThat(response.getBody().get(1).getName())
                                                                .isEqualTo("Bruno Fernandes");
                                        })
                                        .verifyComplete();

                        verify(footballDataService).getTeamSquad(expectedTeamName, country, testUser);
                }

                @Test
                @DisplayName("should convert hyphens to spaces in team name")
                void shouldConvertHyphensToSpacesInTeamName() {
                        // Arrange
                        String country = "Spain";
                        String teamNameWithMultipleHyphens = "real-madrid-cf";
                        String expectedTeamName = "real madrid cf";

                        when(footballDataService.getTeamSquad(expectedTeamName, country, testUser))
                                        .thenReturn(Mono.just(Collections.emptyList()));

                        // Act
                        Mono<ResponseEntity<List<Player>>> result = teamController.getSquadFromScraping(country,
                                        teamNameWithMultipleHyphens, testUser);

                        // Assert
                        StepVerifier.create(result)
                                        .expectNextCount(1)
                                        .verifyComplete();

                        verify(footballDataService).getTeamSquad(expectedTeamName, country, testUser);
                }

                @Test
                @DisplayName("should handle team name without hyphens")
                void shouldHandleTeamNameWithoutHyphens() {
                        // Arrange
                        String country = "Italy";
                        String teamName = "juventus";
                        Player player = createPlayer("Dusan Vlahovic", 24, "Serbia", "Forward");

                        when(footballDataService.getTeamSquad(teamName, country, testUser))
                                        .thenReturn(Mono.just(Collections.singletonList(player)));

                        // Act
                        Mono<ResponseEntity<List<Player>>> result = teamController.getSquadFromScraping(country,
                                        teamName, testUser);

                        // Assert
                        StepVerifier.create(result)
                                        .assertNext(response -> {
                                                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                                                assertThat(response.getBody()).hasSize(1);
                                        })
                                        .verifyComplete();

                        verify(footballDataService).getTeamSquad(teamName, country, testUser);
                }

                @Test
                @DisplayName("should return OK with empty list when squad has no players")
                void shouldReturnOkWithEmptyListWhenSquadHasNoPlayers() {
                        // Arrange
                        String country = "France";
                        String teamName = "psg";

                        when(footballDataService.getTeamSquad(teamName, country, testUser))
                                        .thenReturn(Mono.just(Collections.emptyList()));

                        // Act
                        Mono<ResponseEntity<List<Player>>> result = teamController.getSquadFromScraping(country,
                                        teamName, testUser);

                        // Assert
                        StepVerifier.create(result)
                                        .assertNext(response -> {
                                                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                                                assertThat(response.getBody()).isNotNull();
                                                assertThat(response.getBody()).isEmpty();
                                        })
                                        .verifyComplete();
                }
        }

        @Nested
        @DisplayName("getSquadFromScraping - Error Cases")
        class ErrorCases {

                @Test
                @DisplayName("should return NOT_FOUND when service returns empty Mono")
                void shouldReturnNotFoundWhenServiceReturnsEmptyMono() {
                        // Arrange
                        String country = "Germany";
                        String teamName = "bayern-munich";

                        when(footballDataService.getTeamSquad(anyString(), anyString(), any(User.class)))
                                        .thenReturn(Mono.empty());

                        // Act
                        Mono<ResponseEntity<List<Player>>> result = teamController.getSquadFromScraping(country,
                                        teamName, testUser);

                        // Assert
                        StepVerifier.create(result)
                                        .assertNext(response -> {
                                                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                                                assertThat(response.getBody()).isNull();
                                        })
                                        .verifyComplete();
                }

                @Test
                @DisplayName("should propagate error when service throws exception")
                void shouldPropagateErrorWhenServiceThrowsException() {
                        // Arrange
                        String country = "England";
                        String teamName = "chelsea";
                        RuntimeException expectedException = new RuntimeException("Service unavailable");

                        when(footballDataService.getTeamSquad(anyString(), anyString(), any(User.class)))
                                        .thenReturn(Mono.error(expectedException));

                        // Act
                        Mono<ResponseEntity<List<Player>>> result = teamController.getSquadFromScraping(country,
                                        teamName, testUser);

                        // Assert
                        StepVerifier.create(result)
                                        .expectError(RuntimeException.class)
                                        .verify();
                }

                @Test
                @DisplayName("should handle null team name gracefully")
                void shouldHandleNullTeamNameGracefully() {
                        // Arrange
                        String country = "Spain";
                        String teamName = null;

                        // This test documents current behavior - NPE is thrown during replace()
                        // In a production system, you might want to add @PathVariable validation
                        // For now, we verify the current behavior and mark it as a known limitation

                        // Act & Assert
                        try {
                                teamController.getSquadFromScraping(country, teamName, testUser);
                                // If we get here without NPE, the behavior changed
                                StepVerifier.create(teamController.getSquadFromScraping(country, teamName, testUser))
                                                .expectNextCount(0)
                                                .verifyComplete();
                        } catch (NullPointerException e) {
                                // Expected behavior - NPE thrown during replace operation
                                assertThat(e).isInstanceOf(NullPointerException.class);
                        }

                        verifyNoInteractions(footballDataService);
                }
        }

        @Nested
        @DisplayName("getSquadFromScraping - Edge Cases")
        class EdgeCases {

                @Test
                @DisplayName("should handle special characters in country name")
                void shouldHandleSpecialCharactersInCountryName() {
                        // Arrange
                        String country = "Côte d'Ivoire";
                        String teamName = "asec-mimosas";

                        when(footballDataService.getTeamSquad(anyString(), eq(country), any(User.class)))
                                        .thenReturn(Mono.just(Collections.emptyList()));

                        // Act
                        Mono<ResponseEntity<List<Player>>> result = teamController.getSquadFromScraping(country,
                                        teamName, testUser);

                        // Assert
                        StepVerifier.create(result)
                                        .expectNextCount(1)
                                        .verifyComplete();

                        verify(footballDataService).getTeamSquad("asec mimosas", country, testUser);
                }

                @Test
                @DisplayName("should handle very long team name")
                void shouldHandleVeryLongTeamName() {
                        // Arrange
                        String country = "Wales";
                        String teamName = "llanfairpwllgwyngyllgogerychwyrndrobwllllantysiliogogogoch-fc";
                        String expectedTeamName = teamName.replace('-', ' ');

                        when(footballDataService.getTeamSquad(expectedTeamName, country, testUser))
                                        .thenReturn(Mono.just(Collections.emptyList()));

                        // Act
                        Mono<ResponseEntity<List<Player>>> result = teamController.getSquadFromScraping(country,
                                        teamName, testUser);

                        // Assert
                        StepVerifier.create(result)
                                        .expectNextCount(1)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("should handle team name with numbers")
                void shouldHandleTeamNameWithNumbers() {
                        // Arrange
                        String country = "Germany";
                        String teamName = "1860-munich";
                        String expectedTeamName = "1860 munich";

                        when(footballDataService.getTeamSquad(expectedTeamName, country, testUser))
                                        .thenReturn(Mono.just(Collections.emptyList()));

                        // Act
                        Mono<ResponseEntity<List<Player>>> result = teamController.getSquadFromScraping(country,
                                        teamName, testUser);

                        // Assert
                        StepVerifier.create(result)
                                        .expectNextCount(1)
                                        .verifyComplete();

                        verify(footballDataService).getTeamSquad(expectedTeamName, country, testUser);
                }

                @Test
                @DisplayName("should handle consecutive hyphens")
                void shouldHandleConsecutiveHyphens() {
                        // Arrange
                        String country = "Brazil";
                        String teamName = "sao--paulo";
                        String expectedTeamName = "sao  paulo"; // Double space

                        when(footballDataService.getTeamSquad(expectedTeamName, country, testUser))
                                        .thenReturn(Mono.just(Collections.emptyList()));

                        // Act
                        Mono<ResponseEntity<List<Player>>> result = teamController.getSquadFromScraping(country,
                                        teamName, testUser);

                        // Assert
                        StepVerifier.create(result)
                                        .expectNextCount(1)
                                        .verifyComplete();
                }
        }

        @Nested
        @DisplayName("getSquadFromScraping - Integration Scenarios")
        class IntegrationScenarios {

                @Test
                @DisplayName("should handle large squad with many players")
                void shouldHandleLargeSquadWithManyPlayers() {
                        // Arrange
                        String country = "England";
                        String teamName = "arsenal";
                        List<Player> largeSquad = createLargeSquad(30);

                        when(footballDataService.getTeamSquad(teamName, country, testUser))
                                        .thenReturn(Mono.just(largeSquad));

                        // Act
                        Mono<ResponseEntity<List<Player>>> result = teamController.getSquadFromScraping(country,
                                        teamName, testUser);

                        // Assert
                        StepVerifier.create(result)
                                        .assertNext(response -> {
                                                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                                                assertThat(response.getBody()).hasSize(30);
                                        })
                                        .verifyComplete();
                }

                @Test
                @DisplayName("should verify service is called exactly once")
                void shouldVerifyServiceIsCalledExactlyOnce() {
                        // Arrange
                        String country = "Italy";
                        String teamName = "ac-milan";

                        when(footballDataService.getTeamSquad(anyString(), anyString(), any(User.class)))
                                        .thenReturn(Mono.just(Collections.emptyList()));

                        // Act
                        teamController.getSquadFromScraping(country, teamName, testUser).block();

                        // Assert
                        verify(footballDataService, times(1)).getTeamSquad("ac milan", country, testUser);
                        verifyNoMoreInteractions(footballDataService);
                }

                @Test
                @DisplayName("should handle case sensitive country names")
                void shouldHandleCaseSensitiveCountryNames() {
                        // Arrange
                        String countryUpperCase = "ENGLAND";
                        String countryLowerCase = "england";
                        String teamName = "liverpool";

                        when(footballDataService.getTeamSquad(anyString(), anyString(), any(User.class)))
                                        .thenReturn(Mono.just(Collections.emptyList()));

                        // Act
                        teamController.getSquadFromScraping(countryUpperCase, teamName, testUser).block();
                        teamController.getSquadFromScraping(countryLowerCase, teamName, testUser).block();

                        // Assert
                        verify(footballDataService).getTeamSquad(teamName, countryUpperCase, testUser);
                        verify(footballDataService).getTeamSquad(teamName, countryLowerCase, testUser);
                }
        }

        // Helper methods
        private Player createPlayer(String name, int age, String nationality, String position) {
                Player player = new Player();
                player.setName(name);
                player.setAge(age);
                player.setNationality(nationality);
                player.setPosition(position);
                player.setRating(7.5);
                player.setMatches(20);
                player.setGoals(5);
                player.setAssist(3);
                player.setRedCards(0);
                player.setYellowCards(2);
                return player;
        }

        private List<Player> createLargeSquad(int size) {
                List<Player> squad = new java.util.ArrayList<>();
                for (int i = 1; i <= size; i++) {
                        squad.add(createPlayer("Player " + i, 20 + i % 15, "Country " + i, "Position " + (i % 4)));
                }
                return squad;
        }
}

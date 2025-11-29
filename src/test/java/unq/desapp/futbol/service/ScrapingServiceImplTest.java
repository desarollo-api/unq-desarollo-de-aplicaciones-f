package unq.desapp.futbol.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import unq.desapp.futbol.service.impl.ScrapingServiceImpl;

class ScrapingServiceImplTest {

        private WireMockServer wireMockServer;
        private ScrapingServiceImpl scrapingService;

        @BeforeEach
        void setUp() {
                // Arrange - Start WireMock server on random port
                wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
                wireMockServer.start();

                // Configure WireMock client
                configureFor("localhost", wireMockServer.port());

                // Create service instance with WireMock URL
                String baseUrl = "http://localhost:" + wireMockServer.port();
                scrapingService = new ScrapingServiceImpl(baseUrl);
        }

        @AfterEach
        void tearDown() {
                // Cleanup - Stop WireMock server
                if (wireMockServer != null && wireMockServer.isRunning()) {
                        wireMockServer.stop();
                }
        }

        // ==================== TEAM SQUAD TESTS ====================

        @Test
        void shouldReturnPlayersWhenScrapingTeamSquadSucceeds() {
                // Arrange
                String teamName = "River Plate";
                String country = "Argentina";

                // Mock search response
                stubFor(get(urlMatching("/search/.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "text/html")
                                                .withBody(loadTestResource("team-search-response.html"))));

                // Mock API response
                stubFor(get(urlMatching("/statisticsfeed/.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "application/json")
                                                .withBody(loadTestResource("team-squad-response.json"))));

                // Act & Assert
                StepVerifier.create(scrapingService.getTeamSquad(teamName, country))
                                .expectNextMatches(players -> {
                                        assertThat(players).isNotEmpty();
                                        assertThat(players.get(0).getName()).isNotBlank();
                                        return true;
                                })
                                .verifyComplete();
        }

        @Test
        void shouldReturnEmptyMonoWhenTeamNotFound() {
                // Arrange
                String teamName = "NonExistent Team";
                String country = "Unknown";

                // Mock search response with no results
                stubFor(get(urlMatching("/search/.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "text/html")
                                                .withBody("<html><body><div class=\"search-result\"></div></body></html>")));

                // Act & Assert
                StepVerifier.create(scrapingService.getTeamSquad(teamName, country))
                                .verifyComplete(); // Empty Mono
        }

        @Test
        void shouldReturnEmptyMonoWhenScrapingFailsForTeamSquad() {
                // Arrange
                String teamName = "Test Team";
                String country = "Test Country";

                // Mock server error
                stubFor(get(urlMatching("/search/.*"))
                                .willReturn(aResponse()
                                                .withStatus(500)
                                                .withBody("Internal Server Error")));

                // Act & Assert
                StepVerifier.create(scrapingService.getTeamSquad(teamName, country))
                                .verifyComplete(); // Empty Mono due to error handling
        }

        @Test
        void shouldReturnEmptyListWhenPlayerArrayIsEmpty() {
                // Arrange
                String teamName = "Empty Squad Team";
                String country = "Argentina";

                // Mock search response
                stubFor(get(urlMatching("/search/.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "text/html")
                                                .withBody(loadTestResource("team-search-response.html"))));

                // Mock API response with empty player array
                stubFor(get(urlMatching("/statisticsfeed/.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "application/json")
                                                .withBody("{\"playerTableStats\": []}")));

                // Act & Assert
                StepVerifier.create(scrapingService.getTeamSquad(teamName, country))
                                .expectNextMatches(players -> {
                                        assertThat(players).isEmpty();
                                        return true;
                                })
                                .verifyComplete();
        }

        // ==================== PLAYER PERFORMANCE TESTS ====================

        @Test
        void shouldReturnPlayerPerformanceWhenScrapingSucceeds() {
                // Arrange
                String playerName = "Lionel Messi";

                // Mock player search response
                stubFor(get(urlMatching("/search/.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "text/html")
                                                .withBody(loadTestResource("player-search-response.html"))));

                // Mock player stats API response
                stubFor(get(urlMatching("/statisticsfeed/.*playerId.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "application/json")
                                                .withBody(loadTestResource("player-performance-response.json"))));

                // Act & Assert
                StepVerifier.create(scrapingService.getPlayerPerformance(playerName))
                                .expectNextMatches(performance -> {
                                        assertThat(performance).isNotNull();
                                        assertThat(performance.getName()).isEqualTo(playerName);
                                        assertThat(performance.getSeasons()).isNotEmpty();
                                        return true;
                                })
                                .verifyComplete();
        }

        @Test
        void shouldReturnEmptyMonoWhenPlayerNotFound() {
                // Arrange
                String playerName = "Unknown Player";

                // Mock search response with no player results
                stubFor(get(urlMatching("/search/.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "text/html")
                                                .withBody("<html><body><div class=\"search-result\"></div></body></html>")));

                // Act & Assert
                StepVerifier.create(scrapingService.getPlayerPerformance(playerName))
                                .verifyComplete(); // Empty Mono
        }

        @Test
        void shouldReturnPlayerPerformanceWithEmptySeasonsWhenNoStatsAvailable() {
                // Arrange
                String playerName = "New Player";

                // Mock player search response
                stubFor(get(urlMatching("/search/.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "text/html")
                                                .withBody(loadTestResource("player-search-response.html"))));

                // Mock API response with empty stats array
                stubFor(get(urlMatching("/statisticsfeed/.*playerId.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "application/json")
                                                .withBody("{\"playerTableStats\": []}")));

                // Act & Assert
                StepVerifier.create(scrapingService.getPlayerPerformance(playerName))
                                .expectNextMatches(performance -> {
                                        assertThat(performance).isNotNull();
                                        assertThat(performance.getSeasons()).isEmpty();
                                        return true;
                                })
                                .verifyComplete();
        }

        @Test
        void shouldReturnEmptyMonoWhenScrapingFailsForPlayerPerformance() {
                // Arrange
                String playerName = "Test Player";

                // Mock server error
                stubFor(get(urlMatching("/search/.*"))
                                .willReturn(aResponse()
                                                .withStatus(500)
                                                .withBody("Internal Server Error")));

                // Act & Assert
                StepVerifier.create(scrapingService.getPlayerPerformance(playerName))
                                .verifyComplete(); // Empty Mono due to error handling
        }

        // ==================== UPCOMING MATCHES TESTS ====================

        @Test
        void shouldReturnUpcomingMatchesWhenScrapingSucceeds() {
                // Arrange
                String teamName = "Boca Juniors";
                String country = "Argentina";

                // Mock team search
                stubFor(get(urlMatching("/search/.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "text/html")
                                                .withBody(loadTestResource("team-search-response.html"))));

                // Mock fixtures page
                stubFor(get(urlMatching("/.*fixtures.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "text/html")
                                                .withBody(loadTestResource("fixtures-response.html"))));

                // Act & Assert
                StepVerifier.create(scrapingService.getUpcomingMatches(teamName, country))
                                .expectNextMatches(matches -> {
                                        assertThat(matches).isNotEmpty();
                                        return true;
                                })
                                .verifyComplete();
        }

        @Test
        void shouldReturnEmptyListWhenNoUpcomingMatches() {
                // Arrange
                String teamName = "Test Team";
                String country = "Argentina";

                // Mock team search
                stubFor(get(urlMatching("/search/.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "text/html")
                                                .withBody(loadTestResource("team-search-response.html"))));

                // Mock fixtures page with no upcoming matches
                stubFor(get(urlMatching("/.*fixtures.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "text/html")
                                                .withBody(loadTestResource("fixtures-empty-response.html"))));

                // Act & Assert
                StepVerifier.create(scrapingService.getUpcomingMatches(teamName, country))
                                .expectNextMatches(matches -> {
                                        assertThat(matches).isEmpty();
                                        return true;
                                })
                                .verifyComplete();
        }

        @Test
        void shouldReturnEmptyMonoWhenScrapingFailsForUpcomingMatches() {
                // Arrange
                String teamName = "Test Team";
                String country = "Argentina";

                // Mock server error
                stubFor(get(urlMatching("/search/.*"))
                                .willReturn(aResponse()
                                                .withStatus(500)
                                                .withBody("Internal Server Error")));

                // Act & Assert
                StepVerifier.create(scrapingService.getUpcomingMatches(teamName, country))
                                .verifyComplete(); // Empty Mono
        }

        // ==================== MATCH PREDICTION TESTS ====================

        @Test
        void shouldReturnMatchPredictionWhenScrapingSucceeds() {
                // Arrange
                String teamName = "River Plate";
                String country = "Argentina";

                // Mock team search
                stubFor(get(urlMatching("/search/.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "text/html")
                                                .withBody(loadTestResource("team-search-response.html"))));

                // Mock fixtures page
                stubFor(get(urlMatching("/.*fixtures.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "text/html")
                                                .withBody(loadTestResource("fixtures-response.html"))));

                // Mock match details page
                stubFor(get(urlMatching("/matches/.*/show"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "text/html")
                                                .withBody(loadTestResource("match-details-response.html"))));

                // Act & Assert
                StepVerifier.create(scrapingService.predictNextMatch(teamName, country))
                                .expectNextMatches(prediction -> {
                                        assertThat(prediction).isNotNull();
                                        assertThat(prediction.getHomeTeam()).isNotBlank();
                                        assertThat(prediction.getAwayTeam()).isNotBlank();
                                        return true;
                                })
                                .verifyComplete();
        }

        @Test
        void shouldReturnEmptyMonoWhenNoUpcomingMatchesForPrediction() {
                // Arrange
                String teamName = "Test Team";
                String country = "Argentina";

                // Mock team search
                stubFor(get(urlMatching("/search/.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "text/html")
                                                .withBody(loadTestResource("team-search-response.html"))));

                // Mock fixtures page with no upcoming matches
                stubFor(get(urlMatching("/.*fixtures.*"))
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "text/html")
                                                .withBody(loadTestResource("fixtures-empty-response.html"))));

                // Act & Assert
                StepVerifier.create(scrapingService.predictNextMatch(teamName, country))
                                .verifyComplete(); // Empty Mono (null)
        }

        @Test
        void shouldReturnEmptyMonoWhenScrapingFailsForMatchPrediction() {
                // Arrange
                String teamName = "Test Team";
                String country = "Argentina";

                // Mock server error
                stubFor(get(urlMatching("/search/.*"))
                                .willReturn(aResponse()
                                                .withStatus(500)
                                                .withBody("Internal Server Error")));

                // Act & Assert
                StepVerifier.create(scrapingService.predictNextMatch(teamName, country))
                                .verifyComplete(); // Empty Mono
        }

        // ==================== HELPER METHODS ====================

        /**
         * Loads a test resource file from src/test/resources/wiremock/
         */
        private String loadTestResource(String fileName) {
                try {
                        String resourcePath = "src/test/resources/wiremock/" + fileName;
                        return Files.readString(Paths.get(resourcePath));
                } catch (IOException e) {
                        // Return minimal valid response as fallback
                        if (fileName.endsWith(".json")) {
                                return "{}";
                        } else {
                                return "<html><body></body></html>";
                        }
                }
        }
}

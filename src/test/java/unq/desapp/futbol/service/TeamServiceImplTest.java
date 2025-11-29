package unq.desapp.futbol.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import unq.desapp.futbol.model.MatchPrediction;
import unq.desapp.futbol.model.TeamStats;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.PreviousMatch;
import unq.desapp.futbol.model.Role;
import unq.desapp.futbol.model.UpcomingMatch;
import unq.desapp.futbol.model.User;
import unq.desapp.futbol.service.impl.TeamServiceImpl;

class TeamServiceImplTest {

        private User testUser;
        private UserService userService;

        @BeforeEach
        void setUp() {
                testUser = new User("test@user.com", "password", "Test", "User", Role.USER);
                userService = mock(UserService.class);
        }

        @Test
        void shouldReturnTeamSquadWhenScrapingServiceReturnsPlayers() {
                // Arrange
                String teamName = "River Plate";
                String country = "Argentina";

                Player player1 = new Player("Franco Armani", 37, "Argentina", "Goalkeeper", 7.5, 30, 0, 0, 0, 2);
                Player player2 = new Player("Paulo Díaz", 29, "Chile", "Defender", 7.8, 28, 2, 1, 1, 5);
                Player player3 = new Player("Enzo Pérez", 38, "Argentina", "Midfielder", 7.2, 25, 1, 2, 0, 3);
                List<Player> expectedPlayers = List.of(player1, player2, player3);

                ScrapingService scrapingService = mock(ScrapingService.class);
                when(scrapingService.getTeamSquad(teamName, country))
                                .thenReturn(Mono.just(expectedPlayers));

                TeamServiceImpl teamService = new TeamServiceImpl(scrapingService, userService);

                // Act & Assert
                StepVerifier.create(teamService.getTeamSquad(teamName, country, testUser))
                                .expectNextMatches(players -> {
                                        assertThat(players).hasSize(3);
                                        assertThat(players.get(0).getName()).isEqualTo("Franco Armani");
                                        assertThat(players.get(0).getPosition()).isEqualTo("Goalkeeper");
                                        assertThat(players.get(1).getName()).isEqualTo("Paulo Díaz");
                                        assertThat(players.get(1).getNationality()).isEqualTo("Chile");
                                        assertThat(players.get(2).getName()).isEqualTo("Enzo Pérez");
                                        return true;
                                })
                                .verifyComplete();

                verify(scrapingService, times(1)).getTeamSquad(teamName, country);
        }

        @Test
        void shouldReturnEmptyListWhenScrapingServiceReturnsNoPlayers() {
                // Arrange
                String teamName = "Unknown Team";
                String country = "Argentina";

                ScrapingService scrapingService = mock(ScrapingService.class);
                when(scrapingService.getTeamSquad(teamName, country))
                                .thenReturn(Mono.just(Collections.emptyList()));

                TeamServiceImpl teamService = new TeamServiceImpl(scrapingService, userService);

                // Act & Assert
                StepVerifier.create(teamService.getTeamSquad(teamName, country, testUser))
                                .expectNextMatches(players -> {
                                        assertThat(players).isEmpty();
                                        return true;
                                })
                                .verifyComplete();

                verify(scrapingService, times(1)).getTeamSquad(teamName, country);
        }

        @Test
        void shouldPropagateErrorWhenScrapingServiceFailsForTeamSquad() {
                // Arrange
                String teamName = "River Plate";
                String country = "Argentina";
                RuntimeException expectedException = new RuntimeException("Scraping service error");

                ScrapingService scrapingService = mock(ScrapingService.class);
                when(scrapingService.getTeamSquad(teamName, country))
                                .thenReturn(Mono.error(expectedException));

                TeamServiceImpl teamService = new TeamServiceImpl(scrapingService, userService);

                // Act & Assert
                StepVerifier.create(teamService.getTeamSquad(teamName, country, testUser))
                                .expectErrorMatches(error -> error instanceof RuntimeException &&
                                                error.getMessage().equals("Scraping service error"))
                                .verify();

                verify(scrapingService, times(1)).getTeamSquad(teamName, country);
        }

        @Test
        void shouldReturnUpcomingMatchesWhenScrapingServiceReturnsMatches() {
                // Arrange
                String teamName = "Boca Juniors";
                String country = "Argentina";

                UpcomingMatch match1 = new UpcomingMatch("2024-10-20", "Liga Profesional", "Boca Juniors",
                                "River Plate");
                UpcomingMatch match2 = new UpcomingMatch("2024-10-27", "Copa Argentina", "Independiente",
                                "Boca Juniors");
                UpcomingMatch match3 = new UpcomingMatch("2024-11-03", "Liga Profesional", "Boca Juniors", "Racing");
                List<UpcomingMatch> expectedMatches = Arrays.asList(match1, match2, match3);

                ScrapingService scrapingService = mock(ScrapingService.class);
                when(scrapingService.getUpcomingMatches(teamName, country))
                                .thenReturn(Mono.just(expectedMatches));

                TeamServiceImpl teamService = new TeamServiceImpl(scrapingService, userService);

                // Act & Assert
                StepVerifier.create(teamService.getUpcomingMatches(teamName, country, testUser))
                                .expectNextMatches(matches -> {
                                        assertThat(matches).hasSize(3);
                                        assertThat(matches.get(0).getHomeTeam()).isEqualTo("Boca Juniors");
                                        assertThat(matches.get(0).getAwayTeam()).isEqualTo("River Plate");
                                        assertThat(matches.get(0).getCompetition()).isEqualTo("Liga Profesional");
                                        assertThat(matches.get(1).getAwayTeam()).isEqualTo("Boca Juniors");
                                        assertThat(matches.get(2).getDate()).isEqualTo("2024-11-03");
                                        return true;
                                })
                                .verifyComplete();

                verify(scrapingService, times(1)).getUpcomingMatches(teamName, country);
        }

        @Test
        void shouldReturnEmptyListWhenScrapingServiceReturnsNoMatches() {
                // Arrange
                String teamName = "Team Without Matches";
                String country = "Argentina";

                ScrapingService scrapingService = mock(ScrapingService.class);
                when(scrapingService.getUpcomingMatches(teamName, country))
                                .thenReturn(Mono.just(Collections.emptyList()));

                TeamServiceImpl teamService = new TeamServiceImpl(scrapingService, userService);

                // Act & Assert
                StepVerifier.create(teamService.getUpcomingMatches(teamName, country, testUser))
                                .expectNextMatches(matches -> {
                                        assertThat(matches).isEmpty();
                                        return true;
                                })
                                .verifyComplete();

                verify(scrapingService, times(1)).getUpcomingMatches(teamName, country);
        }

        @Test
        void shouldPropagateErrorWhenScrapingServiceFailsForUpcomingMatches() {
                // Arrange
                String teamName = "Boca Juniors";
                String country = "Argentina";
                RuntimeException expectedException = new RuntimeException("Network error");

                ScrapingService scrapingService = mock(ScrapingService.class);
                when(scrapingService.getUpcomingMatches(teamName, country))
                                .thenReturn(Mono.error(expectedException));

                TeamServiceImpl teamService = new TeamServiceImpl(scrapingService, userService);

                // Act & Assert
                StepVerifier.create(teamService.getUpcomingMatches(teamName, country, testUser))
                                .expectErrorMatches(error -> error instanceof RuntimeException &&
                                                error.getMessage().equals("Network error"))
                                .verify();

                verify(scrapingService, times(1)).getUpcomingMatches(teamName, country);
        }

        @Test
        void shouldHandleDifferentTeamNamesAndCountries() {
                // Arrange
                String teamName = "Barcelona";
                String country = "Spain";

                Player player = new Player("Lionel Messi", 36, "Argentina", "Forward", 9.5, 40, 35, 15, 0, 1);
                List<Player> expectedPlayers = Collections.singletonList(player);

                ScrapingService scrapingService = mock(ScrapingService.class);
                when(scrapingService.getTeamSquad(teamName, country))
                                .thenReturn(Mono.just(expectedPlayers));

                TeamServiceImpl teamService = new TeamServiceImpl(scrapingService, userService);

                // Act & Assert
                StepVerifier.create(teamService.getTeamSquad(teamName, country, testUser))
                                .expectNextMatches(players -> {
                                        assertThat(players).hasSize(1);
                                        assertThat(players.get(0).getName()).isEqualTo("Lionel Messi");
                                        return true;
                                })
                                .verifyComplete();

                verify(scrapingService, times(1)).getTeamSquad(teamName, country);
        }

        @Test
        void shouldHandleMultipleSequentialCallsToGetTeamSquad() {
                // Arrange
                String teamName = "River Plate";
                String country = "Argentina";

                Player player = new Player("Test Player", 25, "Argentina", "Forward", 7.0, 20, 10, 5, 0, 2);
                List<Player> expectedPlayers = Collections.singletonList(player);

                ScrapingService scrapingService = mock(ScrapingService.class);
                when(scrapingService.getTeamSquad(teamName, country))
                                .thenReturn(Mono.just(expectedPlayers));

                TeamServiceImpl teamService = new TeamServiceImpl(scrapingService, userService);

                // Act & Assert - First call
                StepVerifier.create(teamService.getTeamSquad(teamName, country, testUser))
                                .expectNextMatches(players -> players.size() == 1)
                                .verifyComplete();

                // Act & Assert - Second call
                StepVerifier.create(teamService.getTeamSquad(teamName, country, testUser))
                                .expectNextMatches(players -> players.size() == 1)
                                .verifyComplete();

                verify(scrapingService, times(2)).getTeamSquad(teamName, country);
        }

        @Test
        void shouldHandleMultipleSequentialCallsToGetUpcomingMatches() {
                // Arrange
                String teamName = "Boca Juniors";
                String country = "Argentina";

                UpcomingMatch match = new UpcomingMatch("2024-10-20", "Liga Profesional", "Boca Juniors",
                                "River Plate");
                List<UpcomingMatch> expectedMatches = Collections.singletonList(match);

                ScrapingService scrapingService = mock(ScrapingService.class);
                when(scrapingService.getUpcomingMatches(teamName, country))
                                .thenReturn(Mono.just(expectedMatches));

                TeamServiceImpl teamService = new TeamServiceImpl(scrapingService, userService);

                // Act & Assert - First call
                StepVerifier.create(teamService.getUpcomingMatches(teamName, country, testUser))
                                .expectNextMatches(matches -> matches.size() == 1)
                                .verifyComplete();

                // Act & Assert - Second call
                StepVerifier.create(teamService.getUpcomingMatches(teamName, country, testUser))
                                .expectNextMatches(matches -> matches.size() == 1)
                                .verifyComplete();

                verify(scrapingService, times(2)).getUpcomingMatches(teamName, country);
        }

        @Test
        void shouldHandleNullValuesInPlayerData() {
                // Arrange
                String teamName = "Test Team";
                String country = "Test Country";

                Player player = new Player("Player Name", null, null, "Forward", null, null, null, null, null, null);
                List<Player> expectedPlayers = Collections.singletonList(player);

                ScrapingService scrapingService = mock(ScrapingService.class);
                when(scrapingService.getTeamSquad(teamName, country))
                                .thenReturn(Mono.just(expectedPlayers));

                TeamServiceImpl teamService = new TeamServiceImpl(scrapingService, userService);

                // Act & Assert
                StepVerifier.create(teamService.getTeamSquad(teamName, country, testUser))
                                .expectNextMatches(players -> {
                                        assertThat(players).hasSize(1);
                                        assertThat(players.get(0).getName()).isEqualTo("Player Name");
                                        assertThat(players.get(0).getAge()).isNull();
                                        assertThat(players.get(0).getRating()).isNull();
                                        return true;
                                })
                                .verifyComplete();

                verify(scrapingService, times(1)).getTeamSquad(teamName, country);
        }

        @Test
        void shouldHandleNullValuesInMatchData() {
                // Arrange
                String teamName = "Test Team";
                String country = "Test Country";

                UpcomingMatch match = new UpcomingMatch("2024-10-20", null, "Home Team", "Away Team");
                List<UpcomingMatch> expectedMatches = Collections.singletonList(match);

                ScrapingService scrapingService = mock(ScrapingService.class);
                when(scrapingService.getUpcomingMatches(teamName, country))
                                .thenReturn(Mono.just(expectedMatches));

                TeamServiceImpl teamService = new TeamServiceImpl(scrapingService, userService);

                // Act & Assert
                StepVerifier.create(teamService.getUpcomingMatches(teamName, country, testUser))
                                .expectNextMatches(matches -> {
                                        assertThat(matches).hasSize(1);
                                        assertThat(matches.get(0).getDate()).isEqualTo("2024-10-20");
                                        assertThat(matches.get(0).getCompetition()).isNull();
                                        return true;
                                })
                                .verifyComplete();

                verify(scrapingService, times(1)).getUpcomingMatches(teamName, country);
        }

        // Tests for predictNextMatch method

        @Test
        void shouldReturnMatchPredictionWhenScrapingServiceReturnsData() {
                // Arrange
                String teamName = "River Plate";
                String country = "Argentina";

                PreviousMatch match1 = new PreviousMatch("2024-10-15", "Copa Libertadores", "River Plate", "2",
                                "Boca Juniors", "1");
                PreviousMatch match2 = new PreviousMatch("2024-09-20", "Liga Profesional", "Boca Juniors", "0",
                                "River Plate", "1");
                List<PreviousMatch> recentMeetings = Arrays.asList(match1, match2);
                MatchPrediction expectedPrediction = new MatchPrediction("River Plate", "Boca Juniors",
                                recentMeetings, "Victory for River Plate");

                ScrapingService scrapingService = mock(ScrapingService.class);
                when(scrapingService.predictNextMatch(teamName, country))
                                .thenReturn(Mono.just(expectedPrediction));

                TeamServiceImpl teamService = new TeamServiceImpl(scrapingService, userService);

                // Act & Assert
                StepVerifier.create(teamService.predictNextMatch(teamName, country, testUser))
                                .expectNextMatches(prediction -> {
                                        assertThat(prediction).isNotNull();
                                        assertThat(prediction.getHomeTeam()).isEqualTo("River Plate");
                                        assertThat(prediction.getAwayTeam()).isEqualTo("Boca Juniors");
                                        assertThat(prediction.getPredictedResult())
                                                        .isEqualTo("Victory for River Plate");
                                        assertThat(prediction.getRecentMeetings()).hasSize(2);
                                        return true;
                                })
                                .verifyComplete();

                verify(scrapingService, times(1)).predictNextMatch(teamName, country);
        }

        @Test
        void shouldReturnMatchPredictionWithNullUser() {
                // Arrange
                String teamName = "Barcelona";
                String country = "Spain";

                MatchPrediction expectedPrediction = new MatchPrediction("Barcelona", "Real Madrid",
                                Collections.emptyList(), "Draw");

                ScrapingService scrapingService = mock(ScrapingService.class);
                when(scrapingService.predictNextMatch(teamName, country))
                                .thenReturn(Mono.just(expectedPrediction));

                TeamServiceImpl teamService = new TeamServiceImpl(scrapingService, userService);

                // Act & Assert
                StepVerifier.create(teamService.predictNextMatch(teamName, country, null))
                                .expectNextMatches(prediction -> {
                                        assertThat(prediction).isNotNull();
                                        assertThat(prediction.getHomeTeam()).isEqualTo("Barcelona");
                                        return true;
                                })
                                .verifyComplete();

                verify(scrapingService, times(1)).predictNextMatch(teamName, country);
        }

        @Test
        void shouldReturnEmptyWhenScrapingServiceReturnsNullPrediction() {
                // Arrange
                String teamName = "Unknown Team";
                String country = "Unknown Country";

                ScrapingService scrapingService = mock(ScrapingService.class);
                when(scrapingService.predictNextMatch(teamName, country))
                                .thenReturn(Mono.empty());

                TeamServiceImpl teamService = new TeamServiceImpl(scrapingService, userService);

                // Act & Assert
                StepVerifier.create(teamService.predictNextMatch(teamName, country, testUser))
                                .verifyComplete();

                verify(scrapingService, times(1)).predictNextMatch(teamName, country);
        }

        @Test
        void shouldPropagateErrorWhenScrapingServiceFailsForMatchPrediction() {
                // Arrange
                String teamName = "Test Team";
                String country = "Test Country";
                RuntimeException expectedException = new RuntimeException("Prediction service error");

                ScrapingService scrapingService = mock(ScrapingService.class);
                when(scrapingService.predictNextMatch(teamName, country))
                                .thenReturn(Mono.error(expectedException));

                TeamServiceImpl teamService = new TeamServiceImpl(scrapingService, userService);

                // Act & Assert
                StepVerifier.create(teamService.predictNextMatch(teamName, country, testUser))
                                .expectErrorMatches(error -> error instanceof RuntimeException &&
                                                error.getMessage().equals("Prediction service error"))
                                .verify();

                verify(scrapingService, times(1)).predictNextMatch(teamName, country);
        }

        // Tests for getSingleTeamStats

        @Test
        void shouldReturnTeamStatsWhenScrapingServiceReturnsData() {
                // Arrange
                String teamName = "River Plate";
                String country = "Argentina";
                TeamStats expectedStats = new TeamStats(teamName, country);
                expectedStats.setSquadSize(25);
                expectedStats.setAverageAge(28.5);

                ScrapingService scrapingService = mock(ScrapingService.class);
                when(scrapingService.getTeamStats(teamName, country)).thenReturn(Mono.just(expectedStats));

                TeamServiceImpl teamService = new TeamServiceImpl(scrapingService, userService);

                // Act & Assert
                StepVerifier.create(teamService.getSingleTeamStats(teamName, country, testUser))
                                .expectNextMatches(stats -> {
                                        assertThat(stats).isNotNull();
                                        assertThat(stats.getTeamName()).isEqualTo(teamName);
                                        assertThat(stats.getSquadSize()).isEqualTo(25);
                                        assertThat(testUser.getSearchHistory()).hasSize(1);
                                        return true;
                                })
                                .verifyComplete();

                verify(scrapingService, times(1)).getTeamStats(teamName, country);
        }

        // Tests for compareTeams

        @Test
        void shouldReturnTeamComparisonWhenScrapingServiceReturnsSquads() {
                // Arrange
                String teamNameA = "River Plate";
                String countryA = "Argentina";
                String teamNameB = "Boca Juniors";
                String countryB = "Argentina";

                Player playerA = new Player("Franco Armani", 37, "Argentina", "Goalkeeper", 7.5, 30, 0, 0, 0, 2);
                Player playerB = new Player("Edinson Cavani", 37, "Uruguay", "Forward", 7.2, 20, 10, 2, 0, 1);

                List<Player> squadA = Collections.singletonList(playerA);
                List<Player> squadB = Collections.singletonList(playerB);

                ScrapingService scrapingService = mock(ScrapingService.class);
                when(scrapingService.getTeamSquad(teamNameA, countryA)).thenReturn(Mono.just(squadA));
                when(scrapingService.getTeamSquad(teamNameB, countryB)).thenReturn(Mono.just(squadB));

                TeamServiceImpl teamService = new TeamServiceImpl(scrapingService, userService);

                // Act & Assert
                StepVerifier.create(teamService.compareTeams(teamNameA, countryA, teamNameB, countryB, testUser))
                                .expectNextMatches(comparison -> {
                                        assertThat(comparison).isNotNull();
                                        assertThat(comparison.getTeamA().getTeamName()).isEqualTo(teamNameA);
                                        assertThat(comparison.getTeamB().getTeamName()).isEqualTo(teamNameB);
                                        assertThat(comparison.getTeamA().getAverageRating()).isEqualTo(7.5);
                                        assertThat(comparison.getTeamB().getTotalGoals()).isEqualTo(10);
                                        assertThat(testUser.getSearchHistory()).hasSize(1);
                                        return true;
                                })
                                .verifyComplete();
        }
}

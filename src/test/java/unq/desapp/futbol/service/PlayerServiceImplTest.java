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
import unq.desapp.futbol.model.PlayerPerformance;
import unq.desapp.futbol.model.Role;
import unq.desapp.futbol.model.SeasonPerformance;
import unq.desapp.futbol.model.User;
import unq.desapp.futbol.service.impl.PlayerServiceImpl;

class PlayerServiceImplTest {

    private User testUser;
    private UserService userService;
    private ScrapingService scrapingService;
    private PlayerServiceImpl playerService;

    @BeforeEach
    void setUp() {
        testUser = new User("test@user.com", "password", "Test", "User", Role.USER);
        userService = mock(UserService.class);
        scrapingService = mock(ScrapingService.class);
        playerService = new PlayerServiceImpl(scrapingService, userService);
    }

    @Test
    void shouldReturnPlayerPerformanceWhenScrapingServiceReturnsData() {
        // Arrange
        String playerName = "Lionel Messi";

        SeasonPerformance season1 = new SeasonPerformance("2023/2024", "Inter Miami", "MLS", 30, 25, 15, 8.5);
        SeasonPerformance season2 = new SeasonPerformance("2022/2023", "PSG", "Ligue 1", 35, 20, 18, 8.2);
        List<SeasonPerformance> seasons = Arrays.asList(season1, season2);
        PlayerPerformance expectedPerformance = new PlayerPerformance(playerName, seasons);

        when(scrapingService.findPlayerPerformance(playerName)).thenReturn(Mono.just(expectedPerformance));

        // Act & Assert
        StepVerifier.create(playerService.getPlayerPerformance(playerName, testUser))
                .expectNextMatches(performance -> {
                    assertThat(performance).isNotNull();
                    assertThat(performance.getName()).isEqualTo(playerName);
                    assertThat(performance.getSeasons()).hasSize(2);
                    assertThat(testUser.getSearchHistory()).hasSize(1);
                    assertThat(testUser.getSearchHistory().get(0).getQuery()).isEqualTo(playerName);
                    return true;
                })
                .verifyComplete();

        verify(scrapingService, times(1)).findPlayerPerformance(playerName);
    }

    @Test
    void shouldReturnPlayerPerformanceWithNullUser() {
        // Arrange
        String playerName = "Cristiano Ronaldo";

        SeasonPerformance season = new SeasonPerformance("2023/2024", "Al Nassr", "Saudi Pro League", 28, 22, 10, 8.0);
        List<SeasonPerformance> seasons = Collections.singletonList(season);
        PlayerPerformance expectedPerformance = new PlayerPerformance(playerName, seasons);

        when(scrapingService.findPlayerPerformance(playerName)).thenReturn(Mono.just(expectedPerformance));

        // Act & Assert
        StepVerifier.create(playerService.getPlayerPerformance(playerName, null))
                .expectNextMatches(performance -> {
                    assertThat(performance).isNotNull();
                    assertThat(performance.getName()).isEqualTo(playerName);
                    return true;
                })
                .verifyComplete();

        verify(scrapingService, times(1)).findPlayerPerformance(playerName);
    }

    @Test
    void shouldReturnEmptyWhenScrapingServiceReturnsNullPerformance() {
        // Arrange
        String playerName = "Unknown Player";

        when(scrapingService.findPlayerPerformance(playerName)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(playerService.getPlayerPerformance(playerName, testUser))
                .verifyComplete();

        verify(scrapingService, times(1)).findPlayerPerformance(playerName);
        assertThat(testUser.getSearchHistory()).isEmpty();
    }

    @Test
    void shouldPropagateErrorWhenScrapingServiceFailsForPlayerPerformance() {
        // Arrange
        String playerName = "Test Player";
        RuntimeException expectedException = new RuntimeException("Player data not found");

        when(scrapingService.findPlayerPerformance(playerName)).thenReturn(Mono.error(expectedException));

        // Act & Assert
        StepVerifier.create(playerService.getPlayerPerformance(playerName, testUser))
                .expectErrorMatches(
                        error -> error instanceof RuntimeException
                                && error.getMessage().equals("Player data not found"))
                .verify();

        verify(scrapingService, times(1)).findPlayerPerformance(playerName);
    }
}

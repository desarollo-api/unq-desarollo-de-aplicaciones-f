package unq.desapp.futbol.webservice;

import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.service.FootballDataService;

@AutoConfigureWebTestClient
class TeamControllerTest extends BaseControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private FootballDataService footballDataService;

    @Test
    void testGetSquadFromScraping_whenTeamExists_returnsOkWithSquad() {
        String country = "Argentina";
        String teamUrlName = "Boca-Juniors";
        String teamName = "Boca Juniors";
        Player player = new Player("Edinson Cavani", 37, "Uruguay", "Forward", 7.5, 30, 20, 5, 0, 2);
        List<Player> squad = Collections.singletonList(player);

        when(footballDataService.getTeamSquad(teamName, country)).thenReturn(Mono.just(squad));

        webTestClient.get().uri("/teams/{country}/{name}/squad", country, teamUrlName)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Player.class)
                .isEqualTo(squad);
    }

    @Test
    void testGetSquadFromScraping_whenTeamDoesNotExist_returnsNotFound() {
        String country = "Nowhere";
        String teamUrlName = "Non-Existent-Team";
        String teamName = "Non Existent Team";

        when(footballDataService.getTeamSquad(teamName, country)).thenReturn(Mono.empty());

        webTestClient.get().uri("/teams/{country}/{name}/squad", country, teamUrlName)
                .exchange()
                .expectStatus().isNotFound();
    }
}

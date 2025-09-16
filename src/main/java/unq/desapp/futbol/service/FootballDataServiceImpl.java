package unq.desapp.futbol.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.Team;
import unq.desapp.futbol.model.TeamDetails;
import unq.desapp.futbol.model.TeamListResponse;

@Service
public class FootballDataServiceImpl implements FootballDataService {

    private static final Logger logger = LoggerFactory.getLogger(FootballDataServiceImpl.class);
    private final WebClient webClient;
    private final ScrapingService scrapingService;

    public FootballDataServiceImpl(ScrapingService scrapingService,
                                   @Value("${football.api.baseurl}") String baseUrl,
                                   @Value("${football.api.token}") String apiToken
                                   ) {
        this.scrapingService = scrapingService;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Auth-Token", apiToken)
                .build();
    }

    @Override
    public Mono<Player> getPlayerById(Long id) {
        return this.webClient.get()
                .uri("/persons/{id}", id)
                .retrieve()
                .bodyToMono(Player.class)
                .flatMap(this::enrichPlayerWithRating)
                .doOnError(error -> logger.error("Error fetching player data for id {}: {}", id, error.getMessage()));
    }

    @Override
    public Flux<Team> getTeams() {
        return this.webClient.get()
                .uri("/teams")
                .retrieve()
                .bodyToMono(TeamListResponse.class)
                .flatMapMany(response -> Flux.fromIterable(response.getTeams()))
                .doOnError(error -> logger.error("Error fetching teams: {}", error.getMessage()));
    }

    @Override
    public Mono<TeamDetails> getTeamById(Long teamId) {
        return this.webClient.get()
                .uri("/teams/{id}", teamId)
                .retrieve()
                .bodyToMono(TeamDetails.class);
    }

    private Mono<Player> enrichPlayerWithRating(Player player) {
        return scrapingService.getPlayerRating(player.getName())
                .map(rating -> {
                    player.setRating(rating);
                    return player;
                })
                .defaultIfEmpty(player); // Si no se encuentra rating, devuelve el jugador original
    }
}

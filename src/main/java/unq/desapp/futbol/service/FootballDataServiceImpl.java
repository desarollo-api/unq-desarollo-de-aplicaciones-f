package unq.desapp.futbol.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Player;

@Service
public class FootballDataServiceImpl implements FootballDataService {

    private static final Logger logger = LoggerFactory.getLogger(FootballDataServiceImpl.class);

    private final WebClient webClient;

    public FootballDataServiceImpl(@Value("${football.api.baseurl}") String baseUrl,
                                   @Value("${football.api.token}") String apiToken) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Auth-Token", apiToken) // Set token for all requests from this client
                .build();
    }

    @Override
    public Mono<Player> getPlayerById(Long id) {
        return this.webClient.get()
                .uri("/persons/{id}", id)
                .retrieve()
                .bodyToMono(Player.class)
                .doOnError(error -> {
                    logger.error("Error fetching player data for id {}: {}", id, error.getMessage());
                });
    }
}

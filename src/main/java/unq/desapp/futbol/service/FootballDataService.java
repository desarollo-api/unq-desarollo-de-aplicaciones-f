package unq.desapp.futbol.service;

import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Player;

public interface FootballDataService {
    Mono<Player> getPlayerById(Long id);
}

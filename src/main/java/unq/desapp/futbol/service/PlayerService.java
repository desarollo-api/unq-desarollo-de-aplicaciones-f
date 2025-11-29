package unq.desapp.futbol.service;

import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.PlayerPerformance;
import unq.desapp.futbol.model.User;

public interface PlayerService {

    Mono<PlayerPerformance> getPlayerPerformance(String playerName, User user);
}

package unq.desapp.futbol.service;

import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.PlayerPerformance;

import java.util.List;

public interface FootballDataService {
    Mono<List<Player>> getTeamSquad(String teamName, String country);

    Mono<PlayerPerformance> getPlayerPerformance(String playerName);
}

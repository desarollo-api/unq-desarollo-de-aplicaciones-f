package unq.desapp.futbol.service;

import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Match;
import unq.desapp.futbol.model.Player;

import java.util.List;

public interface FootballDataService {
    Mono<List<Player>> getTeamSquad(String teamName, String country);
    Mono<List<Match>> getUpcomingMatches(String teamName, String country);
}

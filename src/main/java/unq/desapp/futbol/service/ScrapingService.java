package unq.desapp.futbol.service;

import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Match;
import unq.desapp.futbol.model.MatchPrediction;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.PlayerPerformance;
import java.util.List;

public interface ScrapingService {
    Mono<List<Player>> getTeamSquad(String teamName, String country);

    Mono<List<Match>> getUpcomingMatches(String teamName, String country);

    Mono<PlayerPerformance> getPlayerPerformance(String playerName);

    Mono<MatchPrediction> predictNextMatch(String teamName, String country);
}

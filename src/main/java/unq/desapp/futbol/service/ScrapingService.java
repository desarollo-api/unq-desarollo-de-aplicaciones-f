package unq.desapp.futbol.service;

import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.UpcomingMatch;
import unq.desapp.futbol.model.MatchPrediction;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.TeamStats;
import unq.desapp.futbol.model.PlayerPerformance;
import java.util.List;

public interface ScrapingService {
    Mono<List<Player>> findTeamSquad(String teamName, String country);

    Mono<List<UpcomingMatch>> findUpcomingMatches(String teamName, String country);

    Mono<PlayerPerformance> findPlayerPerformance(String playerName);

    Mono<TeamStats> findTeamStats(String teamName, String country);

    Mono<MatchPrediction> predictNextMatch(String teamName, String country);
}

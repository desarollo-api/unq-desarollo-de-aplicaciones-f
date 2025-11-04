package unq.desapp.futbol.service;

import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Match;
import unq.desapp.futbol.model.MatchPrediction;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.PlayerPerformance;
import unq.desapp.futbol.model.User;
import java.util.List;

public interface FootballDataService {

    Mono<List<Match>> getUpcomingMatches(String teamName, String country, User user);

    Mono<List<Player>> getTeamSquad(String teamName, String country, User user);

    Mono<PlayerPerformance> getPlayerPerformance(String playerName, User user);

    Mono<MatchPrediction> predictNextMatch(String teamName, String country, User user);
}

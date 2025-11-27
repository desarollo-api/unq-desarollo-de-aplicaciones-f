package unq.desapp.futbol.service;

import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.UpcomingMatch;
import unq.desapp.futbol.model.MatchPrediction;
import unq.desapp.futbol.model.TeamComparison;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.PlayerPerformance;
import unq.desapp.futbol.model.User;
import java.util.List;

public interface FootballDataService {

    Mono<List<UpcomingMatch>> getUpcomingMatches(String teamName, String country, User user);

    Mono<List<Player>> getTeamSquad(String teamName, String country, User user);

    Mono<PlayerPerformance> getPlayerPerformance(String playerName, User user);

    Mono<MatchPrediction> predictNextMatch(String teamName, String country, User user);

    Mono<TeamComparison> compareTeams(String teamNameA, String countryA, String teamNameB, String countryB,
            User user);
}

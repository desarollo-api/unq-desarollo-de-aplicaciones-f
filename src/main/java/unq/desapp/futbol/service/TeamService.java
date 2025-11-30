package unq.desapp.futbol.service;

import java.util.List;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.MatchPrediction;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.TeamComparisonResponse;
import unq.desapp.futbol.model.TeamStats;
import unq.desapp.futbol.model.UpcomingMatch;
import unq.desapp.futbol.model.User;

public interface TeamService {

    Mono<List<UpcomingMatch>> getUpcomingMatches(String teamName, String country, User user);

    Mono<List<Player>> getTeamSquad(String teamName, String country, User user);

    Mono<MatchPrediction> getNextMatchPrediction(String teamName, String country, User user);

    Mono<TeamComparisonResponse> getTeamsComparasion(String teamNameA, String countryA, String teamNameB,
            String countryB,
            User user);

    Mono<TeamStats> getSingleTeamStats(String teamName, String country, User user);
}

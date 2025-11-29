package unq.desapp.futbol.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.MatchPrediction;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.SearchType;
import unq.desapp.futbol.model.TeamComparison;
import unq.desapp.futbol.model.TeamStats;
import unq.desapp.futbol.model.UpcomingMatch;
import unq.desapp.futbol.model.User;
import unq.desapp.futbol.service.ScrapingService;
import unq.desapp.futbol.service.TeamService;
import unq.desapp.futbol.service.UserService;

@Service
public class TeamServiceImpl implements TeamService {

    private final ScrapingService scrapingService;
    private final UserService userService;

    public TeamServiceImpl(ScrapingService scrapingService, UserService userService) {
        this.scrapingService = scrapingService;
        this.userService = userService;
    }

    @Override
    public Mono<List<Player>> getTeamSquad(String teamName, String country, User user) {
        return scrapingService.getTeamSquad(teamName, country).doOnSuccess(squad -> {
            if (squad != null && !squad.isEmpty() && user != null) {
                user.addSearchHistory(SearchType.TEAM, teamName + " (" + country + ")");
                userService.saveUser(user);
            }
        });
    }

    @Override
    public Mono<List<UpcomingMatch>> getUpcomingMatches(String teamName, String country, User user) {
        return scrapingService.getUpcomingMatches(teamName, country).doOnSuccess(matches -> {
            if (matches != null && !matches.isEmpty() && user != null) {
                user.addSearchHistory(SearchType.TEAM, teamName + " (" + country + ")");
                userService.saveUser(user);
            }
        });
    }

    @Override
    public Mono<MatchPrediction> predictNextMatch(String teamName, String country, User user) {
        return scrapingService.predictNextMatch(teamName, country);
    }

    @Override
    public Mono<TeamStats> getSingleTeamStats(String teamName, String country, User user) {
        return scrapingService.getTeamStats(teamName, country).doOnSuccess(stats -> {
            if (stats != null && stats.getSquadSize() > 0 && user != null) {
                String query = String.format("%s (%s) stats", teamName, country);
                user.addSearchHistory(SearchType.TEAM, query);
                userService.saveUser(user);
            }
        });
    }

    @Override
    public Mono<TeamComparison> compareTeams(String teamNameA, String countryA, String teamNameB, String countryB,
            User user) {
        Mono<List<Player>> squadA = scrapingService.getTeamSquad(teamNameA, countryA);
        Mono<List<Player>> squadB = scrapingService.getTeamSquad(teamNameB, countryB);

        return Mono.zip(squadA, squadB).map(tuple -> {
            List<Player> playersA = tuple.getT1();
            List<Player> playersB = tuple.getT2();

            TeamStats statsA = calculateTeamStats(teamNameA, countryA, playersA);
            TeamStats statsB = calculateTeamStats(teamNameB, countryB, playersB);

            String verdict = generateVerdict(statsA, statsB);

            return new TeamComparison(statsA, statsB, verdict);
        }).doOnSuccess(comparison -> {
            if (user != null) {
                String query = String.format("%s (%s) vs %s (%s)", teamNameA, countryA, teamNameB, countryB);
                user.addSearchHistory(SearchType.TEAM, query);
                userService.saveUser(user);
            }
        });
    }

    private TeamStats calculateTeamStats(String teamName, String country, List<Player> players) {
        TeamStats stats = new TeamStats(teamName, country);
        if (players == null || players.isEmpty()) {
            return stats;
        }

        stats.setSquadSize(players.size());
        stats.setAverageAge(
                players.stream().map(Player::getAge).filter(java.util.Objects::nonNull).mapToInt(a -> a).average()
                        .orElse(0.0));
        stats.setAverageRating(players.stream().map(Player::getRating).filter(java.util.Objects::nonNull)
                .mapToDouble(r -> r).average().orElse(0.0));
        stats.setTotalGoals(
                players.stream().map(Player::getGoals).filter(java.util.Objects::nonNull).mapToInt(g -> g).sum());
        stats.setTotalAssists(
                players.stream().map(Player::getAssist).filter(java.util.Objects::nonNull).mapToInt(a -> a).sum());

        return stats;
    }

    private String generateVerdict(TeamStats statsA, TeamStats statsB) {
        double ratingDiff = statsA.getAverageRating() - statsB.getAverageRating();
        int goalsDiff = statsA.getTotalGoals() - statsB.getTotalGoals();

        if (Math.abs(ratingDiff) < 0.1) {
            return "Ambos equipos parecen tener un nivel muy similar.";
        } else if (ratingDiff > 0 && goalsDiff > 0) {
            return String.format("%s parece superior, con un mejor rating promedio y más goles en su plantilla.",
                    statsA.getTeamName());
        } else {
            return String.format("%s parece superior, con un mejor rating promedio y más goles en su plantilla.",
                    statsB.getTeamName());
        }
    }
}

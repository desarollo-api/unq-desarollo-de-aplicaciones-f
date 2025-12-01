package unq.desapp.futbol.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.MatchPrediction;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.TeamComparisonDetails;
import unq.desapp.futbol.model.TeamComparisonResponse;
import unq.desapp.futbol.model.SearchType;
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
        return scrapingService.findTeamSquad(teamName, country).doOnSuccess(squad -> {
            if (squad != null && !squad.isEmpty() && user != null) {
                user.addSearchHistory(SearchType.TEAM, teamName + " (" + country + ")");
                userService.saveUser(user);
            }
        });
    }

    @Override
    public Mono<List<UpcomingMatch>> getUpcomingMatches(String teamName, String country, User user) {
        return scrapingService.findUpcomingMatches(teamName, country).doOnSuccess(matches -> {
            if (matches != null && !matches.isEmpty() && user != null) {
                user.addSearchHistory(SearchType.TEAM, teamName + " (" + country + ")");
                userService.saveUser(user);
            }
        });
    }

    @Override
    public Mono<MatchPrediction> getNextMatchPrediction(String teamName, String country, User user) {
        return scrapingService.predictNextMatch(teamName, country)
                .doOnSuccess(prediction -> {
                    if (prediction != null && user != null) {
                        user.addSearchHistory(SearchType.TEAM, teamName + " (" + country + ")");
                        userService.saveUser(user);
                    }
                });
    }

    @Override
    public Mono<TeamStats> getSingleTeamStats(String teamName, String country, User user) {
        return scrapingService.findTeamStats(teamName, country).doOnSuccess(stats -> {
            if (stats != null && stats.getBestPlayer() != null && user != null) {
                String query = String.format("%s (%s) stats", teamName, country);
                user.addSearchHistory(SearchType.TEAM, query);
                userService.saveUser(user);
            }
        });
    }

    @Override
    public Mono<TeamComparisonResponse> getTeamsComparasion(String teamNameA, String countryA, String teamNameB,
            String countryB,
            User user) {
        Mono<TeamStats> monoStatsA = scrapingService.findTeamStats(teamNameA, countryA);
        Mono<TeamStats> monoStatsB = scrapingService.findTeamStats(teamNameB, countryB);

        return Mono.zip(monoStatsA, monoStatsB).map(tuple -> {
            TeamStats teamStatsA = tuple.getT1();
            TeamStats teamStatsB = tuple.getT2();

            return buildComparisonResponse(teamStatsA, teamStatsB);

        }).doOnSuccess(comparison -> {
            if (user != null) {
                String query = String.format("%s (%s) vs %s (%s)", teamNameA, countryA, teamNameB, countryB);
                user.addSearchHistory(SearchType.TEAM, query);
                userService.saveUser(user);
            }
        });
    }

    private TeamComparisonResponse buildComparisonResponse(TeamStats statsA, TeamStats statsB) {
        TeamComparisonDetails detailsA = new TeamComparisonDetails(statsA.getTeamName(), statsA.getCountry());
        TeamComparisonDetails detailsB = new TeamComparisonDetails(statsB.getTeamName(), statsB.getCountry());

        // Comparar Average Age
        detailsA.setAverageAge(
                compareAndDescribe(statsA.getAverageAge(), statsB.getAverageAge(), "average age", false));
        detailsB.setAverageAge(
                compareAndDescribe(statsB.getAverageAge(), statsA.getAverageAge(), "average age", false));

        // Comparar Average Rating
        detailsA.setAverageRating(
                compareAndDescribe(statsA.getAverageRating(), statsB.getAverageRating(), "average rating", true));
        detailsB.setAverageRating(
                compareAndDescribe(statsB.getAverageRating(), statsA.getAverageRating(), "average rating", true));

        // Comparar Win Rate
        detailsA.setWinRate(compareAndDescribe(statsA.getWinRate(), statsB.getWinRate(), "win rate", true) + "%");
        detailsB.setWinRate(compareAndDescribe(statsB.getWinRate(), statsA.getWinRate(), "win rate", true) + "%");

        detailsA.setBestPlayer(statsA.getBestPlayer());
        detailsB.setBestPlayer(statsB.getBestPlayer());

        String verdict = generateVerdict(statsA, statsB);

        return new TeamComparisonResponse(detailsA, detailsB, verdict);
    }

    private String compareAndDescribe(double valueA, double valueB, String metric, boolean higherIsBetter) {
        String comparison = (valueA > valueB) ? (higherIsBetter ? "Higher" : "Lower")
                : (valueA < valueB ? (higherIsBetter ? "Lower" : "Higher") : "Same");
        return String.format("%s %s (%.1f vs %.1f)", comparison, metric, valueA, valueB);
    }

    private String generateVerdict(TeamStats statsA, TeamStats statsB) {
        double ratingDiff = statsA.getAverageRating() - statsB.getAverageRating();
        double winRateDiff = statsA.getWinRate() - statsB.getWinRate();

        if (Math.abs(ratingDiff) < 0.1 && Math.abs(winRateDiff) < 2) {
            return "Both teams seem to have a very similar level in rating and win rate.";
        }

        if (ratingDiff > 0.2 && winRateDiff > 5) {
            return String.format(
                    "%s seems superior, with a noticeably higher average rating and win rate.",
                    statsA.getTeamName());
        } else if (ratingDiff < -0.2 && winRateDiff < -5) {
            return String.format(
                    "%s seems superior, with a noticeably higher average rating and win rate.",
                    statsB.getTeamName());
        } else {
            String betterRatingTeam = ratingDiff > 0 ? statsA.getTeamName() : statsB.getTeamName();
            String betterWinRateTeam = winRateDiff > 0 ? statsA.getTeamName() : statsB.getTeamName();
            return String.format(
                    "The comparison is close. %s has a better average rating, while %s has a higher win rate.",
                    betterRatingTeam, betterWinRateTeam);
        }
    }
}

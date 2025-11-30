package unq.desapp.futbol.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.UpcomingMatch;
import unq.desapp.futbol.model.MatchPrediction;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.SearchType;
import unq.desapp.futbol.model.User;
import unq.desapp.futbol.model.PlayerPerformance;
import java.util.List;

@Service
public class FootballDataServiceImpl implements FootballDataService {
    private final ScrapingService scrapingService;
    private final UserService userService;

    public FootballDataServiceImpl(ScrapingService scrapingService, UserService userService) {
        this.scrapingService = scrapingService;
        this.userService = userService;
    }

    @Override
    public Mono<List<Player>> getTeamSquad(String teamName, String country, User user) {
        return scrapingService.getTeamSquad(teamName, country)
                .doOnSuccess(squad -> {
                    if (squad != null && !squad.isEmpty() && user != null) {
                        user.addSearchHistory(SearchType.TEAM, teamName + " (" + country + ")");
                        userService.saveUser(user);
                    }
                });
    }

    @Override
    public Mono<PlayerPerformance> getPlayerPerformance(String playerName, User user) {
        return scrapingService.getPlayerPerformance(playerName)
                .doOnSuccess(performance -> {
                    if (performance != null && user != null) {
                        user.addSearchHistory(SearchType.PLAYER, playerName);
                        userService.saveUser(user);
                    }
                });
    }

    @Override
    public Mono<List<UpcomingMatch>> getUpcomingMatches(String teamName, String country, User user) {
        return scrapingService.getUpcomingMatches(teamName, country)
                .doOnSuccess(matches -> {
                    if (matches != null && !matches.isEmpty() && user != null) {
                        user.addSearchHistory(SearchType.TEAM, teamName + " (" + country + ")");
                        userService.saveUser(user);
                    }
                });
    }

    @Override
    public Mono<MatchPrediction> predictNextMatch(String teamName, String country, User user) {
        return scrapingService.predictNextMatch(teamName, country)
                .doOnSuccess(prediction -> {
                    if (prediction != null && user != null) {
                        user.addSearchHistory(SearchType.TEAM, teamName + " (" + country + ")");
                        userService.saveUser(user);
                    }
                });
    }
}

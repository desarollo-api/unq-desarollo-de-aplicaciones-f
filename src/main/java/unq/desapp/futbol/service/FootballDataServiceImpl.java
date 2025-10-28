package unq.desapp.futbol.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.User;
import unq.desapp.futbol.model.PlayerPerformance;
import java.util.List;

@Service
public class FootballDataServiceImpl implements FootballDataService {
    private final ScrapingService scrapingService;

    public FootballDataServiceImpl(ScrapingService scrapingService,
            @Value("${football.api.baseurl}") String baseUrl,
            @Value("${football.api.token}") String apiToken) {
        this.scrapingService = scrapingService;
    }

    @Override
    public Mono<List<Player>> getTeamSquad(String teamName, String country, User user) {
        return scrapingService.getTeamSquad(teamName, country)
                .doOnSuccess(squad -> {
                    if (squad != null && !squad.isEmpty() && user != null) {
                        user.addSearchHistory("Team: " + teamName + " (" + country + ")");
                    }
                });
    }

    @Override
    public Mono<PlayerPerformance> getPlayerPerformance(String playerName, User user) {
        return scrapingService.getPlayerPerformance(playerName)
                .doOnSuccess(performance -> {
                    if (performance != null && user != null) {
                        user.addSearchHistory("Player: " + playerName);
                    }
                });
    }
}

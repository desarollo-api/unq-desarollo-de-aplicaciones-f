package unq.desapp.futbol.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.PlayerPerformance;

import java.util.List;

@Service
public class FootballDataServiceImpl implements FootballDataService {
    private final ScrapingService scrapingService;

    public FootballDataServiceImpl(ScrapingService scrapingService,
                                   @Value("${football.api.baseurl}") String baseUrl,
                                   @Value("${football.api.token}") String apiToken
                                   ) {
        this.scrapingService = scrapingService;
    }

    @Override
    public Mono<List<Player>> getTeamSquad(String teamName, String country) {
        return scrapingService.getTeamSquad(teamName, country);
    }

    @Override
    public Mono<PlayerPerformance> getPlayerRating(String playerName, String country) {
        return scrapingService.getPlayerPerformance(playerName, country);
    }
}

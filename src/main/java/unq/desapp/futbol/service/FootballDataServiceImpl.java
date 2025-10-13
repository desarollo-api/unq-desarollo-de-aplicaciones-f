package unq.desapp.futbol.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Match;
import unq.desapp.futbol.model.Player;

import java.util.List;

@Service
public class FootballDataServiceImpl implements FootballDataService {
    private static final Logger logger = LoggerFactory.getLogger(FootballDataServiceImpl.class);
    private final WebClient webClient;
    private final ScrapingService scrapingService;

    public FootballDataServiceImpl(ScrapingService scrapingService,
                                   @Value("${football.api.baseurl}") String baseUrl,
                                   @Value("${football.api.token}") String apiToken
                                   ) {
        this.scrapingService = scrapingService;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Auth-Token", apiToken)
                .build();
    }

    @Override
    public Mono<List<Player>> getTeamSquad(String teamName, String country) {
        return scrapingService.getTeamSquad(teamName, country);
    }

    @Override
    public Mono<List<Match>> getUpcomingMatches(String teamName, String country) {
        return scrapingService.getUpcomingMatches(teamName, country);
    }
}
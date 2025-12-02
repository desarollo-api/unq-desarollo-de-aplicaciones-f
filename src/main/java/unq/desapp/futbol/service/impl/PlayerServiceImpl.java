package unq.desapp.futbol.service.impl;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.PlayerPerformance;
import unq.desapp.futbol.model.SearchType;
import unq.desapp.futbol.model.User;
import unq.desapp.futbol.service.PlayerService;
import unq.desapp.futbol.service.ScrapingService;
import unq.desapp.futbol.service.UserService;

@Service
public class PlayerServiceImpl implements PlayerService {

    private final ScrapingService scrapingService;
    private final UserService userService;

    public PlayerServiceImpl(ScrapingService scrapingService, UserService userService) {
        this.scrapingService = scrapingService;
        this.userService = userService;
    }

    @Override
    public Mono<PlayerPerformance> getPlayerPerformance(String playerName, User user) {
        return scrapingService.findPlayerPerformance(playerName).doOnSuccess(performance -> {
            if (performance != null && user != null) {
                user.addSearchHistory(SearchType.PLAYER, playerName);
                userService.saveUser(user);
            }
        });
    }
}

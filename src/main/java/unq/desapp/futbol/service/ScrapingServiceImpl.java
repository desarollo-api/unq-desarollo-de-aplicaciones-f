package unq.desapp.futbol.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.PlayerPerformance;
import unq.desapp.futbol.model.SeasonPerformance;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ScrapingServiceImpl implements ScrapingService {

    private static final Logger logger = LoggerFactory.getLogger(ScrapingServiceImpl.class);
    private static final String WHOSCORED_BASE_URL = "https://www.whoscored.com";
    private static final String SEARCH_URL_TEMPLATE = WHOSCORED_BASE_URL + "/search/?t=%s";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
    private static final String HEADER_ACCEPT = "Accept";

    private final ObjectMapper objectMapper = new ObjectMapper();

    // TEAM SQUAD

    @Override
    public Mono<List<Player>> getTeamSquad(String teamName, String country) {
        return Mono.fromCallable(() -> fetchTeamSquadFromAPI(teamName, country))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    logger.error("Error fetching team squad for team: {} ({})", teamName, country, e);
                    return Mono.empty();
                });
    }

    private List<Player> fetchTeamSquadFromAPI(String teamName, String country) throws IOException {
        String teamPageUrl = searchTeam(teamName, country);
        int teamId = extractTeamId(teamPageUrl);

        String apiUrl = "https://www.whoscored.com/statisticsfeed/1/getplayerstatistics" +
                "?category=summary&subcategory=all&statsAccumulationType=0&isCurrent=true" +
                "&playerId=&teamIds=" + teamId +
                "&matchId=&stageId=&tournamentOptions=67&sortBy=Rating&includeZeroValues=true&incPens=";

        logger.info("Fetching squad from API: {}", apiUrl);

        String jsonResponse = Jsoup.connect(apiUrl)
                .ignoreContentType(true)
                .header(HEADER_ACCEPT, "application/json")
                .userAgent(USER_AGENT)
                .get()
                .body()
                .text();

        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode playersArray = root.path("playerTableStats");

        if (!playersArray.isArray() || playersArray.isEmpty()) {
            logger.warn("No players found for team: {}", teamName);
            return Collections.emptyList();
        }

        List<Player> players = new ArrayList<>();
        for (JsonNode p : playersArray) {
            Player player = new Player(
                    p.path("name").asText("-"),
                    p.path("age").isMissingNode() ? null : p.path("age").asInt(),
                    p.path("teamRegionName").asText("-"),
                    p.path("positionText").asText("-"),
                    p.path("rating").asDouble(0.0),
                    p.path("apps").asInt(0),
                    p.path("goal").asInt(0),
                    p.path("assistTotal").asInt(0),
                    p.path("redCard").asInt(0),
                    p.path("yellowCard").asInt(0));
            players.add(player);
        }

        logger.info("Successfully fetched {} players for '{}'", players.size(), teamName);
        return players;
    }

    private String searchTeam(String teamName, String country) throws IOException {
        String encodedName = URLEncoder.encode(teamName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        String searchUrl = String.format(SEARCH_URL_TEMPLATE, encodedName);

        logger.info("Searching team '{}' at URL: {}", teamName, searchUrl);

        Document searchResultPage = Jsoup.connect(searchUrl)
                .userAgent(USER_AGENT)
                .referrer("https://www.whoscored.com/")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header(HEADER_ACCEPT, "text/html")
                .get();

        Elements resultRows = searchResultPage.select(".search-result table tr");

        for (Element row : resultRows) {
            Elements cells = row.select("td");
            if (cells.size() == 2) {
                String rowCountry = cells.get(1).text();
                if (country.equalsIgnoreCase(rowCountry)) {
                    Element link = cells.get(0).selectFirst("a");
                    if (link != null) {
                        String foundUrl = WHOSCORED_BASE_URL + link.attr("href");
                        logger.info("Found team '{}' ({}) → {}", teamName, country, foundUrl);
                        return foundUrl;
                    }
                }
            }
        }

        throw new IOException("Team not found for name: '" + teamName + "' (" + country + ")");
    }

    private int extractTeamId(String teamUrl) {
        Matcher m = Pattern.compile("/teams/(\\d+)/").matcher(teamUrl);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        } else {
            throw new IllegalArgumentException("Cannot extract teamId from URL: " + teamUrl);
        }
    }

    // PLAYER PERFORMANCE

    @Override
    public Mono<PlayerPerformance> getPlayerPerformance(String playerName) {
        return Mono.fromCallable(() -> fetchPlayerPerformanceFromAPI(playerName))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    logger.error("Error fetching player performance for: {}", playerName, e);
                    return Mono.empty();
                });
    }

    private PlayerPerformance fetchPlayerPerformanceFromAPI(String playerName) throws IOException {
        String playerPageUrl = searchPlayer(playerName);
        int playerId = extractPlayerId(playerPageUrl);

        String apiUrl = "https://www.whoscored.com/statisticsfeed/1/getplayerstatistics" +
                "?category=summary&subcategory=all&statsAccumulationType=0&isCurrent=false" +
                "&playerId=" + playerId +
                "&includeZeroValues=true&incPens=";

        logger.info("Fetching player stats from API: {}", apiUrl);

        String jsonResponse = Jsoup.connect(apiUrl)
                .ignoreContentType(true)
                .header(HEADER_ACCEPT, "application/json")
                .userAgent(USER_AGENT)
                .get()
                .body()
                .text();

        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode statsArray = root.path("playerTableStats");

        if (!statsArray.isArray() || statsArray.isEmpty()) {
            logger.warn("No stats found for player: {}", playerName);
            return new PlayerPerformance(playerName, Collections.emptyList());
        }

        List<SeasonPerformance> performances = new ArrayList<>();
        for (JsonNode stat : statsArray) {
            SeasonPerformance seasonPerf = new SeasonPerformance(
                    stat.path("seasonName").asText("-"),
                    stat.path("teamName").asText("-"),
                    stat.path("tournamentName").asText("-"),
                    stat.path("apps").asInt(0),
                    stat.path("goal").asInt(0),
                    stat.path("assistTotal").asInt(0),
                    stat.path("rating").asDouble(0.0));
            performances.add(seasonPerf);
        }

        logger.info("Successfully fetched {} seasons for player '{}'", performances.size(), playerName);
        return new PlayerPerformance(playerName, performances);
    }

    private String searchPlayer(String playerName) throws IOException {
        String encodedName = URLEncoder.encode(playerName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        String searchUrl = String.format(SEARCH_URL_TEMPLATE, encodedName);

        logger.info("Searching player '{}' at URL: {}", playerName, searchUrl);

        Document searchResultPage = Jsoup.connect(searchUrl)
                .userAgent(USER_AGENT)
                .referrer("https://www.whoscored.com/")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header(HEADER_ACCEPT, "text/html")
                .get();

        Element playerLink = searchResultPage.selectFirst(".search-result a[href^='/Players/']");

        if (playerLink != null) {
            String foundUrl = WHOSCORED_BASE_URL + playerLink.attr("href");
            logger.info("Found player '{}' → {}", playerName, foundUrl);
            return foundUrl;
        }

        throw new IOException("Player not found for name: '" + playerName + "'");
    }

    private int extractPlayerId(String playerUrl) {
        Matcher m = Pattern.compile("/players/(\\d+)/").matcher(playerUrl);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        } else {
            throw new IllegalArgumentException("Cannot extract playerId from URL: " + playerUrl);
        }
    }
}
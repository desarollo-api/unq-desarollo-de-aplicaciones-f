package unq.desapp.futbol.service;

import com.fasterxml.jackson.core.type.TypeReference;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    // =====================================================================================
    // TEAM SCRAPING (igual que antes)
    // =====================================================================================

    @Override
    public Mono<List<Player>> getTeamSquad(String teamName, String country) {
        return Mono.fromCallable(() -> findAndScrapeSquad(teamName, country))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    logger.error("Error scraping team squad for team: {} in country: {}", teamName, country, e);
                    return Mono.empty();
                });
    }

    private String searchTeam(String teamName, String country) throws IOException {
        String searchUrl = String.format(SEARCH_URL_TEMPLATE, URLEncoder.encode(teamName, StandardCharsets.UTF_8.name()));
        Document searchResultPage = Jsoup.connect(searchUrl).userAgent(USER_AGENT).get();
        Elements resultRows = searchResultPage.select(".search-result table tr");

        for (Element row : resultRows) {
            Elements cells = row.select("td");
            if (cells.size() == 2) {
                String rowCountry = cells.get(1).text();
                if (country.equalsIgnoreCase(rowCountry)) {
                    Element link = cells.get(0).selectFirst("a");
                    if (link != null) {
                        String foundUrl = WHOSCORED_BASE_URL + link.attr("href");
                        logger.info("Found matching team for '{}' in '{}' at URL: {}", teamName, country, foundUrl);
                        return foundUrl;
                    }
                }
            }
        }
        throw new IOException("Team not found for name: '" + teamName + "' and country: '" + country + "'");
    }

    private List<Player> findAndScrapeSquad(String teamName, String country) throws IOException {
        String teamPageUrl = searchTeam(teamName, country);
        return scrapeSquadFromPage(teamPageUrl);
    }

    private List<Player> scrapeSquadFromPage(String teamPageUrl) throws IOException {
        logger.info("Scraping squad from URL: {}", teamPageUrl);
        Document teamPage = Jsoup.connect(teamPageUrl).userAgent(USER_AGENT).get();

        Elements scripts = teamPage.getElementsByTag("script");
        String scriptContent = scripts.stream()
                .map(Element::data)
                .filter(s -> s.contains("require.config.params['args']"))
                .findFirst()
                .orElse(null);

        if (scriptContent == null) {
            logger.warn("Could not find player data script on page: {}", teamPageUrl);
            return Collections.emptyList();
        }

        Pattern pattern = Pattern.compile("require\\.config\\.params\\['args']\\s+=\\s+(\\{.*?\\});", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(scriptContent);

        if (!matcher.find()) {
            logger.warn("Could not match player data regex on page: {}", teamPageUrl);
            return Collections.emptyList();
        }

        String playersJsObject = matcher.group(1)
                .replaceAll("([\\{,]\\s*)(\\w+)(\\s*:)", "$1\"$2\"$3")
                .replaceAll("'", "\"");

        List<List<Object>> rawPlayers = extractPlayersFromJson(playersJsObject);
        List<Player> squad = new ArrayList<>();

        for (List<Object> rawPlayer : rawPlayers) {
            Player player = parsePlayerFromRawData(rawPlayer);
            if (player != null) squad.add(player);
        }

        logger.info("Successfully scraped {} players.", squad.size());
        return squad;
    }

    private List<List<Object>> extractPlayersFromJson(String json) throws IOException {
        TypeReference<List<List<Object>>> listType = new TypeReference<>() {};
        var rootNode = objectMapper.readTree(json);
        var formationsNode = rootNode.path("bestElevenFormation");

        List<List<Object>> allPlayers = new ArrayList<>();
        if (formationsNode.isArray()) {
            for (JsonNode formationNode : formationsNode) {
                var playersNode = formationNode.path(6);
                if (playersNode.isArray()) {
                    List<List<Object>> players = objectMapper.convertValue(playersNode, listType);
                    allPlayers.addAll(players);
                }
            }
        }
        return allPlayers;
    }

    private Player parsePlayerFromRawData(List<Object> rawData) {
        try {
            if (rawData.size() < 7) return null;
            String name = rawData.get(1).toString();
            double rating = ((Number) rawData.get(3)).doubleValue();
            String position = rawData.get(5).toString();
            int matches = ((Number) rawData.get(6)).intValue();

            return new Player(name, null, null, position, rating, matches, 0, 0, 0, 0);
        } catch (Exception e) {
            logger.warn("Skipping malformed player data row: {}", rawData, e);
            return null;
        }
    }

    // =====================================================================================
    // PLAYER PERFORMANCE (reemplazado por el nuevo endpoint JSON)
    // =====================================================================================

    @Override
    public Mono<PlayerPerformance> getPlayerPerformance(String playerName) {
        return Mono.fromCallable(() -> fetchPlayerPerformanceFromAPI(playerName))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    logger.error("Error scraping player performance for: {}", playerName, e);
                    return Mono.empty();
                });
    }

    private PlayerPerformance fetchPlayerPerformanceFromAPI(String playerName) throws IOException {
        String playerPageUrl = searchPlayer(playerName);
        int playerId = extractPlayerId(playerPageUrl);

        String apiUrl = "https://www.whoscored.com/statisticsfeed/1/getplayerstatistics" +
                "?category=summary&subcategory=all&statsAccumulationType=0&isCurrent=false" +
                "&playerId=" + playerId + "&includeZeroValues=true&incPens=";

        logger.info("Fetching player stats from API: {}", apiUrl);

        String jsonResponse = Jsoup.connect(apiUrl)
                .ignoreContentType(true)
                .header("Accept", "application/json")
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
                    stat.path("aerialWonPerGame").asDouble(0.0),
                    stat.path("rating").asDouble(0.0)
            );
            performances.add(seasonPerf);
        }

        return new PlayerPerformance(playerName, performances);
    }

    private String searchPlayer(String playerName) throws IOException {
        String searchUrl = String.format(SEARCH_URL_TEMPLATE, URLEncoder.encode(playerName, StandardCharsets.UTF_8.name()));
        Document searchResultPage = Jsoup.connect(searchUrl).userAgent(USER_AGENT).get();
        Element playerLink = searchResultPage.selectFirst(".search-result a[href^='/Players/']");

        if (playerLink != null) {
            String foundUrl = WHOSCORED_BASE_URL + playerLink.attr("href");
            logger.info("Found potential player match for '{}' at URL: {}", playerName, foundUrl);
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
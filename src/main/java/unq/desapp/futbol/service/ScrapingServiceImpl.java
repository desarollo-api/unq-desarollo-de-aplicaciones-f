package unq.desapp.futbol.service;

import com.fasterxml.jackson.core.type.TypeReference;
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
import unq.desapp.futbol.model.PlayerPerformance.Performance;

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


    @Override
    public Mono<List<Player>> getTeamSquad(String teamName, String country) {
        return Mono.fromCallable(() -> findAndScrapeSquad(teamName, country))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    logger.error("Error scraping team squad for team: {} in country: {}", teamName, country, e);
                    return Mono.empty();
                });
    }

    private List<Player> findAndScrapeSquad(String teamName, String country) throws IOException {
        logger.info("Finding team page for team: '{}' in country '{}'", teamName, country);
        String searchUrl = String.format(SEARCH_URL_TEMPLATE, URLEncoder.encode(teamName, StandardCharsets.UTF_8.name()));
        logger.info("Search URL: {}", searchUrl);
        Document searchPage = Jsoup.connect(searchUrl).userAgent(USER_AGENT).get();
        Elements teamRows = searchPage.select(".search-result table tr");

        for (Element row : teamRows) {
            Elements cells = row.select("td");
            if (cells.size() == 2) { // Asegurarse de que es una fila de datos de equipo
                String rowCountry = cells.get(1).text();
                if (country.equalsIgnoreCase(rowCountry)) {
                    Element link = cells.get(0).selectFirst("a");
                    if (link != null) {
                        String teamProfileUrl = WHOSCORED_BASE_URL + link.attr("href");
                        logger.info("Found team page URL: {}", teamProfileUrl);
                        return scrapeSquadFromPage(teamProfileUrl);
                    }
                }
            }
        }

        logger.warn("Could not find team '{}' from '{}' on whoscored.com", teamName, country);
        return null;
    }

    private List<Player> scrapeSquadFromPage(String teamPageUrl) throws IOException {
        logger.info("Scraping squad from URL: {}", teamPageUrl);
        Document teamPage = Jsoup.connect(teamPageUrl).userAgent(USER_AGENT).get();

        // Los datos de los jugadores están en un script, no en la tabla HTML directamente.
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

        Pattern pattern = Pattern.compile("require\\.config\\.params\\['args']\\s+=\\s+(\\{.*\\});", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(scriptContent);

        if (!matcher.find()) {
            logger.warn("Could not match player data regex on page: {}", teamPageUrl);
            return Collections.emptyList();
        }

        String playersJsObject = matcher.group(1);
        String playersJson = playersJsObject.replaceAll("([\\{,]\\s*)(\\w+)(\\s*:)", "$1\"$2\"$3")
                                            .replaceAll("'", "\"")
                                            .replaceAll(",\\s*,", ",\"\",")
                                            .replaceAll(",\\s*]", "]");
        List<List<Object>> rawPlayers = extractPlayersFromJson(playersJson);

        List<Player> squad = new ArrayList<>();
        for (List<Object> rawPlayer : rawPlayers) {
            Player player = parsePlayerFromRawData(rawPlayer);
            if (player != null) {
                squad.add(player);
            }
        }
        logger.info("Successfully scraped {} players.", squad.size());
        return squad;
    }

    private List<List<Object>> extractPlayersFromJson(String json) throws IOException {
        TypeReference<List<List<Object>>> formationListType = new TypeReference<List<List<Object>>>() {};
        List<List<Object>> allPlayers = new ArrayList<>();

        // El JSON contiene una estructura compleja, navegamos hasta la lista de jugadores
        var rootNode = objectMapper.readTree(json);
        var formationsNode = rootNode.path("bestElevenFormation");

        if (formationsNode.isArray()) {
            for (final var formationNode : formationsNode) {
                var playersNode = formationNode.path(6); // El 7mo elemento es la lista de jugadores
                if (playersNode.isArray()) {
                    List<List<Object>> playersInFormation = objectMapper.convertValue(playersNode, formationListType);
                    allPlayers.addAll(playersInFormation);
                }
            }
        }
        return allPlayers;
    }

    private Player parsePlayerFromRawData(List<Object> rawData) {
        try {
            if (rawData.size() < 7) {
                logger.warn("Skipping malformed player data row (not enough elements): {}", rawData);
                return null;
            }
            String name = rawData.get(1).toString();
            Double rating = ((Number) rawData.get(3)).doubleValue();
            String position = rawData.get(5).toString();
            Integer matches = ((Number) rawData.get(6)).intValue();

            // Los otros datos no están en esta estructura, los dejamos como null o 0.
            return new Player(name, null, null, position, rating, matches, 0, 0, 0, 0);
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            logger.warn("Skipping malformed player data row due to unexpected format: {}", rawData, e);
            return null;
        }
    }

    @Override
    public Mono<PlayerPerformance> getPlayerPerformance(String playerName, String country) {
        return Mono.fromCallable(() -> findAndScrapePlayerPerformance(playerName))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    logger.error("Error scraping player performance for: {}", playerName, e);
                    return Mono.empty();
                });
    }

    private PlayerPerformance findAndScrapePlayerPerformance(String playerName) throws IOException {
        logger.info("Finding player page for: '{}'", playerName);
        String searchUrl = String.format(SEARCH_URL_TEMPLATE, URLEncoder.encode(playerName, StandardCharsets.UTF_8.name()));
        Document searchPage = Jsoup.connect(searchUrl).userAgent(USER_AGENT).get();

        Element playerLink = searchPage.selectFirst(".search-result table a[href^='/Players/']");

        if (playerLink == null) {
            logger.warn("Could not find player '{}' on whoscored.com", playerName);
            return null;
        }

        String playerHistoryUrl = WHOSCORED_BASE_URL + playerLink.attr("href").replace("Show", "History");
        logger.info("Found player history page URL: {}", playerHistoryUrl);

        return scrapePerformanceFromPage(playerHistoryUrl);
    }

    private PlayerPerformance scrapePerformanceFromPage(String playerHistoryUrl) throws IOException {
        Document playerPage = Jsoup.connect(playerHistoryUrl).userAgent(USER_AGENT).get();

        String playerName = playerPage.selectFirst("h1.header-name").text();

        String scriptContent = playerPage.getElementsByTag("script").stream()
                .map(Element::data)
                .filter(s -> s.contains("player-tournament-stats"))
                .findFirst()
                .orElse(null);

        if (scriptContent == null) {
            logger.warn("Could not find player statistics script on page: {}", playerHistoryUrl);
            return new PlayerPerformance(playerName, Collections.emptyList());
        }

        Pattern pattern = Pattern.compile("'stats':(\\[\\[.*?\\]\\])");
        Matcher matcher = pattern.matcher(scriptContent);

        if (!matcher.find()) {
            logger.warn("Could not match player statistics regex on page: {}", playerHistoryUrl);
            return new PlayerPerformance(playerName, Collections.emptyList());
        }

        String statsJsonArray = matcher.group(1).replaceAll("'", "\"");

        List<List<Object>> rawPerformances = objectMapper.readValue(statsJsonArray, new TypeReference<List<List<Object>>>() {});

        List<Performance> performances = new ArrayList<>();
        for (List<Object> rawPerf : rawPerformances) {
            performances.add(parsePerformanceFromRawData(rawPerf));
        }

        return new PlayerPerformance(playerName, performances);
    }

    private Performance parsePerformanceFromRawData(List<Object> rawData) {
        Performance perf = new Performance();
        try {
            perf.setSeason(rawData.get(1).toString());
            perf.setTeam(rawData.get(6).toString());
            perf.setCompetition(rawData.get(3).toString());

            String appsStr = rawData.get(7).toString();
            Matcher m = Pattern.compile("\\d+").matcher(appsStr);
            if (m.find()) {
                perf.setAppearances(Integer.parseInt(m.group()));
            }

            perf.setGoals(toInt(rawData.get(9)));
            perf.setAssists(toInt(rawData.get(10)));

        } catch (Exception e) {
            logger.warn("Skipping malformed performance data row: {}", rawData, e);
            return null; 
        }
        return perf;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        return Integer.parseInt(obj.toString());
    }
}
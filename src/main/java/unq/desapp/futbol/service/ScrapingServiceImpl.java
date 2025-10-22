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

    @Override
    public Mono<PlayerPerformance> getPlayerPerformance(String playerName) {
        return Mono.fromCallable(() -> findAndScrapePlayerPerformance(playerName))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    logger.error("Error scraping player performance for: {}", playerName, e);
                    return Mono.empty();
                });
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

    private List<Player> findAndScrapeSquad(String teamName, String country) throws IOException {
        String teamPageUrl = searchTeam(teamName, country);
        return scrapeSquadFromPage(teamPageUrl);
    }

    private PlayerPerformance findAndScrapePlayerPerformance(String playerName) throws IOException {
        String playerPageUrl = searchPlayer(playerName);
        String playerHistoryUrl = playerPageUrl.replace("show", "history");
        logger.info("Transformed to player history URL: {}", playerHistoryUrl);
        return scrapePerformanceFromPage(playerHistoryUrl, playerName);
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


    private PlayerPerformance scrapePerformanceFromPage(String playerHistoryUrl, String playerName) throws IOException {
        logger.info("Scraping player history from URL: {}", playerHistoryUrl);
        Document playerPage = Jsoup.connect(playerHistoryUrl).userAgent(USER_AGENT).get();

        // Los datos están en la tabla con id 'player-table-statistics-body'
        Element statsTableBody = playerPage.selectFirst("#player-table-statistics-body");

        if (statsTableBody == null) {
            logger.warn("Could not find player statistics table on page: {}", playerHistoryUrl);
            return new PlayerPerformance(playerName, Collections.emptyList());
        }

        Elements statRows = statsTableBody.select("tr");
        List<SeasonPerformance> performances = new ArrayList<>();

        for (Element row : statRows) {
            // La última fila es el total, la ignoramos.
            if (row.text().contains("Total / Average")) {
                continue;
            }
            SeasonPerformance performance = parsePerformanceFromTableRow(row);
            if (performance != null) {
                performances.add(performance);
            }
        }

        return new PlayerPerformance(playerName, performances);
    }

    private SeasonPerformance parsePerformanceFromTableRow(Element row) {
        try {
            Elements cells = row.select("td");
            if (cells.size() < 14) return null; // Fila inválida

            String season = cells.get(0).text();
            String team = cells.get(1).text();
            String competition = cells.get(4).text();
            String appearancesText = cells.get(5).text().replaceAll("[()]", ""); // Limpia "26(2)" a "28"
            int appearances = Pattern.compile("\\d+").matcher(appearancesText).results().mapToInt(r -> Integer.parseInt(r.group())).sum();
            int goals = Integer.parseInt(cells.get(7).text().replace("-", "0"));
            int assists = Integer.parseInt(cells.get(8).text().replace("-", "0"));
            double rating = Double.parseDouble(cells.get(14).text().replace("-", "0.0"));

            return new SeasonPerformance(season, team, competition, appearances, goals, assists, 0.0, rating);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            logger.warn("Skipping malformed performance row due to parsing error: {}", row.text(), e);
            return null;
        }
    }
}
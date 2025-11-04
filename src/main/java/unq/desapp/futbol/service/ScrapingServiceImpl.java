package unq.desapp.futbol.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import unq.desapp.futbol.model.HeadToHeadMatch;
import unq.desapp.futbol.model.Match;
import unq.desapp.futbol.model.MatchPrediction;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.PlayerPerformance;
import unq.desapp.futbol.model.SeasonPerformance;
import unq.desapp.futbol.model.TeamStats;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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

    // UPCOMING MATCHES

    @Override
    public Mono<List<Match>> getUpcomingMatches(String teamName, String country) {
        return Mono.fromCallable(() -> scrapeUpcomingMatches(teamName, country))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    logger.error("Error scraping upcoming matches for team: {} in country: {}", teamName, country, e);
                    return Mono.empty();
                });
    }

    private List<Match> scrapeUpcomingMatches(String teamName, String country) throws IOException {
        String fixturesUrl = buildFixturesUrl(teamName, country);
        String fixturesData = buildFixturesData(fixturesUrl);

        if (fixturesData == null) {
            logger.warn("Could not find matches data script on page: {}", fixturesUrl);
            return Collections.emptyList();
        }

        Matcher dataMatcher = Pattern.compile("require\\.config\\.params\\['args']\\s+=\\s+(\\{.*\\});", Pattern.DOTALL)
                .matcher(fixturesData);
        boolean hasMatchesData = dataMatcher.find();

        if (!hasMatchesData) {
            logger.warn("Could not find matches data on page: {}", fixturesUrl);
            return Collections.emptyList();
        }

        List<List<Object>> fixtureMatches = buildFixtureMatches(dataMatcher);
        List<Match> upcomingMatches = new ArrayList<>();

        for (List<Object> fixtureMatch : fixtureMatches) {
            Match upcomingMatch = buildUpcomingMatch(fixtureMatch);

            if (upcomingMatch != null) {
                upcomingMatches.add(upcomingMatch);
            }
        }

        return upcomingMatches;
    }

    private String buildFixturesUrl(String teamName, String country) throws IOException {
        return searchTeam(teamName, country).replace("show", "fixtures");
    }

    private String buildFixturesData(String fixturesUrl) throws IOException {
        Document fixturesPage = Jsoup.connect(fixturesUrl).userAgent(USER_AGENT).get();
        Elements fixturesPageScripts = fixturesPage.getElementsByTag("script");

        return fixturesPageScripts.stream()
                .map(Element::data)
                .filter(s -> s.contains("require.config.params['args']"))
                .findFirst()
                .orElse(null);
    }

    private List<List<Object>> buildFixtureMatches(Matcher dataMatcher) throws IOException {
        String dataJson = dataMatcher.group(1)
                .replaceAll("([\\{,]\\s*)(\\w+)(\\s*:)", "$1\"$2\"$3")
                .replaceAll("'", "\"")
                .replaceAll(",\\s*,", ",\"\",")
                .replaceAll(",\\s*]", "]");

        JsonNode matchesNode = objectMapper.readTree(dataJson)
                .path("fixtureMatches");

        return matchesNode.isArray()
                ? objectMapper.convertValue(matchesNode, new TypeReference<List<List<Object>>>() {
                })
                : Collections.emptyList();
    }

    private Match buildUpcomingMatch(List<Object> fixtureMatch) {
        boolean hasEnoughMatchData = fixtureMatch.size() >= 17;
        if (!hasEnoughMatchData) {
            logger.warn("Skipping malformed match data node (not enough elements)");
            return null;
        }

        boolean isUpcomingMatch = "vs".equals(fixtureMatch.get(10).toString());
        if (!isUpcomingMatch) {
            return null;
        }

        String date = fixtureMatch.get(2).toString();
        String competition = fixtureMatch.get(16).toString();
        String homeTeam = fixtureMatch.get(5).toString();
        String awayTeam = fixtureMatch.get(8).toString();

        return new Match(date, competition, homeTeam, awayTeam);
    }

    // UPCOMING MATCHPREDICTION

    @Override
    public Mono<MatchPrediction> predictNextMatch(String teamName, String country) {
        return Mono.fromCallable(() -> buildMatchPrediction(teamName, country))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    logger.error("Error generating match prediction for team: {} in country: {}", teamName, country, e);
                    return Mono.empty();
                });
    }

    private MatchPrediction buildMatchPrediction(String teamName, String country) throws IOException {
        // 1️⃣ Buscar el próximo partido
        List<Match> upcoming = scrapeUpcomingMatches(teamName, country);
        if (upcoming.isEmpty()) {
            logger.warn("No upcoming matches found for team '{}'", teamName);
            return null;
        }

        Match nextMatch = upcoming.get(0);
        logger.info("Found next match: {} vs {}", nextMatch.getHomeTeam(), nextMatch.getAwayTeam());

        // 2️⃣ Buscar el ID del partido desde fixtures (usar searchTeam a partir de
        // fixturesPage como antes)
        String fixturesUrl = searchTeam(teamName, country).replace("show", "fixtures");
        logger.info("Fetching fixtures from {}", fixturesUrl);

        Document fixturesPage = Jsoup.connect(fixturesUrl)
                .userAgent(USER_AGENT)
                .timeout(15000)
                .get();

        Elements scripts = fixturesPage.getElementsByTag("script");
        String fixturesScript = scripts.stream()
                .map(Element::data)
                .filter(s -> s != null && s.contains("require.config.params['args']"))
                .findFirst()
                .orElse(null);

        if (fixturesScript == null) {
            logger.warn("Could not find matches data script on page: {}", fixturesUrl);
            return null;
        }

        Matcher matcher = Pattern.compile("require\\.config\\.params\\['args']\\s*=\\s*(\\{.*?\\});", Pattern.DOTALL)
                .matcher(fixturesScript);

        if (!matcher.find()) {
            logger.warn("Could not extract fixtures JSON from script");
            return null;
        }

        String jsonData = matcher.group(1)
                .replaceAll("([\\{,]\\s*)(\\w+)(\\s*:)", "$1\"$2\"$3")
                .replaceAll("'", "\"")
                .replaceAll(",\\s*,", ",\"\",")
                .replaceAll(",\\s*]", "]");

        JsonNode root = objectMapper.readTree(jsonData);
        JsonNode fixtureMatches = root.path("fixtureMatches");

        if (!fixtureMatches.isArray() || fixtureMatches.isEmpty()) {
            logger.warn("No fixtureMatches array found for {}", teamName);
            return null;
        }

        // 3️⃣ Obtener matchId
        String matchId = null;
        for (JsonNode match : fixtureMatches) {
            if (match.size() > 10 && "vs".equalsIgnoreCase(match.get(10).asText())) {
                matchId = match.get(0).asText();
                break;
            }
        }

        if (matchId == null) {
            logger.warn("No matchId found for next match between {} and {}", nextMatch.getHomeTeam(),
                    nextMatch.getAwayTeam());
            return null;
        }

        String matchUrl = "https://www.whoscored.com/matches/" + matchId + "/show";
        logger.info("✅ Match page to scrape: {}", matchUrl);

        // 4️⃣ Cargar la página del partido
        Document matchPage = Jsoup.connect(matchUrl)
                .userAgent(USER_AGENT)
                .timeout(15000)
                .get();

        // 5️⃣ Buscar el script con require.config.params["args"]
        Elements matchScripts = matchPage.getElementsByTag("script");
        String matchScript = matchScripts.stream()
                .map(Element::data)
                .filter(s -> s != null && (s.contains("require.config.params[\"args\"]")
                        || s.contains("require.config.params['args']")))
                .findFirst()
                .orElse(null);

        if (matchScript == null) {
            logger.warn("Could not find require.config.params['args'] script in match page");
            return null;
        }

        // Buscar índice de inicio del objeto específico (después del token)
        String needle1 = "require.config.params[\"args\"]";
        String needle2 = "require.config.params['args']";
        int idx = matchScript.indexOf(needle1);
        if (idx < 0)
            idx = matchScript.indexOf(needle2);
        if (idx < 0) {
            // fallback: buscar primer '{' en el script
            idx = matchScript.indexOf('{');
        } else {
            idx = matchScript.indexOf('{', idx);
        }

        if (idx < 0) {
            logger.warn("Could not find opening brace in args script");
            return null;
        }

        // Extraer bloque JSON balanceando llaves
        String matchJson = balanceBracesSubstr(matchScript, idx);
        if (matchJson == null) {
            logger.warn("Could not balance braces to extract JSON from args script");
            logger.debug("Script snippet around token:\n{}",
                    matchScript.substring(Math.max(0, idx - 200), Math.min(matchScript.length(), idx + 1200)));
            return null;
        }

        logger.info("📦 Raw JS object length: {}", matchJson.length());

        // -------------------------
        // LIMPIEZA ROBUSTA (JS -> JSON)
        // -------------------------
        // 1) Quitar bloques HTML comentados, etiquetas y scripts incrustados
        String cleaned = matchJson
                // Eliminar comentarios HTML o bloques <!-- ... -->
                .replaceAll("<!--.*?-->", " ")
                // Eliminar etiquetas HTML que aparecen dentro de cadenas o arrays
                .replaceAll("<[^>]+>", " ")
                // Normalizar saltos y espacios múltiples
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s{2,}", " ")

                // Asegurar comillas dobles para claves sin comillas
                .replaceAll("([\\{,]\\s*)([a-zA-Z0-9_]+)\\s*:", "$1\"$2\":")
                // Sustituir True/False/undefined
                .replace("undefined", "null")
                .replace("True", "true")
                .replace("False", "false")

                // Eliminar comas sobrantes antes de cierre de objetos o arrays
                .replaceAll(",\\s*([}\\]])", "$1")

                // Quitar comentarios JS
                .replaceAll("//.*?\\n", "")
                .replaceAll("/\\*.*?\\*/", "")

                // 🔹 Normalizar comillas simples a dobles solo si no están en fechas ni
                // escapadas
                .replaceAll("(?<![A-Za-z0-9])'(?![A-Za-z0-9])", "\"")
                .replaceAll("(?<=[:,\\[{]\\s*)'(.*?)'(\\s*[,}\\]])", "\"$1\"$2")

                // Eliminar comillas sueltas huérfanas (que causan errores)
                .replaceAll("(^|[\\s,:\\[{])'(\\s|[,}\\]])", "$1$2")

                // Quitar espacios redundantes
                .trim();

        // Limitar al objeto principal { ... }
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }

        // Reparar fragmentos tipo "} facts" → "},\"facts\""
        cleaned = cleaned.replaceAll("}(\\s*[a-zA-Z_]+)\\s*:", "},\"$1\":");

        // Guardar JSON limpio para depuración
        Path debugFixed = Paths.get("failed_match_json_fixed.txt");
        Files.writeString(debugFixed, cleaned);
        logger.info("✅ Cleaned JSON length: {}", cleaned.length());
        logger.debug("Cleaned preview:\n{}", cleaned.substring(0, Math.min(1500, cleaned.length())));

        // 7️⃣ Intentar parsear con ObjectMapper
        JsonNode matchData;
        try {
            matchData = objectMapper.readTree(cleaned);
        } catch (Exception e) {
            logger.error("❌ Could not parse fixed match JSON, saved to {}", debugFixed.toAbsolutePath(), e);
            throw e;
        }

        // -------------------------
        // Extraer matchCentreData (si está stringificado con JSON.parse)
        // -------------------------
        JsonNode matchCentreData = matchData.path("matchCentreData");
        if (!matchCentreData.isMissingNode() && matchCentreData.isTextual()) {
            String inner = matchCentreData.asText();
            try {
                matchCentreData = objectMapper.readTree(inner);
            } catch (Exception e) {
                logger.warn("Could not parse inner matchCentreData JSON (will try matchData fallback)");
                matchCentreData = objectMapper.createObjectNode(); // fallback vacío
            }
        } else if (matchCentreData.isMissingNode() && matchData.has("matchCentreData")) {
            matchCentreData = matchData.get("matchCentreData");
        }

        // 8️⃣ Buscar previousMeetings (primero en matchCentreData, luego en matchData)
        JsonNode previousMeetings = matchCentreData.path("previousMeetings");
        if (previousMeetings.isMissingNode() || !previousMeetings.isArray()) {
            previousMeetings = matchData.path("previousMeetings");
        }

        List<HeadToHeadMatch> history = new ArrayList<>();
        if (previousMeetings.isArray()) {
            for (JsonNode node : previousMeetings) {
                if (!node.isArray() || node.size() < 11)
                    continue;
                String date = node.get(2).asText("-");
                String competition = node.size() > 16 ? node.get(16).asText("-") : "-";
                String home = node.get(5).asText("-");
                String away = node.get(8).asText("-");
                String result = node.get(10).asText("-");
                history.add(new HeadToHeadMatch(date, competition, home, away, result));
            }
        }
        logger.info("✅ Found {} previous meetings", history.size());

        // 9️⃣ Extraer estadísticas completas usando helper (si están disponibles)
        TeamStats homeTeamStats = extractTeamStats(matchCentreData.path("home"), nextMatch.getHomeTeam());
        TeamStats awayTeamStats = extractTeamStats(matchCentreData.path("away"), nextMatch.getAwayTeam());

        // 10️⃣ Predicción simple basada en history (fallback a matchHeader si querés)
        String prediction = "Draw";
        if (!history.isEmpty()) {
            long homeWins = history.stream()
                    .filter(h -> h.getResult().startsWith("2")
                            && h.getHomeTeam().equalsIgnoreCase(nextMatch.getHomeTeam()))
                    .count();
            long awayWins = history.stream()
                    .filter(h -> h.getResult().startsWith("2")
                            && h.getAwayTeam().equalsIgnoreCase(nextMatch.getAwayTeam()))
                    .count();

            if (homeWins > awayWins)
                prediction = nextMatch.getHomeTeam() + " win";
            else if (awayWins > homeWins)
                prediction = nextMatch.getAwayTeam() + " win";
        }

        logger.info("🔮 Prediction: {}", prediction);

        // 11️⃣ Retornar predicción final
        return new MatchPrediction(
                nextMatch.getHomeTeam(),
                nextMatch.getAwayTeam(),
                homeTeamStats,
                awayTeamStats,
                history,
                prediction);
    }

    // ------------------ Helper: extrae TeamStats a partir de un JsonNode
    // ------------------
    private TeamStats extractTeamStats(JsonNode node, String defaultName) {
        if (node == null || node.isMissingNode() || node.isNull())
            return null;

        return new TeamStats(
                node.path("name").asText(defaultName),
                node.path("position").asInt(0),
                node.path("played").asInt(0),
                node.path("wins").asInt(0),
                node.path("draws").asInt(0),
                node.path("losses").asInt(0),
                node.path("goalsFor").asInt(0),
                node.path("goalsAgainst").asInt(0),
                node.path("points").asInt(0));
    }

    // ------------------ Balanceo de llaves (tu función, la mantuve)
    // ------------------
    private static String balanceBracesSubstr(String text, int startIdx) {
        int len = text.length();
        int depth = 0;
        boolean inString = false;
        char stringQuote = 0;
        for (int i = startIdx; i < len; i++) {
            char c = text.charAt(i);

            // manejar strings para no confundir llaves dentro de them
            if (!inString && (c == '"' || c == '\'')) {
                inString = true;
                stringQuote = c;
            } else if (inString && c == stringQuote) {
                // ignorar escaped quotes
                int backslashes = 0;
                int j = i - 1;
                while (j >= 0 && text.charAt(j) == '\\') {
                    backslashes++;
                    j--;
                }
                if (backslashes % 2 == 0) { // no escaped
                    inString = false;
                }
            }

            if (!inString) {
                if (c == '{')
                    depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return text.substring(startIdx, i + 1);
                    }
                }
            }
        }
        return null;
    }
}

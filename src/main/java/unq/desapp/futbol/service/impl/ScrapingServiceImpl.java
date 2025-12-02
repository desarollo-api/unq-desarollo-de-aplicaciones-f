package unq.desapp.futbol.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import unq.desapp.futbol.model.UpcomingMatch;
import unq.desapp.futbol.service.ScrapingService;
import unq.desapp.futbol.model.TeamStats;
import unq.desapp.futbol.model.MatchPrediction;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.PlayerPerformance;
import unq.desapp.futbol.model.PreviousMatch;
import unq.desapp.futbol.model.SeasonPerformance;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class ScrapingServiceImpl implements ScrapingService {

    private static final Logger logger = LoggerFactory.getLogger(ScrapingServiceImpl.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
    private static final String HEADER_ACCEPT = "Accept";
    private static final String VICTORY_LITERAL = "Victory";
    private static final String DEFEAT_LITERAL = "Defeat";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;

    public ScrapingServiceImpl(@Value("${whoscored.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    // TEAM SQUAD

    @Override
    public Mono<List<Player>> findTeamSquad(String teamName, String country) {
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

        String apiUrl = baseUrl + "/statisticsfeed/1/getplayerstatistics" +
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
        String searchUrl = baseUrl + "/search/?t=" + encodedName;

        logger.info("Searching team '{}' at URL: {}", teamName, searchUrl);

        Document searchResultPage = Jsoup.connect(searchUrl)
                .userAgent(USER_AGENT)
                .referrer(baseUrl + "/")
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
                        String foundUrl = baseUrl + link.attr("href");
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
    public Mono<PlayerPerformance> findPlayerPerformance(String playerName) {
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

        String apiUrl = baseUrl + "/statisticsfeed/1/getplayerstatistics" +
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
        String searchUrl = baseUrl + "/search/?t=" + encodedName;

        logger.info("Searching player '{}' at URL: {}", playerName, searchUrl);

        Document searchResultPage = Jsoup.connect(searchUrl)
                .userAgent(USER_AGENT)
                .referrer(baseUrl + "/")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header(HEADER_ACCEPT, "text/html")
                .get();

        Element playerLink = searchResultPage.selectFirst(".search-result a[href^='/Players/']");

        if (playerLink != null) {
            String foundUrl = baseUrl + playerLink.attr("href");
            logger.info("Found player '{}' → {}", playerName, foundUrl);
            return foundUrl;
        }

        throw new IOException("Player not found for name: '" + playerName + "'");
    }

    private int extractPlayerId(String playerUrl) {
        Matcher m = Pattern.compile("/[Pp]layers/(\\d+)/").matcher(playerUrl);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        } else {
            throw new IllegalArgumentException("Cannot extract playerId from URL: " + playerUrl);
        }
    }

    // UPCOMING MATCHES

    public Mono<List<UpcomingMatch>> findUpcomingMatches(String teamName, String country) {
        return Mono.fromCallable(() -> scrapeUpcomingMatches(teamName, country))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    logger.error("Error scraping upcoming matches for team: {} in country: {}", teamName, country, e);
                    return Mono.empty();
                });
    }

    private List<UpcomingMatch> scrapeUpcomingMatches(String teamName, String country) throws IOException {
        List<List<Object>> fixtureMatches = buildFixtureMatches(teamName, country);
        List<UpcomingMatch> upcomingMatches = new ArrayList<>();

        for (List<Object> fixtureMatch : fixtureMatches) {
            UpcomingMatch upcomingMatch = buildUpcomingMatch(fixtureMatch);

            if (upcomingMatch != null) {
                upcomingMatches.add(upcomingMatch);
            }
        }

        return upcomingMatches;
    }

    private List<List<Object>> buildFixtureMatches(String teamName, String country) throws IOException {
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

        return buildFixtureMatches(dataMatcher);
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
                .replace("'", "\"")
                .replaceAll("([\\{,]\\s*)(\\w+)(\\s*:)", "$1\"$2\"$3")
                .replaceAll(",\\s*,", ",\"\",")
                .replaceAll(",\\s*]", "]");

        JsonNode matchesNode = objectMapper.readTree(dataJson)
                .path("fixtureMatches");

        return matchesNode.isArray()
                ? objectMapper.convertValue(matchesNode, new TypeReference<List<List<Object>>>() {
                })
                : Collections.emptyList();
    }

    private UpcomingMatch buildUpcomingMatch(List<Object> fixtureMatch) {
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

        return new UpcomingMatch(date, competition, homeTeam, awayTeam);
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
        List<List<Object>> fixtureMatches = buildFixtureMatches(teamName, country);
        if (fixtureMatches.isEmpty())
            return null;

        List<List<Object>> upcomingMatches = buildUpcomingMatches(fixtureMatches);
        if (upcomingMatches.isEmpty())
            return null;

        List<Object> upcomingMatch = upcomingMatches.get(0);
        String matchId = upcomingMatch.get(0).toString();
        String homeTeam = upcomingMatch.get(5).toString();
        String awayTeam = upcomingMatch.get(8).toString();

        if (!StringUtils.hasText(matchId))
            return null;

        String matchUrl = baseUrl + "/matches/" + matchId + "/show";
        logger.info("🔍 Scraping match page: {}", matchUrl);

        JsonNode rootNode = extractMatchJson(matchUrl);
        if (rootNode == null)
            return null;

        List<PreviousMatch> previousMatches = buildPreviousMatches(
                objectMapper.convertValue(rootNode.path("previousMeetings"), new TypeReference<List<List<Object>>>() {
                }));
        List<PreviousMatch> homeMatches = buildPreviousMatchesV2(
                objectMapper.convertValue(rootNode.path("homeMatches").get(0), new TypeReference<List<List<Object>>>() {
                }));

        List<PreviousMatch> awayMatches = buildPreviousMatchesV2(
                objectMapper.convertValue(rootNode.path("awayMatches").get(0), new TypeReference<List<List<Object>>>() {
                }));

        boolean isHomeTeam = homeTeam.equalsIgnoreCase(teamName);
        String currentTeam = isHomeTeam ? homeTeam : awayTeam;

        int currentPoints = 0;
        int opponentPoints = 0;

        currentPoints += evaluatePrediction(previousMatches, currentTeam, true);
        opponentPoints += evaluatePrediction(previousMatches, currentTeam, false);

        currentPoints += evaluateTeamPrediction(homeMatches, homeTeam, isHomeTeam, true);
        opponentPoints += evaluateTeamPrediction(homeMatches, homeTeam, isHomeTeam, false);

        currentPoints += evaluateTeamPrediction(awayMatches, awayTeam, !isHomeTeam, true);
        opponentPoints += evaluateTeamPrediction(awayMatches, awayTeam, !isHomeTeam, false);

        String finalPrediction = computeFinalPrediction(currentPoints, opponentPoints, currentTeam);

        List<PreviousMatch> combinedMeetings = Stream.of(previousMatches, homeMatches, awayMatches)
                .flatMap(Collection::stream)
                .distinct()
                .limit(5)
                .toList();

        return new MatchPrediction(homeTeam, awayTeam, combinedMeetings, finalPrediction);
    }

    private JsonNode extractMatchJson(String matchUrl) throws IOException {
        Document matchPage = Jsoup.connect(matchUrl)
                .userAgent(USER_AGENT)
                .timeout(15000)
                .get();

        String script = matchPage.getElementsByTag("script").stream()
                .map(Element::data)
                .filter(s -> s.contains("require.config.params[\"args\"]"))
                .findFirst()
                .orElse(null);

        if (script == null) {
            logger.warn("No match data script found at {}", matchUrl);
            return null;
        }

        Matcher matcher = Pattern.compile("require\\.config\\.params\\[\"args\"]\\s+=\\s+(\\{.*\\})", Pattern.DOTALL)
                .matcher(script);

        if (!matcher.find())
            return null;

        String json = matcher.group(1)
                .replaceFirst("(?s)showLeagueTableStandings.*?homeMatches", "homeMatches")
                .replace("'", "\"")
                .replaceAll("([\\{,]\\s*)(\\w+)(\\s*:)", "$1\"$2\"$3")
                .replaceAll(",\\s*,", ",\"\",")
                .replaceAll(",\\s*]", "]")
                .replaceAll(",\\s*}", "}");

        return objectMapper.readTree(json);
    }

    private int evaluatePrediction(List<PreviousMatch> matches, String team, boolean forTeam) {
        String prediction = getPrediction(matches, team);
        return prediction.contains(getOutcomeLiteral(forTeam)) ? 1 : 0;
    }

    private CharSequence getOutcomeLiteral(boolean isVictory) {
        return isVictory ? VICTORY_LITERAL : DEFEAT_LITERAL;
    }

    private int evaluateTeamPrediction(List<PreviousMatch> matches, String team, boolean isTeam, boolean forTeam) {
        String prediction = getPrediction(matches, team);
        boolean condition = isTeam
                ? prediction.contains(getOutcomeLiteral(forTeam))
                : prediction.contains(getOutcomeLiteral(!forTeam));
        return condition ? 1 : 0;
    }

    private String computeFinalPrediction(int currentPoints, int opponentPoints, String team) {
        if (currentPoints == opponentPoints)
            return "Draw";
        return currentPoints > opponentPoints
                ? String.format(VICTORY_LITERAL + " for %s", team)
                : String.format(DEFEAT_LITERAL + " for %s", team);
    }

    private String getPrediction(List<PreviousMatch> matches, String teamName) {
        if (matches == null || matches.isEmpty())
            return "Cannot predict match outcome";

        int[] scores = calculateTeamScores(matches, teamName);
        return formatPredictionResult(scores[0], scores[1], teamName);
    }

    private int[] calculateTeamScores(List<PreviousMatch> matches, String teamName) {
        int teamWins = 0;
        int opponentWins = 0;

        for (PreviousMatch match : matches) {
            try {
                int homeScore = Integer.parseInt(match.getHomeScore());
                int awayScore = Integer.parseInt(match.getAwayScore());

                if (homeScore > awayScore) {
                    if (match.getHomeTeam().equalsIgnoreCase(teamName))
                        teamWins++;
                    else
                        opponentWins++;
                } else if (awayScore > homeScore) {
                    if (match.getAwayTeam().equalsIgnoreCase(teamName))
                        teamWins++;
                    else
                        opponentWins++;
                }
            } catch (NumberFormatException ignored) {
                // Ignore invalid score formats; continue with next match
            }
        }

        return new int[] { teamWins, opponentWins };
    }

    private String formatPredictionResult(int teamWins, int opponentWins, String teamName) {
        if (teamWins > opponentWins)
            return VICTORY_LITERAL + " for " + teamName;
        if (opponentWins > teamWins)
            return DEFEAT_LITERAL + " for " + teamName;
        return "Draw";
    }

    private List<PreviousMatch> buildPreviousMatches(List<List<Object>> previousMeetings) {
        return previousMeetings.stream()
                .map(m -> new PreviousMatch(
                        m.get(2).toString(),
                        m.get(16).toString(),
                        m.get(5).toString(),
                        m.get(33).toString(),
                        m.get(8).toString(),
                        m.get(34).toString()))
                .toList();
    }

    private List<PreviousMatch> buildPreviousMatchesV2(List<List<Object>> previousMeetings) {
        return previousMeetings.stream()
                .map(m -> new PreviousMatch(
                        m.get(2).toString(),
                        m.get(16).toString(),
                        m.get(5).toString(),
                        m.get(31).toString(),
                        m.get(8).toString(),
                        m.get(32).toString()))
                .toList();
    }

    private List<List<Object>> buildUpcomingMatches(List<List<Object>> fixtureMatches) {
        List<List<Object>> upcomingMatches = new ArrayList<>();

        for (List<Object> fixtureMatch : fixtureMatches) {
            if ("vs".equals(fixtureMatch.get(10).toString())) {
                upcomingMatches.add(fixtureMatch);
            }
        }

        return upcomingMatches;
    }

    // TEAM STATS

    @Override
    public Mono<TeamStats> findTeamStats(String teamName, String country) {
        return Mono.fromCallable(() -> fetchTeamStats(teamName, country))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    logger.error("Error fetching team stats for team: {} ({})", teamName, country, e);
                    return Mono.empty();
                });
    }

    private TeamStats fetchTeamStats(String teamName, String country) throws IOException {
        // 1. Reutilizar la búsqueda del equipo para obtener su URL y su ID
        TeamStats stats = new TeamStats(teamName, country);

        // 2. Obtener plantilla de jugadores y calcular estadísticas basadas en ellos
        List<Player> squad = findTeamSquad(teamName, country).block();
        if (squad != null && !squad.isEmpty()) {
            calculatePlayerBasedStats(stats, squad);
        }

        // 3. Obtener historial de partidos y calcular resultados
        List<List<Object>> fixtureMatches = buildFixtureMatches(teamName, country);
        if (!fixtureMatches.isEmpty()) {
            calculateMatchResults(stats, fixtureMatches, teamName);
        }

        logger.info("Successfully generated stats for team '{}'", teamName);
        return stats;
    }

    private void calculatePlayerBasedStats(TeamStats stats, List<Player> players) {
        if (players == null || players.isEmpty()) {
            return;
        }

        double averageAge = players.stream()
                .map(Player::getAge).filter(java.util.Objects::nonNull)
                .mapToInt(a -> a).average().orElse(0.0);
        stats.setAverageAge(Math.round(averageAge * 10.0) / 10.0);

        double averageRating = players.stream()
                .map(Player::getRating).filter(java.util.Objects::nonNull)
                .mapToDouble(r -> r).average().orElse(0.0);
        stats.setAverageRating(Math.round(averageRating * 10.0) / 10.0);

        players.stream()
                .filter(p -> p.getRating() != null)
                .max(Comparator.comparing(Player::getRating))
                .ifPresent(p -> stats.setBestPlayer(p.getName()));
    }

    private void calculateMatchResults(TeamStats stats, List<List<Object>> fixtureMatches, String teamName) {
        int wins = 0;
        int draws = 0;
        int defeats = 0;

        for (List<Object> match : fixtureMatches) {
            if (match.size() > 10 && !"vs".equals(match.get(10).toString())) { // Partido jugado
                String homeTeam = match.get(5).toString(); // "1 : 0"
                String result = match.get(10).toString().replace("*", "");
                String[] scores = result.split(" : ");
                int homeScore = Integer.parseInt(scores[0]);
                int awayScore = Integer.parseInt(scores[1]);

                boolean isHome = teamName.equalsIgnoreCase(homeTeam);
                if (homeScore == awayScore)
                    draws++;
                else if ((isHome && homeScore > awayScore) || (!isHome && awayScore > homeScore))
                    wins++;
                else
                    defeats++;
            }
        }
        stats.setWins(wins);
        stats.setDraws(draws);
        stats.setDefeats(defeats);

        int totalMatches = wins + draws + defeats;
        if (totalMatches > 0) {
            stats.setWinRate(Math.round(((double) wins / totalMatches) * 1000.0) / 10.0);
        }
    }
}

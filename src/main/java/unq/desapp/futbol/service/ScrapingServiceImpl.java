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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class ScrapingServiceImpl implements ScrapingService {

    private static final Logger logger = LoggerFactory.getLogger(ScrapingServiceImpl.class);
    private static final String WHOSCORED_BASE_URL = "https://www.whoscored.com";
    private static final String SEARCH_URL_TEMPLATE = WHOSCORED_BASE_URL + "/Search/?t=%s";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";

    // ObjectMapper es thread-safe y puede ser reutilizado
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Double> getPlayerRating(String playerName) {
        // El scraping es una operación de I/O bloqueante, por lo que debe ejecutarse en un scheduler dedicado.
        return Mono.fromCallable(() -> scrapeRating(playerName))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    logger.error("Error scraping rating for player: {}", playerName, e);
                    return Mono.empty(); // Devuelve un Mono vacío en caso de error
                });
    }

    private Double scrapeRating(String playerName) throws IOException {
        logger.info("Scraping rating for player: {}", playerName);
        String searchUrl = String.format(SEARCH_URL_TEMPLATE, URLEncoder.encode(playerName, StandardCharsets.UTF_8.name()));

        // 1. Busca al jugador para encontrar la URL de su perfil
        Document searchPage = Jsoup.connect(searchUrl).userAgent(USER_AGENT).get();
        // El link al perfil del jugador está en la primera fila de la tabla de resultados de búsqueda
        Element playerLink = searchPage.selectFirst(".search-result table a");

        if (playerLink == null) {
            logger.warn("Could not find player '{}' on whoscored.com", playerName);
            return null;
        }

        String playerProfileUrl = WHOSCORED_BASE_URL + playerLink.attr("href");
        logger.info("Found player profile URL: {}", playerProfileUrl);

        // 2. Va a la página del perfil del jugador
        Document playerPage = Jsoup.connect(playerProfileUrl).userAgent(USER_AGENT).get();

        // 3. La tabla de estadísticas se renderiza con JS. Extraemos los datos del JSON incrustado en un tag <script>.
        Elements scripts = playerPage.getElementsByTag("script");
        String scriptContent = null;
        for (Element script : scripts) {
            if (script.html().contains("require.config.params['args']")) {
                scriptContent = script.html();
                break;
            }
        }

        if (scriptContent == null) {
            logger.warn("Could not find player stats script for player {}", playerName);
            return null;
        }

        // 4. Extrae el objeto JSON del contenido del script.
        int startIndex = scriptContent.indexOf('{');
        int endIndex = scriptContent.lastIndexOf('}');
        if (startIndex == -1 || endIndex == -1) {
            logger.warn("Could not parse player stats JSON for player {}", playerName);
            return null;
        }
        String jsObjectString = scriptContent.substring(startIndex, endIndex + 1);

        // El string extraído es un objeto JavaScript, no un JSON válido (las claves no están entre comillas).
        // Usaremos una expresión regular para añadir comillas a las claves y convertirlo en un JSON válido.
        String jsonString = jsObjectString.replaceAll("([a-zA-Z0-9_]+)\\s*:", "\"$1\":");

        // 5. Parsea el JSON y calcula el rating promedio de los torneos.
        JsonNode rootNode = objectMapper.readTree(jsonString);
        JsonNode tournamentsNode = rootNode.path("tournaments");

        if (tournamentsNode.isMissingNode() || !tournamentsNode.isArray()) {
            logger.warn("Tournaments data not found in JSON for player {}", playerName);
            return null;
        }

        double totalRating = 0;
        int count = 0;
        for (JsonNode tournament : tournamentsNode) {
            double rating = tournament.path("Rating").asDouble();
            if (rating > 0) { // Ignoramos torneos sin rating (valor 0 o '-')
                totalRating += rating;
                count++;
            }
        }

        if (count > 0) {
            double averageRating = totalRating / count;
            // Redondea a dos decimales para que coincida con el sitio
            return Math.round(averageRating * 100.0) / 100.0;
        } else {
            logger.warn("No valid ratings found in tournaments data for player {}", playerName);
            return null;
        }
    }
}
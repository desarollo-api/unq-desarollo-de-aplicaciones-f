package unq.desapp.futbol.service;

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
    private static final String SEARCH_URL_TEMPLATE = WHOSCORED_BASE_URL + "/search/?t=%s";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public Mono<String> findTeamPage(String teamName, String country) {
        return Mono.fromCallable(() -> scrapeTeamUrl(teamName, country))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    logger.error("Error scraping team URL for team: {} in country: {}", teamName, country, e);
                    return Mono.empty();
                });
    }

    private String scrapeTeamUrl(String teamName, String country) throws IOException {
        logger.info("Scraping team URL for team: '{}' in country '{}'", teamName, country);
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
                        return teamProfileUrl;
                    }
                }
            }
        }

        logger.warn("Could not find team '{}' from '{}' on whoscored.com", teamName, country);
        return null;
    }
}
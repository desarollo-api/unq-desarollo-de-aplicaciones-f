package unq.desapp.futbol.security;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

final class Constants {
    final class Auth {
        static final String PATTERN = "/auth/**";
    }

    final class Cors {
        static final List<String> ALL_ALLOWED = List.of("*");
        static final List<String> ALLOWED_METHODS = List.of(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.OPTIONS.name());
        static final List<String> EXPOSED_HEADERS = List.of(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_TYPE);
        static final String PATTERN = "/**";
    }

    final class Jwt {
        static final String DEFAULT = "";
        static final String PREFIX = "Bearer ";
    }
}

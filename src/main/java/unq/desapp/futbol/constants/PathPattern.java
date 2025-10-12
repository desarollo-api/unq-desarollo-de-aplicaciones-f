package unq.desapp.futbol.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PathPattern {
    public static final String ACTUATOR = "/actuator/**";
    public static final String AUTH = "/auth/**";
}

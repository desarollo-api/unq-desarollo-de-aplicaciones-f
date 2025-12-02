package unq.desapp.futbol.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class NoUpcomingMatchException extends RuntimeException {
    public NoUpcomingMatchException(String message) {
        super(message);
    }
}

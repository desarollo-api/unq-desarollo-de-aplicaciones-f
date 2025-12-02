package unq.desapp.futbol.exceptions;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(NoUpcomingMatchException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleNoUpcomingMatchException(NoUpcomingMatchException ex) {
        Map<String, String> errorResponse = Map.of(
                "error", "Not Found",
                "message", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TeamNotFoundException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleTeamNotFoundException(TeamNotFoundException ex) {
        Map<String, String> errorResponse = Map.of(
                "error", "Not Found",
                "message", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
}
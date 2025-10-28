package unq.desapp.futbol.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class SearchHistoryEntry {

    @NonNull
    private final String query;

    @NonNull
    private final LocalDateTime timestamp;

    public SearchHistoryEntry(String query) {
        this.query = query;
        this.timestamp = LocalDateTime.now();
    }
}

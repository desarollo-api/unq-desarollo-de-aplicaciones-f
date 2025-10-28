package unq.desapp.futbol.model;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class SearchHistoryEntry {

    @NonNull
    private final SearchType type;
    @NonNull
    private final String query;

    @NonNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS")
    private final LocalDateTime timestamp;

    public SearchHistoryEntry(SearchType type, String query) {
        this.type = type;
        this.query = query;
        this.timestamp = LocalDateTime.now();
    }
}
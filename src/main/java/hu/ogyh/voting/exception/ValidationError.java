package hu.ogyh.voting.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/** A „Validációs hiba” válasz {@code hibak} tömbjének egy eleme. */
public record ValidationError(@JsonProperty("mezo") String field, @JsonProperty("uzenet") String message) {

    public ValidationError {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(message, "message");
    }
}

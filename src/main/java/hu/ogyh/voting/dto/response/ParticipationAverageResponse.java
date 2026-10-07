package hu.ogyh.voting.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Objects;

public record ParticipationAverageResponse(@JsonProperty("atlag") BigDecimal average) {

    public ParticipationAverageResponse {
        Objects.requireNonNull(average, "average");
    }
}

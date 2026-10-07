package hu.ogyh.voting.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import hu.ogyh.voting.domain.enums.VoteType;
import java.util.Objects;

public record VoteResponse(@JsonProperty("kepviselo") String memberId, @JsonProperty("szavazat") VoteType choice) {

    public VoteResponse {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(choice, "choice");
    }
}

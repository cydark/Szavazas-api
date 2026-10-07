package hu.ogyh.voting.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import hu.ogyh.voting.domain.enums.VoteType;
import java.util.Objects;

public record MemberVoteResponse(@JsonProperty("szavazat") VoteType choice) {

    public MemberVoteResponse {
        Objects.requireNonNull(choice, "choice");
    }
}

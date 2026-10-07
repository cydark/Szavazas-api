package hu.ogyh.voting.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import hu.ogyh.voting.domain.enums.ProcedureType;
import hu.ogyh.voting.domain.enums.ResultType;
import hu.ogyh.voting.domain.enums.VotingType;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.Builder;

/** 4. Napi szavazások egy eleme. */
@Builder
public record VotingResponse(
        @JsonProperty("idopont") Instant votedTime,
        @JsonProperty("targy") String subject,
        @JsonProperty("tipus") VotingType type,
        @JsonProperty("eljaras") ProcedureType procedure,
        @JsonProperty("elnok") String presidentId,
        @JsonProperty("eredmeny") ResultType result,
        @JsonProperty("kepviselokSzama") long memberCount,
        @JsonProperty("szavazatok") List<VoteResponse> votes) {

    public VotingResponse {
        Objects.requireNonNull(votedTime, "votedTime");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(procedure, "procedure");
        Objects.requireNonNull(presidentId, "presidentId");
        Objects.requireNonNull(result, "result");
        votes = List.copyOf(votes);
    }
}

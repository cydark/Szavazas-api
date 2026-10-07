package hu.ogyh.voting.dto.request;

import static hu.ogyh.voting.domain.FieldLimits.MEMBER_ID_MAX_LENGTH;
import static hu.ogyh.voting.domain.FieldLimits.MEMBER_ID_PATTERN;
import static hu.ogyh.voting.domain.FieldLimits.SUBJECT_MAX_LENGTH;

import com.fasterxml.jackson.annotation.JsonProperty;
import hu.ogyh.voting.domain.enums.ProcedureType;
import hu.ogyh.voting.domain.enums.VotingType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** 1. Szavazás mentése. Null-tűrő: a Bean Validation a létrehozás után fut. */
public record SaveVotingRequest(
        @JsonProperty("idopont") @NotNull Instant votedTime,
        @JsonProperty("targy") @NotBlank @Size(max = SUBJECT_MAX_LENGTH) String subject,
        @JsonProperty("tipus") @NotNull VotingType type,
        @JsonProperty("eljaras") @NotNull ProcedureType procedure,
        @JsonProperty("elnok")
                @NotNull
                @Size(max = MEMBER_ID_MAX_LENGTH)
                @Pattern(regexp = MEMBER_ID_PATTERN, message = "{voting.memberId.pattern}")
                String presidentId,
        @JsonProperty("szavazatok") @NotEmpty List<@NotNull @Valid VoteRequest> votes) {

    public SaveVotingRequest {
        // a specifikáció másodperc pontosságú időpontot tárol
        votedTime = votedTime == null ? null : votedTime.truncatedTo(ChronoUnit.SECONDS);
    }
}

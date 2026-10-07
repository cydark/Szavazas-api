package hu.ogyh.voting.dto.request;

import static hu.ogyh.voting.domain.FieldLimits.MEMBER_ID_MAX_LENGTH;
import static hu.ogyh.voting.domain.FieldLimits.MEMBER_ID_PATTERN;

import com.fasterxml.jackson.annotation.JsonProperty;
import hu.ogyh.voting.domain.enums.VoteType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VoteRequest(
        @JsonProperty("kepviselo")
                @NotNull
                @Size(max = MEMBER_ID_MAX_LENGTH)
                @Pattern(regexp = MEMBER_ID_PATTERN, message = "{voting.memberId.pattern}")
                String memberId,
        @JsonProperty("szavazat") @NotNull VoteType choice) {}

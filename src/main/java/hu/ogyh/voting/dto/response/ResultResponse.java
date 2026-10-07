package hu.ogyh.voting.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import hu.ogyh.voting.domain.enums.ResultType;
import java.util.Objects;
import lombok.Builder;

@Builder
public record ResultResponse(
        @JsonProperty("eredmeny") ResultType result,
        @JsonProperty("kepviselokSzama") long memberCount,
        @JsonProperty("igenekSzama") long yesCount,
        @JsonProperty("nemekSzama") long noCount,
        @JsonProperty("tartozkodasokSzama") long abstainCount) {

    public ResultResponse {
        Objects.requireNonNull(result, "result");
    }
}

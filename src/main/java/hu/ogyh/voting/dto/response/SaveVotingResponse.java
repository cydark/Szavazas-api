package hu.ogyh.voting.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public record SaveVotingResponse(@JsonProperty("szavazasId") String publicId) {

    public SaveVotingResponse {
        Objects.requireNonNull(publicId, "publicId");
    }
}

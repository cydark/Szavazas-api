package hu.ogyh.voting.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DailyVotingsResponse(@JsonProperty("szavazasok") List<VotingResponse> votings) {

    public DailyVotingsResponse {
        votings = List.copyOf(votings);
    }
}

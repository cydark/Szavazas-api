package hu.ogyh.voting.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import hu.ogyh.voting.domain.enums.ProcedureType;
import hu.ogyh.voting.domain.enums.VotingType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SaveVotingRequestTest {

    @Test
    @DisplayName("Az időpont másodpercre csonkolódik")
    void truncatesVotedTimeToSeconds() {
        SaveVotingRequest request = requestAt(Instant.parse("2023-09-28T11:06:25.987654Z"));

        assertThat(request.votedTime()).isEqualTo(Instant.parse("2023-09-28T11:06:25Z"));
    }

    @Test
    @DisplayName("Hiányzó időpontnál nem dob kivételt, a validáció jelzi a hibát")
    void toleratesMissingVotedTime() {
        assertThat(requestAt(null).votedTime()).isNull();
    }

    private static SaveVotingRequest requestAt(Instant votedTime) {
        return new SaveVotingRequest(votedTime, "Tárgy", VotingType.PRESENCE, ProcedureType.NORMAL, "K1", List.of());
    }
}

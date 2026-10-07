package hu.ogyh.voting.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class VotingTypeTest {

    @DisplayName("A többségi szabály határesetei")
    @ParameterizedTest(name = "{0}: igen={1}, jelenlévő={2}, teljes={3} -> elfogadott={4}")
    @CsvFileSource(resources = "/voting-type-cases.csv", numLinesToSkip = 1)
    void isAccepted(VotingType type, long yesCount, long presentCount, long totalMembers, boolean expected) {
        assertThat(type.isAccepted(yesCount, presentCount, totalMembers)).isEqualTo(expected);
    }
}

package hu.ogyh.voting.utils;

import static org.assertj.core.api.Assertions.assertThat;

import hu.ogyh.voting.domain.FieldLimits;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

class VotingIdGeneratorTest {

    @RepeatedTest(100)
    @DisplayName("Az azonosító 2 nagybetű és 4 számjegy")
    void generatesIdInSpecifiedFormat() {
        assertThat(VotingIdGenerator.generate()).matches(FieldLimits.PUBLIC_ID_PATTERN);
    }
}

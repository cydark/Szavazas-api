package hu.ogyh.voting.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

@DisplayName("A konfiguráció érvénytelen teljes létszámmal nem indul el")
class VotingPropertiesTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(PropertiesConfiguration.class);

    @DisplayName("Nem pozitív teljes létszámnál az indulás hibával leáll")
    @ParameterizedTest(name = "voting.total-members={0}")
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveTotalMembers(long totalMembers) {
        contextRunner
                .withPropertyValues("voting.total-members=" + totalMembers)
                .run(context ->
                        assertThat(context).hasFailed().getFailure().rootCause().hasMessageContaining("totalMembers"));
    }

    @DisplayName("Pozitív teljes létszám beolvasódik (a hibás eset ellenpróbája)")
    @ParameterizedTest(name = "voting.total-members={0}")
    @ValueSource(longs = {1, 199})
    void bindsPositiveTotalMembers(long totalMembers) {
        contextRunner.withPropertyValues("voting.total-members=" + totalMembers).run(context -> assertThat(
                        context.getBean(VotingProperties.class).totalMembers())
                .isEqualTo(totalMembers));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(VotingProperties.class)
    static class PropertiesConfiguration {}
}

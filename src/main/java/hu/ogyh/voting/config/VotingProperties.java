package hu.ogyh.voting.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 3.4 Konfiguráció.
 *
 * @param totalMembers az Országgyűlés teljes létszáma, a minősített többség alapja
 */
@Validated
@ConfigurationProperties("voting")
public record VotingProperties(@Positive long totalMembers) {}

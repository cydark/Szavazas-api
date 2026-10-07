package hu.ogyh.voting.utils;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * URL-barát, véletlenszerű szavazásazonosító: 2 nagybetű és 4 számjegy (formátuma:
 * {@link hu.ogyh.voting.domain.FieldLimits#PUBLIC_ID_PATTERN}). Az egyediséget a hívó ellenőrzi.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VotingIdGenerator {

    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final int LETTER_COUNT = 2;
    private static final int DIGIT_COUNT = 4;

    public static String generate() {
        RandomGenerator random = ThreadLocalRandom.current();
        StringBuilder id = new StringBuilder(LETTER_COUNT + DIGIT_COUNT);
        for (int i = 0; i < LETTER_COUNT; i++) {
            id.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }
        for (int i = 0; i < DIGIT_COUNT; i++) {
            id.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        }
        return id.toString();
    }
}

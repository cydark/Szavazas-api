package hu.ogyh.voting.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Mezőhosszak és formátumok egy helyen: a validáció, az oszlopméretek és a tesztek is ezt használják. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FieldLimits {

    public static final int SUBJECT_MAX_LENGTH = 500;
    public static final int MEMBER_ID_MAX_LENGTH = 50;
    public static final int PUBLIC_ID_LENGTH = 6;
    public static final int CODE_LENGTH = 1;

    /** Képviselő- és elnökazonosító: csak betű, szám és aláhúzás. */
    public static final String MEMBER_ID_PATTERN = "^[\\p{L}\\p{N}_]+$";

    /** Szavazás nyilvános azonosítója: 2 nagybetű és 4 számjegy. */
    public static final String PUBLIC_ID_PATTERN = "^[A-Z]{2}[0-9]{4}$";
}

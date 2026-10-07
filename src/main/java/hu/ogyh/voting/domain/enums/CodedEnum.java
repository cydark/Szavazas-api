package hu.ogyh.voting.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A specifikáció egybetűs kódjával leírt érték. JSON-ban a kód jelenik meg, beolvasáskor is a kód alapján
 * ismeri fel a Jackson; ismeretlen kód olvashatatlan kérést eredményez.
 */
public interface CodedEnum {

    @JsonValue
    String getCode();
}

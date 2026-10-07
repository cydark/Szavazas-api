package hu.ogyh.voting.domain.enums;

import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResultType implements CodedEnum {
    ACCEPTED("F"),
    REJECTED("U");

    @EnumeratedValue
    private final String code;

    public static ResultType of(boolean accepted) {
        return accepted ? ACCEPTED : REJECTED;
    }
}

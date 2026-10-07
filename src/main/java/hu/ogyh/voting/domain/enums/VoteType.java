package hu.ogyh.voting.domain.enums;

import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VoteType implements CodedEnum {
    YES("i"),
    NO("n"),
    ABSTAIN("t");

    @EnumeratedValue
    private final String code;
}

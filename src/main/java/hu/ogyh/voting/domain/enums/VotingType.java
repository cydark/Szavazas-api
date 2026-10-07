package hu.ogyh.voting.domain.enums;

import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VotingType implements CodedEnum {
    PRESENCE("j"),
    SIMPLE_MAJORITY("e"),
    QUALIFIED_MAJORITY("m");

    @EnumeratedValue
    private final String code;

    /** 2.3 Eredmény: egész aritmetikával, kerekítés nélkül. */
    public boolean isAccepted(long yesCount, long presentCount, long totalMembers) {
        return switch (this) {
            case PRESENCE -> true;
            case SIMPLE_MAJORITY -> yesCount * 2 > presentCount;
            case QUALIFIED_MAJORITY -> yesCount * 2 > totalMembers;
        };
    }
}

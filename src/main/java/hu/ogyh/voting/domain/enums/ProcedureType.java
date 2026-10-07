package hu.ogyh.voting.domain.enums;

import jakarta.persistence.EnumeratedValue;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProcedureType implements CodedEnum {
    NORMAL("n", false),
    URGENT("s", true),
    EXCEPTIONAL("k", true),
    DEVIATING_FROM_RULES("e", true);

    @EnumeratedValue
    private final String code;

    /** Új eljárásnál a konstruktor kikényszeríti a döntést, hogy különleges-e. */
    private final boolean special;

    public static List<ProcedureType> specialProcedures() {
        return Arrays.stream(values()).filter(ProcedureType::isSpecial).toList();
    }
}

package hu.ogyh.voting.repository;

import hu.ogyh.voting.domain.enums.ProcedureType;
import hu.ogyh.voting.domain.enums.ResultType;
import java.util.Objects;

/** 5.2 egy aggregált sora: hány szavazás volt az adott eljárásban az adott eredménnyel. */
public record ProcedureResultCount(ProcedureType procedure, ResultType result, long count) {

    public ProcedureResultCount {
        Objects.requireNonNull(procedure, "procedure");
        Objects.requireNonNull(result, "result");
    }
}

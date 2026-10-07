package hu.ogyh.voting.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import hu.ogyh.voting.domain.enums.ProcedureType;
import hu.ogyh.voting.domain.enums.ResultType;
import java.util.List;
import java.util.Objects;

/** 5.2 Különleges eljárások száma eljárásonként és eredményenként, majd az összesítések. */
public record SpecialProceduresResponse(@JsonProperty("szavazasok") List<Row> rows) {

    /** Az összesítő sorok jelölése a specifikáció szerint. */
    public static final String TOTAL = "összes";

    public SpecialProceduresResponse {
        rows = List.copyOf(rows);
    }

    public record Row(
            @JsonProperty("eljaras") String procedure,
            @JsonProperty("eredmeny") String result,
            @JsonProperty("szam") long count) {

        public Row {
            Objects.requireNonNull(procedure, "procedure");
            Objects.requireNonNull(result, "result");
        }

        public static Row perProcedure(ProcedureType procedure, ResultType result, long count) {
            return new Row(procedure.getCode(), result.getCode(), count);
        }

        public static Row perResult(ResultType result, long count) {
            return new Row(TOTAL, result.getCode(), count);
        }

        public static Row total(long count) {
            return new Row(TOTAL, TOTAL, count);
        }
    }
}

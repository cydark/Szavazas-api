package hu.ogyh.voting.exception;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;

/** Üzleti szabályt sértő kérés (400). Az üzenet a kliensnek szól, a hiba okát írja le. */
public final class InvalidRequestException extends RuntimeException {

    private InvalidRequestException(String message) {
        super(message);
    }

    public static InvalidRequestException presidentNotVoting(String presidentId) {
        return new InvalidRequestException("Az elnök (" + presidentId + ") nem szerepel a szavazók között");
    }

    public static InvalidRequestException multipleVotes(Collection<String> memberIds) {
        return new InvalidRequestException("Több szavazatot adott le: " + String.join(", ", memberIds));
    }

    public static InvalidRequestException votedTimeTaken(Instant votedTime) {
        return new InvalidRequestException("Erre az időpontra már van rögzített szavazás: " + votedTime);
    }

    public static InvalidRequestException reversedPeriod(LocalDate from, LocalDate to) {
        return new InvalidRequestException(
                "Az időszak vége (ig=" + to + ") nem lehet korábbi a kezdeténél (tol=" + from + ")");
    }
}

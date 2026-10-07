package hu.ogyh.voting.exception;

/** A keresett adat nem létezik (404). Az üzenet a kliensnek szól, és a két esetet megkülönbözteti. */
public final class NotFoundException extends RuntimeException {

    private NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException voting(String publicId) {
        return new NotFoundException("Nem található szavazás ezzel az azonosítóval: " + publicId);
    }

    public static NotFoundException vote(String publicId, String memberId) {
        return new NotFoundException("A(z) " + memberId + " képviselő nem szavazott a(z) " + publicId + " szavazáson");
    }
}

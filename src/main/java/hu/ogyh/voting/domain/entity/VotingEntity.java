package hu.ogyh.voting.domain.entity;

import static hu.ogyh.voting.domain.FieldLimits.CODE_LENGTH;
import static hu.ogyh.voting.domain.FieldLimits.MEMBER_ID_MAX_LENGTH;
import static hu.ogyh.voting.domain.FieldLimits.PUBLIC_ID_LENGTH;
import static hu.ogyh.voting.domain.FieldLimits.SUBJECT_MAX_LENGTH;

import hu.ogyh.voting.domain.enums.ProcedureType;
import hu.ogyh.voting.domain.enums.ResultType;
import hu.ogyh.voting.domain.enums.VoteType;
import hu.ogyh.voting.domain.enums.VotingType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "voting",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_voting_public_id", columnNames = "public_id"),
            @UniqueConstraint(name = VotingEntity.UK_VOTED_TIME, columnNames = "voted_time")
        },
        // az előző jelenléti szavazás kereséséhez
        indexes = @Index(name = "idx_voting_type_voted_time", columnList = "type, voted_time"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VotingEntity {

    /** Az egyidejű, azonos időpontú mentések felismeréséhez is ezt a nevet használjuk. */
    public static final String UK_VOTED_TIME = "uk_voting_voted_time";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "voting_seq")
    @SequenceGenerator(name = "voting_seq", sequenceName = "voting_seq")
    private Long id;

    @Column(name = "public_id", nullable = false, length = PUBLIC_ID_LENGTH)
    private String publicId;

    @Column(name = "voted_time", nullable = false)
    private Instant votedTime;

    @Column(name = "subject", nullable = false, length = SUBJECT_MAX_LENGTH)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = CODE_LENGTH)
    private VotingType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "procedure", nullable = false, length = CODE_LENGTH)
    private ProcedureType procedure;

    @Column(name = "president_id", nullable = false, length = MEMBER_ID_MAX_LENGTH)
    private String presidentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = CODE_LENGTH)
    private ResultType result;

    /** A mentéskor rögzített jelenléti létszám, hogy egy később rögzített, korábbi szavazás ne írja át. */
    @Column(name = "member_count", nullable = false)
    private long memberCount;

    @OneToMany(mappedBy = "voting", cascade = CascadeType.PERSIST)
    private List<VoteEntity> votes = new ArrayList<>();

    @Builder
    private VotingEntity(
            String publicId,
            Instant votedTime,
            String subject,
            VotingType type,
            ProcedureType procedure,
            String presidentId,
            ResultType result,
            long memberCount) {
        this.publicId = publicId;
        this.votedTime = votedTime;
        this.subject = subject;
        this.type = type;
        this.procedure = procedure;
        this.presidentId = presidentId;
        this.result = result;
        this.memberCount = memberCount;
    }

    /** Szavazat csak ezen keresztül kerülhet a szavazáshoz, így a kapcsolat mindkét oldala beáll. */
    public void addVote(String memberId, VoteType choice) {
        votes.add(new VoteEntity(this, memberId, choice));
    }

    public List<VoteEntity> getVotes() {
        return Collections.unmodifiableList(votes);
    }
}

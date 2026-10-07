package hu.ogyh.voting.domain.entity;

import static hu.ogyh.voting.domain.FieldLimits.CODE_LENGTH;
import static hu.ogyh.voting.domain.FieldLimits.MEMBER_ID_MAX_LENGTH;

import hu.ogyh.voting.domain.enums.VoteType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "vote",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_vote_voting_member",
                        columnNames = {"voting_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vote_seq")
    @SequenceGenerator(name = "vote_seq", sequenceName = "vote_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voting_id", nullable = false, foreignKey = @ForeignKey(name = "fk_vote_voting"))
    private VotingEntity voting;

    @Column(name = "member_id", nullable = false, length = MEMBER_ID_MAX_LENGTH)
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "choice", nullable = false, length = CODE_LENGTH)
    private VoteType choice;

    /** Csak a {@link VotingEntity#addVote} hívja. */
    VoteEntity(VotingEntity voting, String memberId, VoteType choice) {
        this.voting = voting;
        this.memberId = memberId;
        this.choice = choice;
    }
}

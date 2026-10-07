package hu.ogyh.voting.repository;

import hu.ogyh.voting.domain.entity.VotingEntity;
import hu.ogyh.voting.domain.enums.ProcedureType;
import hu.ogyh.voting.domain.enums.VotingType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VotingRepository extends JpaRepository<VotingEntity, Long> {

    Optional<VotingEntity> findByPublicId(String publicId);

    boolean existsByPublicId(String publicId);

    boolean existsByVotedTime(Instant votedTime);

    /** 2.3 Jelenlévők száma: a szavazást megelőző utolsó jelenléti szavazás létszáma. */
    default Optional<Long> findLastPresenceMemberCountBefore(Instant votedTime) {
        return findLastMemberCountBefore(VotingType.PRESENCE, votedTime);
    }

    @Query(
            """
            select v.memberCount from VotingEntity v
            where v.type = :type and v.votedTime < :votedTime
            order by v.votedTime desc
            limit 1""")
    Optional<Long> findLastMemberCountBefore(@Param("type") VotingType type, @Param("votedTime") Instant votedTime);

    /** 4. Napi szavazások: a szavazatokkal együtt, egyetlen lekérdezéssel (N+1 nélkül). */
    @Query(
            """
            select v from VotingEntity v
            left join fetch v.votes vote
            where v.votedTime >= :from and v.votedTime < :to
            order by v.votedTime, vote.id""")
    List<VotingEntity> findAllWithVotesBetween(@Param("from") Instant from, @Param("to") Instant to);

    /** 5.1 Részvételi átlag alapadatai: a jelenléti szavazások nélkül. */
    default ParticipationTotals sumParticipationBetween(Instant from, Instant to) {
        return sumParticipationExcludingTypeBetween(VotingType.PRESENCE, from, to);
    }

    @Query(
            """
            select new hu.ogyh.voting.repository.ParticipationTotals(count(vote), count(distinct vote.memberId))
            from VoteEntity vote join vote.voting v
            where v.type <> :excludedType and v.votedTime >= :from and v.votedTime < :to""")
    ParticipationTotals sumParticipationExcludingTypeBetween(
            @Param("excludedType") VotingType excludedType, @Param("from") Instant from, @Param("to") Instant to);

    /** 5.2 Különleges eljárások száma eljárásonként és eredményenként (a 0 darabos kombinációk nélkül). */
    default List<ProcedureResultCount> countSpecialProceduresBetween(Instant from, Instant to) {
        return countByProcedureAndResultBetween(ProcedureType.specialProcedures(), from, to);
    }

    @Query(
            """
            select new hu.ogyh.voting.repository.ProcedureResultCount(v.procedure, v.result, count(v))
            from VotingEntity v
            where v.procedure in :procedures and v.votedTime >= :from and v.votedTime < :to
            group by v.procedure, v.result""")
    List<ProcedureResultCount> countByProcedureAndResultBetween(
            @Param("procedures") Collection<ProcedureType> procedures,
            @Param("from") Instant from,
            @Param("to") Instant to);
}

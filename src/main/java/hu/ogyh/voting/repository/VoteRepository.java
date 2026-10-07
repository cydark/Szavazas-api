package hu.ogyh.voting.repository;

import hu.ogyh.voting.domain.entity.VoteEntity;
import hu.ogyh.voting.domain.entity.VotingEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<VoteEntity, Long> {

    Optional<VoteEntity> findByVotingAndMemberId(VotingEntity voting, String memberId);
}

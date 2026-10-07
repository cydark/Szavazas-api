package hu.ogyh.voting.dto.mapper;

import hu.ogyh.voting.domain.entity.VoteEntity;
import hu.ogyh.voting.domain.entity.VotingEntity;
import hu.ogyh.voting.domain.enums.ResultType;
import hu.ogyh.voting.domain.enums.VoteType;
import hu.ogyh.voting.dto.request.SaveVotingRequest;
import hu.ogyh.voting.dto.request.VoteRequest;
import hu.ogyh.voting.dto.response.MemberVoteResponse;
import hu.ogyh.voting.dto.response.ResultResponse;
import hu.ogyh.voting.dto.response.VoteResponse;
import hu.ogyh.voting.dto.response.VotingResponse;
import java.util.EnumMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Kézi leképezés: kevés DTO van, és a nevek (magyar JSON, angol Java) eltérnek. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VotingMapper {

    /** Az eredményt és a jelenléti létszámot a szolgáltatás számolja, mert adatbázis-állapottól függnek. */
    public static VotingEntity toEntity(
            SaveVotingRequest request, String publicId, ResultType result, long memberCount) {
        VotingEntity voting = VotingEntity.builder()
                .publicId(publicId)
                .votedTime(request.votedTime())
                .subject(request.subject())
                .type(request.type())
                .procedure(request.procedure())
                .presidentId(request.presidentId())
                .result(result)
                .memberCount(memberCount)
                .build();
        for (VoteRequest vote : request.votes()) {
            voting.addVote(vote.memberId(), vote.choice());
        }
        return voting;
    }

    public static MemberVoteResponse toMemberVoteResponse(VoteEntity vote) {
        return new MemberVoteResponse(vote.getChoice());
    }

    /** Szavazásonként legfeljebb néhány száz szavazat van, ezért a darabszámokat Java számolja. */
    public static ResultResponse toResultResponse(VotingEntity voting) {
        Map<VoteType, Long> counts = new EnumMap<>(VoteType.class);
        for (VoteEntity vote : voting.getVotes()) {
            counts.merge(vote.getChoice(), 1L, Long::sum);
        }
        return ResultResponse.builder()
                .result(voting.getResult())
                .memberCount(voting.getMemberCount())
                .yesCount(counts.getOrDefault(VoteType.YES, 0L))
                .noCount(counts.getOrDefault(VoteType.NO, 0L))
                .abstainCount(counts.getOrDefault(VoteType.ABSTAIN, 0L))
                .build();
    }

    public static VotingResponse toVotingResponse(VotingEntity voting) {
        return VotingResponse.builder()
                .votedTime(voting.getVotedTime())
                .subject(voting.getSubject())
                .type(voting.getType())
                .procedure(voting.getProcedure())
                .presidentId(voting.getPresidentId())
                .result(voting.getResult())
                .memberCount(voting.getMemberCount())
                .votes(voting.getVotes().stream()
                        .map(VotingMapper::toVoteResponse)
                        .toList())
                .build();
    }

    private static VoteResponse toVoteResponse(VoteEntity vote) {
        return new VoteResponse(vote.getMemberId(), vote.getChoice());
    }
}

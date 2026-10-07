package hu.ogyh.voting.service;

import hu.ogyh.voting.config.VotingProperties;
import hu.ogyh.voting.domain.entity.VotingEntity;
import hu.ogyh.voting.domain.enums.ProcedureType;
import hu.ogyh.voting.domain.enums.ResultType;
import hu.ogyh.voting.domain.enums.VoteType;
import hu.ogyh.voting.dto.mapper.VotingMapper;
import hu.ogyh.voting.dto.request.SaveVotingRequest;
import hu.ogyh.voting.dto.request.VoteRequest;
import hu.ogyh.voting.dto.response.DailyVotingsResponse;
import hu.ogyh.voting.dto.response.MemberVoteResponse;
import hu.ogyh.voting.dto.response.ParticipationAverageResponse;
import hu.ogyh.voting.dto.response.ResultResponse;
import hu.ogyh.voting.dto.response.SaveVotingResponse;
import hu.ogyh.voting.dto.response.SpecialProceduresResponse;
import hu.ogyh.voting.dto.response.SpecialProceduresResponse.Row;
import hu.ogyh.voting.exception.InvalidRequestException;
import hu.ogyh.voting.exception.NotFoundException;
import hu.ogyh.voting.repository.ParticipationTotals;
import hu.ogyh.voting.repository.ProcedureResultCount;
import hu.ogyh.voting.repository.VoteRepository;
import hu.ogyh.voting.repository.VotingRepository;
import hu.ogyh.voting.utils.TimeRange;
import hu.ogyh.voting.utils.VotingIdGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VotingService {

    private static final int AVERAGE_SCALE = 2;

    private final VotingRepository votingRepository;
    private final VoteRepository voteRepository;
    private final VotingProperties votingProperties;

    /** 1. Szavazás mentése: hiba esetén semmi nem rögzül. */
    @Transactional
    public SaveVotingResponse save(SaveVotingRequest request) {
        checkVoters(request);
        if (votingRepository.existsByVotedTime(request.votedTime())) {
            throw InvalidRequestException.votedTimeTaken(request.votedTime());
        }

        long memberCount = memberCount(request);
        long yesCount = request.votes().stream()
                .filter(vote -> vote.choice() == VoteType.YES)
                .count();
        boolean accepted = request.type().isAccepted(yesCount, memberCount, votingProperties.totalMembers());
        VotingEntity voting = VotingMapper.toEntity(request, generatePublicId(), ResultType.of(accepted), memberCount);

        try {
            votingRepository.saveAndFlush(voting);
        } catch (DataIntegrityViolationException exception) {
            // az előzetes ellenőrzés után egy egyidejű kérés foglalhatta le ugyanazt az időpontot
            if (isVotedTimeViolation(exception)) {
                throw InvalidRequestException.votedTimeTaken(request.votedTime());
            }
            throw exception;
        }
        return new SaveVotingResponse(voting.getPublicId());
    }

    /** 2. Képviselő szavazata. */
    public MemberVoteResponse getVote(String publicId, String memberId) {
        VotingEntity voting = findVoting(publicId);
        return voteRepository
                .findByVotingAndMemberId(voting, memberId)
                .map(VotingMapper::toMemberVoteResponse)
                .orElseThrow(() -> NotFoundException.vote(publicId, memberId));
    }

    /** 3. Szavazás eredménye. */
    public ResultResponse getResult(String publicId) {
        return VotingMapper.toResultResponse(findVoting(publicId));
    }

    /** 4. Napi szavazások budapesti nap szerint. */
    public DailyVotingsResponse getDailyVotings(LocalDate day) {
        TimeRange range = TimeRange.ofDay(day);
        return new DailyVotingsResponse(votingRepository.findAllWithVotesBetween(range.from(), range.to()).stream()
                .map(VotingMapper::toVotingResponse)
                .toList());
    }

    /** 5.1 Részvételi átlag: azokra a képviselőkre, akik legalább egyszer szavaztak. */
    public ParticipationAverageResponse getParticipationAverage(LocalDate firstDay, LocalDate lastDay) {
        TimeRange range = period(firstDay, lastDay);
        ParticipationTotals totals = votingRepository.sumParticipationBetween(range.from(), range.to());
        if (totals.memberCount() == 0) {
            return new ParticipationAverageResponse(BigDecimal.ZERO.setScale(AVERAGE_SCALE));
        }
        BigDecimal average = BigDecimal.valueOf(totals.voteCount())
                .divide(BigDecimal.valueOf(totals.memberCount()), AVERAGE_SCALE, RoundingMode.HALF_UP);
        return new ParticipationAverageResponse(average);
    }

    /** 5.2 Különleges eljárások: minden kombináció (a 0 darabosak is), majd az összesítések. */
    public SpecialProceduresResponse countSpecialProcedures(LocalDate firstDay, LocalDate lastDay) {
        TimeRange range = period(firstDay, lastDay);
        Map<ProcedureType, Map<ResultType, Long>> counts = new EnumMap<>(ProcedureType.class);
        for (ProcedureResultCount count : votingRepository.countSpecialProceduresBetween(range.from(), range.to())) {
            counts.computeIfAbsent(count.procedure(), procedure -> new EnumMap<>(ResultType.class))
                    .put(count.result(), count.count());
        }

        List<Row> rows = new ArrayList<>();
        Map<ResultType, Long> totalsPerResult = new EnumMap<>(ResultType.class);
        for (ProcedureType procedure : ProcedureType.specialProcedures()) {
            Map<ResultType, Long> procedureCounts = counts.getOrDefault(procedure, Map.of());
            for (ResultType result : ResultType.values()) {
                long count = procedureCounts.getOrDefault(result, 0L);
                rows.add(Row.perProcedure(procedure, result, count));
                totalsPerResult.merge(result, count, Long::sum);
            }
        }
        long total = 0;
        for (ResultType result : ResultType.values()) {
            long count = totalsPerResult.getOrDefault(result, 0L);
            rows.add(Row.perResult(result, count));
            total += count;
        }
        rows.add(Row.total(total));
        return new SpecialProceduresResponse(rows);
    }

    /** 2.2/1 üzleti szabályok: az elnök szavazott, és senki nem szavazott többször. */
    private static void checkVoters(SaveVotingRequest request) {
        Set<String> voters = new HashSet<>();
        Set<String> multipleVoters = new LinkedHashSet<>();
        for (VoteRequest vote : request.votes()) {
            if (!voters.add(vote.memberId())) {
                multipleVoters.add(vote.memberId());
            }
        }
        if (!voters.contains(request.presidentId())) {
            throw InvalidRequestException.presidentNotVoting(request.presidentId());
        }
        if (!multipleVoters.isEmpty()) {
            throw InvalidRequestException.multipleVotes(multipleVoters);
        }
    }

    /** 2.3 Jelenlévők száma, a mentéskor rögzítve. */
    private long memberCount(SaveVotingRequest request) {
        long participantCount = request.votes().size();
        return switch (request.type()) {
            case PRESENCE -> participantCount;
            case SIMPLE_MAJORITY, QUALIFIED_MAJORITY ->
                votingRepository
                        .findLastPresenceMemberCountBefore(request.votedTime())
                        .orElse(participantCount);
        };
    }

    /** Ütközésnél újat generál; végső védvonal az {@code uk_voting_public_id} megkötés. */
    private String generatePublicId() {
        String publicId;
        do {
            publicId = VotingIdGenerator.generate();
        } while (votingRepository.existsByPublicId(publicId));
        return publicId;
    }

    private VotingEntity findVoting(String publicId) {
        return votingRepository.findByPublicId(publicId).orElseThrow(() -> NotFoundException.voting(publicId));
    }

    /** Az időszak mindkét napja beleszámít; fordított időszak hibás kérés. */
    private static TimeRange period(LocalDate firstDay, LocalDate lastDay) {
        if (lastDay.isBefore(firstDay)) {
            throw InvalidRequestException.reversedPeriod(firstDay, lastDay);
        }
        return TimeRange.ofDays(firstDay, lastDay);
    }

    private static boolean isVotedTimeViolation(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                return violation.getConstraintName().toLowerCase().contains(VotingEntity.UK_VOTED_TIME);
            }
        }
        return false;
    }
}

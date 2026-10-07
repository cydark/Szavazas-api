package hu.ogyh.voting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import hu.ogyh.voting.domain.enums.ProcedureType;
import hu.ogyh.voting.domain.enums.ResultType;
import hu.ogyh.voting.domain.enums.VoteType;
import hu.ogyh.voting.domain.enums.VotingType;
import hu.ogyh.voting.dto.request.SaveVotingRequest;
import hu.ogyh.voting.dto.request.VoteRequest;
import hu.ogyh.voting.dto.response.ResultResponse;
import hu.ogyh.voting.dto.response.SpecialProceduresResponse.Row;
import hu.ogyh.voting.dto.response.VotingResponse;
import hu.ogyh.voting.exception.InvalidRequestException;
import hu.ogyh.voting.exception.NotFoundException;
import hu.ogyh.voting.repository.VoteRepository;
import hu.ogyh.voting.repository.VotingRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class VotingServiceTest {

    private static final Instant T0 = Instant.parse("2023-12-13T10:00:00Z");

    @Autowired
    private VotingService votingService;

    @Autowired
    private VotingRepository votingRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Nested
    @DisplayName("Mentés – üzleti szabályok")
    class SaveRules {

        @Test
        @DisplayName("Az elnöknek szerepelnie kell a szavazók között")
        void presidentMustVote() {
            SaveVotingRequest request = request(T0, VotingType.PRESENCE, "K9", votes("K1", "K2"));

            assertThatThrownBy(() -> votingService.save(request))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("K9");
            assertThat(votingRepository.count()).isZero();
        }

        @Test
        @DisplayName("Többször szavazó képviselőknél a hiba mindegyiket felsorolja, egyszer")
        void listsEveryMultipleVoter() {
            SaveVotingRequest request =
                    request(T0, VotingType.PRESENCE, "K1", votes("K1", "K2", "K2", "K3", "K3", "K3"));

            assertThatThrownBy(() -> votingService.save(request))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessage("Több szavazatot adott le: K2, K3");
            assertThat(votingRepository.count()).isZero();
        }

        @Test
        @DisplayName("Ugyanarra a másodpercre nem lehet két szavazást rögzíteni")
        void rejectsSameSecond() {
            votingService.save(request(T0, VotingType.PRESENCE, "K1", votes("K1")));
            SaveVotingRequest sameSecond = request(T0.plusMillis(900), VotingType.PRESENCE, "K1", votes("K1"));

            assertThatThrownBy(() -> votingService.save(sameSecond)).isInstanceOf(InvalidRequestException.class);
            assertThat(votingRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Sikeres mentés után az azonosítóval lekérdezhető a képviselő szavazata")
        void savedVoteCanBeQueried() {
            String publicId = votingService
                    .save(request(T0, VotingType.PRESENCE, "K1", List.of(vote("K1", VoteType.NO))))
                    .publicId();

            assertThat(votingService.getVote(publicId, "K1").choice()).isEqualTo(VoteType.NO);
            assertThatThrownBy(() -> votingService.getVote(publicId, "K2"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("nem szavazott");
            assertThatThrownBy(() -> votingService.getVote("XX0000", "K1"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Nem található szavazás");
        }
    }

    @Nested
    @DisplayName("Jelenlévők száma")
    class MemberCount {

        @Test
        @DisplayName("Jelenléti szavazásnál a saját résztvevőinek száma")
        void presenceUsesOwnParticipants() {
            assertThat(resultOf(request(T0, VotingType.PRESENCE, "K1", votes("K1", "K2", "K3")))
                            .memberCount())
                    .isEqualTo(3);
        }

        @Test
        @DisplayName("Többségi szavazásnál a megelőző utolsó jelenléti szavazás létszáma")
        void majorityUsesLastPreviousPresence() {
            votingService.save(request(T0, VotingType.PRESENCE, "K1", votes("K1", "K2")));
            votingService.save(request(T0.plusSeconds(10), VotingType.PRESENCE, "K1", votes("K1", "K2", "K3", "K4")));
            // későbbi jelenléti szavazás nem számít
            votingService.save(request(T0.plusSeconds(30), VotingType.PRESENCE, "K1", votes("K1")));

            ResultResponse result =
                    resultOf(request(T0.plusSeconds(20), VotingType.SIMPLE_MAJORITY, "K1", votes("K1")));

            assertThat(result.memberCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("Előző jelenléti szavazás nélkül a saját résztvevők száma")
        void withoutPreviousPresenceUsesOwnParticipants() {
            assertThat(resultOf(request(T0, VotingType.SIMPLE_MAJORITY, "K1", votes("K1", "K2")))
                            .memberCount())
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("Később rögzített, korábbi jelenléti szavazás nem írja át a meghozott eredményt")
        void laterRecordedEarlierPresenceDoesNotChangeResult() {
            String publicId = votingService
                    .save(request(T0, VotingType.SIMPLE_MAJORITY, "K1", votes("K1", "K2")))
                    .publicId();
            votingService.save(
                    request(T0.minusSeconds(60), VotingType.PRESENCE, "K1", votes("K1", "K2", "K3", "K4", "K5", "K6")));

            ResultResponse result = votingService.getResult(publicId);

            assertThat(result.memberCount()).isEqualTo(2);
            assertThat(result.result()).isEqualTo(ResultType.ACCEPTED);
        }
    }

    @Nested
    @DisplayName("Eredmény")
    class Result {

        @Test
        @DisplayName("Egyszerű többség: a jelenlévők több mint fele igennel szavazott (4-ből 3 igen elfogadott)")
        void simpleMajorityAccepted() {
            votingService.save(request(T0, VotingType.PRESENCE, "K1", votes("K1", "K2", "K3", "K4")));

            ResultResponse result = resultOf(request(
                    T0.plusSeconds(1),
                    VotingType.SIMPLE_MAJORITY,
                    "K1",
                    List.of(vote("K1", VoteType.YES), vote("K2", VoteType.YES), vote("K3", VoteType.YES))));

            assertThat(result.result()).isEqualTo(ResultType.ACCEPTED);
            assertThat(result.yesCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Egyszerű többség: pontosan a fele nem elég (4-ből 2 igen elutasított)")
        void simpleMajorityHalfIsRejected() {
            votingService.save(request(T0, VotingType.PRESENCE, "K1", votes("K1", "K2", "K3", "K4")));

            ResultResponse result = resultOf(request(
                    T0.plusSeconds(1),
                    VotingType.SIMPLE_MAJORITY,
                    "K1",
                    List.of(vote("K1", VoteType.YES), vote("K2", VoteType.YES), vote("K3", VoteType.ABSTAIN))));

            assertThat(result.result()).isEqualTo(ResultType.REJECTED);
            assertThat(result)
                    .extracting(ResultResponse::yesCount, ResultResponse::noCount, ResultResponse::abstainCount)
                    .containsExactly(2L, 0L, 1L);
        }

        @Test
        @DisplayName("Minősített többség: a teljes létszám (200) több mint fele kell – 101 igen elfogadott")
        void qualifiedMajorityAccepted() {
            ResultResponse result = resultOf(request(T0, VotingType.QUALIFIED_MAJORITY, "K1", yesVotes(101)));

            assertThat(result.result()).isEqualTo(ResultType.ACCEPTED);
            assertThat(result.memberCount()).isEqualTo(101);
        }

        @Test
        @DisplayName("Minősített többség: 100 igen elutasított, akkor is, ha mindenki jelenlévő igennel szavazott")
        void qualifiedMajorityRejected() {
            assertThat(resultOf(request(T0, VotingType.QUALIFIED_MAJORITY, "K1", yesVotes(100)))
                            .result())
                    .isEqualTo(ResultType.REJECTED);
        }

        @Test
        @DisplayName("Jelenléti szavazás mindig elfogadott")
        void presenceAlwaysAccepted() {
            assertThat(resultOf(request(T0, VotingType.PRESENCE, "K1", List.of(vote("K1", VoteType.NO))))
                            .result())
                    .isEqualTo(ResultType.ACCEPTED);
        }
    }

    @Nested
    @DisplayName("Budapesti nap")
    class BudapestDay {

        @Test
        @DisplayName("23:30Z Budapesten már a következő nap")
        void lateUtcBelongsToNextDay() {
            votingService.save(request(Instant.parse("2023-12-13T23:30:00Z"), VotingType.PRESENCE, "K1", votes("K1")));

            assertThat(dailyTimes(LocalDate.parse("2023-12-13"))).isEmpty();
            assertThat(dailyTimes(LocalDate.parse("2023-12-14"))).containsExactly("2023-12-13T23:30:00Z");
        }

        @Test
        @DisplayName("Tavaszi óraátállítás: a 23 órás nap határai")
        void springForwardDay() {
            saveAt("2023-03-25T22:59:59Z", "2023-03-25T23:00:00Z", "2023-03-26T21:59:59Z", "2023-03-26T22:00:00Z");

            assertThat(dailyTimes(LocalDate.parse("2023-03-26")))
                    .containsExactly("2023-03-25T23:00:00Z", "2023-03-26T21:59:59Z");
        }

        @Test
        @DisplayName("Őszi óraátállítás: a 25 órás nap határai")
        void fallBackDay() {
            saveAt("2023-10-28T21:59:59Z", "2023-10-28T22:00:00Z", "2023-10-29T22:59:59Z", "2023-10-29T23:00:00Z");

            assertThat(dailyTimes(LocalDate.parse("2023-10-29")))
                    .containsExactly("2023-10-28T22:00:00Z", "2023-10-29T22:59:59Z");
        }

        @Test
        @DisplayName("A napi lista időrendben adja a szavazásokat a szavazataikkal")
        void dailyListInChronologicalOrder() {
            votingService.save(request(T0.plusSeconds(60), VotingType.PRESENCE, "K1", votes("K1", "K2")));
            votingService.save(request(T0, VotingType.PRESENCE, "K1", votes("K1")));

            List<VotingResponse> votings =
                    votingService.getDailyVotings(LocalDate.parse("2023-12-13")).votings();

            assertThat(votings).extracting(VotingResponse::votedTime).containsExactly(T0, T0.plusSeconds(60));
            assertThat(votings.get(1).votes()).hasSize(2);
        }

        private void saveAt(String... times) {
            for (String time : times) {
                votingService.save(request(Instant.parse(time), VotingType.PRESENCE, "K1", votes("K1")));
            }
        }

        private List<String> dailyTimes(LocalDate day) {
            return votingService.getDailyVotings(day).votings().stream()
                    .map(voting -> voting.votedTime().toString())
                    .toList();
        }
    }

    @Nested
    @DisplayName("Kimutatások")
    class Statistics {

        private static final LocalDate FIRST_DAY = LocalDate.parse("2023-12-01");
        private static final LocalDate LAST_DAY = LocalDate.parse("2023-12-31");

        @Test
        @DisplayName("Részvételi átlag: jelenléti szavazások nélkül, a szavazó képviselőkre, HALF_UP 2 tizedesre")
        void participationAverage() {
            votingService.save(request(T0, VotingType.PRESENCE, "K1", votes("K1", "K2", "K3", "K4")));
            votingService.save(request(T0.plusSeconds(1), VotingType.SIMPLE_MAJORITY, "K1", votes("K1", "K2")));
            votingService.save(request(T0.plusSeconds(2), VotingType.SIMPLE_MAJORITY, "K1", votes("K1", "K3")));
            votingService.save(request(T0.plusSeconds(3), VotingType.QUALIFIED_MAJORITY, "K1", votes("K1")));
            // időszakon kívül
            votingService.save(request(
                    Instant.parse("2024-01-01T10:00:00Z"), VotingType.SIMPLE_MAJORITY, "K1", votes("K1", "K2")));

            // K1: 3, K2: 1, K3: 1 -> 5 / 3 = 1.666... -> 1.67
            assertThat(votingService
                            .getParticipationAverage(FIRST_DAY, LAST_DAY)
                            .average())
                    .isEqualByComparingTo("1.67");
        }

        @Test
        @DisplayName("Üres időszakban az átlag 0")
        void participationAverageEmpty() {
            assertThat(votingService
                            .getParticipationAverage(FIRST_DAY, LAST_DAY)
                            .average())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Az időszak mindkét napja beleszámít (budapesti nap szerint)")
        void periodIncludesBothDays() {
            votingService.save(
                    request(Instant.parse("2023-11-30T23:00:00Z"), VotingType.SIMPLE_MAJORITY, "K1", votes("K1")));
            votingService.save(
                    request(Instant.parse("2023-12-31T22:59:59Z"), VotingType.SIMPLE_MAJORITY, "K1", votes("K1")));

            assertThat(votingService
                            .getParticipationAverage(FIRST_DAY, LAST_DAY)
                            .average())
                    .isEqualByComparingTo("2");
        }

        @Test
        @DisplayName("Fordított időszak érvénytelen kérés")
        void reversedPeriod() {
            assertThatThrownBy(() -> votingService.getParticipationAverage(LAST_DAY, FIRST_DAY))
                    .isInstanceOf(InvalidRequestException.class);
            assertThatThrownBy(() -> votingService.countSpecialProcedures(LAST_DAY, FIRST_DAY))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Különleges eljárások: minden kombináció a 0 darabosokkal, majd az összesítések")
        void specialProcedures() {
            // a jelenléti szavazás is beleszámít, ha különleges eljárásban volt (mindig elfogadott)
            saveProcedure(0, VotingType.PRESENCE, ProcedureType.URGENT, false);
            saveProcedure(1, VotingType.SIMPLE_MAJORITY, ProcedureType.URGENT, true);
            saveProcedure(2, VotingType.SIMPLE_MAJORITY, ProcedureType.URGENT, false);
            saveProcedure(3, VotingType.SIMPLE_MAJORITY, ProcedureType.EXCEPTIONAL, false);
            saveProcedure(4, VotingType.SIMPLE_MAJORITY, ProcedureType.NORMAL, true);

            assertThat(votingService.countSpecialProcedures(FIRST_DAY, LAST_DAY).rows())
                    .containsExactly(
                            new Row("s", "F", 2),
                            new Row("s", "U", 1),
                            new Row("k", "F", 0),
                            new Row("k", "U", 1),
                            new Row("e", "F", 0),
                            new Row("e", "U", 0),
                            new Row("összes", "F", 2),
                            new Row("összes", "U", 2),
                            new Row("összes", "összes", 4));
        }

        /** Egyetlen jelenlévő (K1): igennel elfogadott, nemmel elutasított többségi szavazás. */
        private void saveProcedure(int second, VotingType type, ProcedureType procedure, boolean yes) {
            VoteRequest vote = vote("K1", yes ? VoteType.YES : VoteType.NO);
            votingService.save(
                    new SaveVotingRequest(T0.plusSeconds(second), "Tárgy", type, procedure, "K1", List.of(vote)));
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Egyidejű kéréseknél a foglalt időpont érvénytelen kérés, és csak egy szavazás rögzül")
    void concurrentSaveWithSameTime() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Boolean>> saves = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            saves.add(() -> {
                start.await();
                try {
                    votingService.save(request(T0, VotingType.PRESENCE, "K1", votes("K1")));
                    return true;
                } catch (InvalidRequestException expected) {
                    return false;
                }
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(saves.size());
        try {
            List<Future<Boolean>> results = saves.stream().map(executor::submit).toList();
            start.countDown();
            List<Boolean> outcomes = new ArrayList<>();
            for (Future<Boolean> result : results) {
                outcomes.add(result.get(10, TimeUnit.SECONDS));
            }

            assertThat(outcomes).containsExactlyInAnyOrder(true, false);
            assertThat(votingRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            // saját tranzakcióban futott, ezért kézzel takarítunk: előbb a szavazatok (fk_vote_voting)
            voteRepository.deleteAllInBatch();
            votingRepository.deleteAllInBatch();
        }
    }

    private ResultResponse resultOf(SaveVotingRequest request) {
        return votingService.getResult(votingService.save(request).publicId());
    }

    private static SaveVotingRequest request(
            Instant votedTime, VotingType type, String presidentId, List<VoteRequest> votes) {
        return new SaveVotingRequest(votedTime, "Tárgy", type, ProcedureType.NORMAL, presidentId, votes);
    }

    /** Mindenki igennel szavaz. */
    private static List<VoteRequest> votes(String... memberIds) {
        return Arrays.stream(memberIds)
                .map(memberId -> vote(memberId, VoteType.YES))
                .toList();
    }

    private static List<VoteRequest> yesVotes(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> vote("K" + i, VoteType.YES))
                .toList();
    }

    private static VoteRequest vote(String memberId, VoteType choice) {
        return new VoteRequest(memberId, choice);
    }
}

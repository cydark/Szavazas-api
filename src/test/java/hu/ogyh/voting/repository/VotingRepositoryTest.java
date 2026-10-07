package hu.ogyh.voting.repository;

import static org.assertj.core.api.Assertions.assertThat;

import hu.ogyh.voting.domain.entity.VoteEntity;
import hu.ogyh.voting.domain.entity.VotingEntity;
import hu.ogyh.voting.domain.enums.ProcedureType;
import hu.ogyh.voting.domain.enums.ResultType;
import hu.ogyh.voting.domain.enums.VoteType;
import hu.ogyh.voting.domain.enums.VotingType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class VotingRepositoryTest {

    private static final Instant VOTED_TIME = Instant.parse("2023-12-13T14:30:00Z");

    @Autowired
    private VotingRepository votingRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("Az enumok a specifikáció kódjával tárolódnak, nem a Java-nevükkel")
    void storesSpecificationCodes() {
        VotingEntity voting =
                voting("AB1234", VOTED_TIME, VotingType.QUALIFIED_MAJORITY, ProcedureType.URGENT, ResultType.REJECTED);
        voting.addVote("K1", VoteType.ABSTAIN);
        votingRepository.saveAndFlush(voting);

        Object[] votingRow = (Object[]) entityManager
                .createNativeQuery("select type, procedure, result from voting")
                .getSingleResult();
        Object choice =
                entityManager.createNativeQuery("select choice from vote").getSingleResult();

        // a H2 a varchar(1) oszlopot natív lekérdezésben Character-ként adja vissza
        assertThat(Arrays.stream(votingRow).map(String::valueOf)).containsExactly("m", "s", "U");
        assertThat(String.valueOf(choice)).isEqualTo("t");
    }

    @Test
    @DisplayName("A napi lista a szavazatokat egyetlen lekérdezéssel tölti be (nincs N+1)")
    void loadsDailyVotingsWithVotesInOneStatement() {
        for (int i = 0; i < 3; i++) {
            VotingEntity voting = voting(
                    "AB000" + i,
                    VOTED_TIME.plusSeconds(i),
                    VotingType.SIMPLE_MAJORITY,
                    ProcedureType.NORMAL,
                    ResultType.ACCEPTED);
            voting.addVote("K1", VoteType.YES);
            voting.addVote("K2", VoteType.NO);
            votingRepository.save(voting);
        }
        entityManager.flush();
        entityManager.clear();
        Statistics statistics =
                entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        List<VotingEntity> votings = votingRepository.findAllWithVotesBetween(VOTED_TIME, VOTED_TIME.plusSeconds(60));
        List<String> memberIds = votings.stream()
                .flatMap(voting -> voting.getVotes().stream())
                .map(VoteEntity::getMemberId)
                .toList();

        assertThat(votings).hasSize(3);
        assertThat(memberIds).containsExactly("K1", "K2", "K1", "K2", "K1", "K2");
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Egyidejű mentésnél az egyedi megkötés csak egy szavazást enged ugyanarra az időpontra")
    void uniqueConstraintRejectsConcurrentSaveWithSameTime() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Boolean>> saves = new ArrayList<>();
        for (String publicId : List.of("CC0001", "CC0002")) {
            saves.add(() -> {
                start.await();
                try {
                    transaction.executeWithoutResult(status -> votingRepository.saveAndFlush(voting(
                            publicId, VOTED_TIME, VotingType.PRESENCE, ProcedureType.NORMAL, ResultType.ACCEPTED)));
                    return true;
                } catch (DataIntegrityViolationException expected) {
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
            transaction.executeWithoutResult(status -> votingRepository.deleteAll());
        }
    }

    private static VotingEntity voting(
            String publicId, Instant votedTime, VotingType type, ProcedureType procedure, ResultType result) {
        return VotingEntity.builder()
                .publicId(publicId)
                .votedTime(votedTime)
                .subject("Tárgy")
                .type(type)
                .procedure(procedure)
                .presidentId("K1")
                .result(result)
                .memberCount(1)
                .build();
    }
}

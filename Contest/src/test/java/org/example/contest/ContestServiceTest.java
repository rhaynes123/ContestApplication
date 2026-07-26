package org.example.contest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ContestServiceTest {

    @Autowired ContestService service;
    @Autowired ContestRepository contests;
    @Autowired GuessRepository guesses;
    @Autowired MutableClock clock;

    @TestConfiguration
    static class Config {
        @Bean @Primary
        MutableClock mutableClock() {
            return new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        }
    }

    static class MutableClock extends Clock {
        private final AtomicReference<Instant> now;
        MutableClock(Instant start) { this.now = new AtomicReference<>(start); }
        void set(Instant t) { now.set(t); }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    }

    private ContestForm form(String name, int secret, Instant deadline) {
        return new ContestForm(name, secret, deadline, "Gold", "Silver", "Bronze");
    }

    private Contest freshContest(int secret) {
        clock.set(Instant.parse("2026-01-01T00:00:00Z"));
        return service.create(form("Test", secret, Instant.parse("2026-02-01T00:00:00Z"))).value();
    }

    @Test
    void createPersistsSecretDeadlineAndPrizes() {
        Contest c = freshContest(500);
        Contest reloaded = contests.findById(c.getId()).orElseThrow();
        assertThat(reloaded.getSecretValue()).isEqualTo(500);
        assertThat(reloaded.getDeadline()).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"));
        assertThat(reloaded.getPrizes()).extracting(Prize::getPlace)
                .containsExactlyInAnyOrder(Place.FIRST, Place.SECOND, Place.THIRD);
    }

    @Test
    void submitGuessBeforeDeadlinePersists() {
        Contest c = freshContest(500);
        clock.set(Instant.parse("2026-01-15T00:00:00Z"));
        Guess g = service.submitGuess(c.getId(), new GuessForm("Alice", 400)).value();
        assertThat(g.getId()).isNotNull();
        assertThat(g.getPlayerName()).isEqualTo("Alice");
        assertThat(g.getValue()).isEqualTo(400);
        assertThat(g.getSubmittedAt()).isEqualTo(Instant.parse("2026-01-15T00:00:00Z"));
    }

    @Test
    void submitGuessAfterDeadlineReturnsClosed() {
        Contest c = freshContest(500);
        clock.set(Instant.parse("2026-02-01T00:00:01Z"));
        ServiceResult<Guess> r = service.submitGuess(c.getId(), new GuessForm("Alice", 400));
        assertThat(r.isOk()).isFalse();
        assertThat(r.errorCode()).isEqualTo("contest_closed");
        assertThat(r.errorStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void duplicateNameInSameContestReturnsError_differentContestsOk() {
        Contest a = freshContest(100);
        Contest b = freshContest(200);
        clock.set(Instant.parse("2026-01-15T00:00:00Z"));
        service.submitGuess(a.getId(), new GuessForm("Alice", 10));
        ServiceResult<Guess> dup = service.submitGuess(a.getId(), new GuessForm("Alice", 20));
        assertThat(dup.isOk()).isFalse();
        assertThat(dup.errorCode()).isEqualTo("name_already_guessed");
        assertThat(dup.errorStatus()).isEqualTo(HttpStatus.CONFLICT);
        service.submitGuess(b.getId(), new GuessForm("Alice", 30));
        assertThat(guesses.findByContest(b)).hasSize(1);
    }

    @Test
    void resolveAssignsByProximityThenSubmissionTime() {
        Contest c = freshContest(500);
        clock.set(Instant.parse("2026-01-10T00:00:00Z"));
        service.submitGuess(c.getId(), new GuessForm("Far",     100));  // diff 400
        clock.set(Instant.parse("2026-01-11T00:00:00Z"));
        service.submitGuess(c.getId(), new GuessForm("EarlyTie", 450)); // diff 50
        clock.set(Instant.parse("2026-01-12T00:00:00Z"));
        service.submitGuess(c.getId(), new GuessForm("LateTie",  550)); // diff 50
        clock.set(Instant.parse("2026-01-13T00:00:00Z"));
        service.submitGuess(c.getId(), new GuessForm("Best",     499)); // diff 1

        clock.set(Instant.parse("2026-02-02T00:00:00Z"));
        Contest resolved = service.get(c.getId()).value();

        List<Prize> ordered = resolved.getPrizes().stream()
                .sorted((x, y) -> x.getPlace().compareTo(y.getPlace())).toList();
        assertThat(ordered.get(0).getWinnerName()).isEqualTo("Best");
        assertThat(ordered.get(1).getWinnerName()).isEqualTo("EarlyTie");
        assertThat(ordered.get(2).getWinnerName()).isEqualTo("LateTie");
        assertThat(resolved.isWon()).isTrue();
        assertThat(resolved.getResolvedAt()).isNotNull();
    }

    @Test
    void resolveWithFewerGuessesThanPrizesLeavesContestNotWon() {
        Contest c = freshContest(500);
        clock.set(Instant.parse("2026-01-10T00:00:00Z"));
        service.submitGuess(c.getId(), new GuessForm("Solo", 490));

        clock.set(Instant.parse("2026-02-02T00:00:00Z"));
        Contest resolved = service.get(c.getId()).value();

        List<Prize> ordered = resolved.getPrizes().stream()
                .sorted((x, y) -> x.getPlace().compareTo(y.getPlace())).toList();
        assertThat(ordered.get(0).getWinnerName()).isEqualTo("Solo");
        assertThat(ordered.get(0).isWon()).isTrue();
        assertThat(ordered.get(1).isWon()).isFalse();
        assertThat(ordered.get(2).isWon()).isFalse();
        assertThat(resolved.isWon()).isFalse();
    }

    @Test
    void resolveIsIdempotent() {
        Contest c = freshContest(500);
        clock.set(Instant.parse("2026-01-10T00:00:00Z"));
        service.submitGuess(c.getId(), new GuessForm("Alice", 400));

        clock.set(Instant.parse("2026-02-02T00:00:00Z"));
        Contest first = service.get(c.getId()).value();
        Instant resolvedAt = first.getResolvedAt();
        String winner = first.getPrizes().stream()
                .filter(p -> p.getPlace() == Place.FIRST).findFirst().orElseThrow().getWinnerName();

        clock.set(Instant.parse("2026-02-03T00:00:00Z"));
        Contest second = service.get(c.getId()).value();
        assertThat(second.getResolvedAt()).isEqualTo(resolvedAt);
        assertThat(second.getPrizes().stream()
                .filter(p -> p.getPlace() == Place.FIRST).findFirst().orElseThrow().getWinnerName())
                .isEqualTo(winner);
    }
}

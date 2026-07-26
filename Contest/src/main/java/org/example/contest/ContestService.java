package org.example.contest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ContestService {

    private final ContestRepository contests;
    private final GuessRepository guesses;
    private final Clock clock;

    public ContestService(ContestRepository contests, GuessRepository guesses, Clock clock) {
        this.contests = contests;
        this.guesses = guesses;
        this.clock = clock;
    }

    @Transactional
    public Contest create(ContestForm form) {
        if (form.contestName() == null || form.contestName().isBlank()
                || form.secretValue() == null
                || form.deadline() == null || form.deadline().isBlank()
                || form.firstPrize() == null || form.firstPrize().isBlank()) {
            throw new IllegalArgumentException("invalid_input");
        }
        Instant deadline;
        try {
            deadline = Instant.parse(form.deadline());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("invalid_input", e);
        }
        List<Prize> prizes = new ArrayList<>(3);
        addPrize(prizes, form.firstPrize(), Place.FIRST);
        addPrize(prizes, form.secondPrize(), Place.SECOND);
        addPrize(prizes, form.thirdPrize(), Place.THIRD);
        return contests.save(new Contest(form.contestName().trim(), form.secretValue(), deadline, prizes));
    }

    @Transactional
    public List<Contest> listAll() {
        List<Contest> all = contests.findAll();
        Instant now = clock.instant();
        for (Contest c : all) {
            if (c.getResolvedAt() == null && !c.getDeadline().isAfter(now)) {
                resolveLocked(c.getId());
            }
        }
        return contests.findAll();
    }

    @Transactional
    public Contest get(long id) {
        Contest c = contests.findById(id).orElseThrow(() -> new ContestNotFoundException(id));
        if (c.getResolvedAt() == null && !c.getDeadline().isAfter(clock.instant())) {
            return resolveLocked(id);
        }
        return c;
    }

    @Transactional
    public Guess submitGuess(long contestId, GuessForm form) {
        if (form.playerName() == null || form.playerName().isBlank() || form.value() == null) {
            throw new IllegalArgumentException("invalid_input");
        }
        Contest c = contests.findById(contestId).orElseThrow(() -> new ContestNotFoundException(contestId));
        Instant now = clock.instant();
        if (!c.getDeadline().isAfter(now)) {
            throw new ContestClosedException(contestId);
        }
        String name = form.playerName().trim();
        if (guesses.existsByContestAndPlayerName(c, name)) {
            throw new DuplicateGuessException(contestId, name);
        }
        return guesses.save(new Guess(c, name, form.value(), now));
    }

    private Contest resolveLocked(long id) {
        Contest c = contests.findWithLockById(id).orElseThrow(() -> new ContestNotFoundException(id));
        if (c.getResolvedAt() != null) {
            return c;
        }
        List<Guess> ordered = guesses.findByContest(c).stream()
                .sorted(Comparator
                        .comparingInt((Guess g) -> Math.abs(g.getValue() - c.getSecretValue()))
                        .thenComparing(Guess::getSubmittedAt))
                .toList();

        List<Prize> prizes = c.getPrizes().stream()
                .sorted(Comparator.comparing(Prize::getPlace))
                .toList();

        int assigned = 0;
        for (int i = 0; i < prizes.size(); i++) {
            if (i >= ordered.size()) break;
            Prize p = prizes.get(i);
            Guess winner = ordered.get(i);
            p.setWinnerName(winner.getPlayerName());
            p.setWon(true);
            assigned++;
        }
        c.setWon(!prizes.isEmpty() && assigned == prizes.size());
        c.setResolvedAt(clock.instant());
        return contests.save(c);
    }

    private static void addPrize(List<Prize> prizes, String value, Place place) {
        if (value != null && !value.isBlank()) {
            prizes.add(new Prize(value.trim(), place));
        }
    }
}

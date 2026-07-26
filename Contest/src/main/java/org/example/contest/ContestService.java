package org.example.contest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
    public ServiceResult<Contest> create(ContestForm form) {
        if (form.contestName() == null || form.contestName().isBlank()
                || form.secretValue() == null
                || form.deadline() == null
                || form.firstPrize() == null || form.firstPrize().isBlank()) {
            return ServiceResult.err(HttpStatus.BAD_REQUEST, "invalid_input");
        }
        List<Prize> prizes = new ArrayList<>(3);
        addPrize(prizes, form.firstPrize(), Place.FIRST);
        addPrize(prizes, form.secondPrize(), Place.SECOND);
        addPrize(prizes, form.thirdPrize(), Place.THIRD);
        return ServiceResult.ok(contests.save(new Contest(form.contestName().trim(), form.secretValue(), form.deadline(), prizes)));
    }

    @Transactional(readOnly = true)
    public List<Contest> listAll() {
        return contests.findAll();
    }

    @Transactional
    public ServiceResult<Contest> get(long id) {
        Optional<Contest> found = contests.findById(id);
        if (found.isEmpty()) {
            return ServiceResult.err(HttpStatus.NOT_FOUND, "not_found");
        }
        Contest c = found.get();
        if (c.getResolvedAt() == null && !c.getDeadline().isAfter(clock.instant())) {
            return ServiceResult.ok(resolveLocked(id));
        }
        return ServiceResult.ok(c);
    }

    @Transactional
    public ServiceResult<Guess> submitGuess(long contestId, GuessForm form) {
        if (form.playerName() == null || form.playerName().isBlank() || form.value() == null) {
            return ServiceResult.err(HttpStatus.BAD_REQUEST, "invalid_input");
        }
        Optional<Contest> found = contests.findById(contestId);
        if (found.isEmpty()) {
            return ServiceResult.err(HttpStatus.NOT_FOUND, "not_found");
        }
        Contest c = found.get();
        Instant now = clock.instant();
        if (!c.getDeadline().isAfter(now)) {
            return ServiceResult.err(HttpStatus.CONFLICT, "contest_closed");
        }
        String name = form.playerName().trim();
        if (guesses.existsByContestAndPlayerName(c, name)) {
            return ServiceResult.err(HttpStatus.CONFLICT, "name_already_guessed");
        }
        return ServiceResult.ok(guesses.save(new Guess(c, name, form.value(), now)));
    }

    private Contest resolveLocked(long id) {
        Contest c = contests.findWithLockById(id).orElseThrow();
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

        for (int i = 0; i < prizes.size() && i < ordered.size(); i++) {
            Prize p = prizes.get(i);
            p.setWinnerName(ordered.get(i).getPlayerName());
            p.setWon(true);
        }
        c.setWon(!prizes.isEmpty() && ordered.size() >= prizes.size());
        c.setResolvedAt(clock.instant());
        return contests.save(c);
    }

    private static void addPrize(List<Prize> prizes, String value, Place place) {
        if (value != null && !value.isBlank()) {
            prizes.add(new Prize(value.trim(), place));
        }
    }
}

package org.example.contest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuessRepository extends JpaRepository<Guess, Long> {

    List<Guess> findByContest(Contest contest);

    boolean existsByContestAndPlayerName(Contest contest, String playerName);
}

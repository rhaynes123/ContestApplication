package org.example.contest;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContestService {

    private final ContestRepository repository;

    public ContestService(ContestRepository repository) {
        this.repository = repository;
    }

    public List<Contest> all() {
        return repository.findAll();
    }

    public Contest create(ContestForm form) {
        List<Prize> prizes = new ArrayList<>(3);
        addPrize(prizes, form.firstPrize(), Place.FIRST);
        addPrize(prizes, form.secondPrize(), Place.SECOND);
        addPrize(prizes, form.thirdPrize(), Place.THIRD);
        return repository.save(new Contest(form.contestName(), prizes));
    }

    private static void addPrize(List<Prize> prizes, String value, Place place) {
        if (value != null && !value.isBlank()) {
            prizes.add(new Prize(value.trim(), place));
        }
    }
}

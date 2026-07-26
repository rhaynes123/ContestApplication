package org.example.contest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/contests")
public class ContestController {

    private final ContestService contests;

    public ContestController(ContestService contests) {
        this.contests = contests;
    }

    @GetMapping
    public List<ContestDto> list() {
        return contests.listAll().stream().map(ContestDto::of).toList();
    }

    @GetMapping("/{id}")
    public ContestDto get(@PathVariable long id) {
        return ContestDto.of(contests.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContestDto create(@ModelAttribute ContestForm form) {
        return ContestDto.of(contests.create(form));
    }

    @PostMapping("/{id}/guesses")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Boolean> guess(@PathVariable long id, @ModelAttribute GuessForm form) {
        contests.submitGuess(id, form);
        return Map.of("accepted", true);
    }
}

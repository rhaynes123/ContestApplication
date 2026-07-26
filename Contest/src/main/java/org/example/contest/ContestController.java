package org.example.contest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ResponseEntity<Object> get(@PathVariable long id) {
        ServiceResult<Contest> r = contests.get(id);
        return r.isOk() ? ResponseEntity.ok(ContestDto.of(r.value())) : errorBody(r);
    }

    @PostMapping
    public ResponseEntity<Object> create(@ModelAttribute ContestForm form) {
        ServiceResult<Contest> r = contests.create(form);
        return r.isOk()
                ? ResponseEntity.status(HttpStatus.CREATED).body(ContestDto.of(r.value()))
                : errorBody(r);
    }

    @PostMapping("/{id}/guesses")
    public ResponseEntity<Object> guess(@PathVariable long id, @ModelAttribute GuessForm form) {
        ServiceResult<Guess> r = contests.submitGuess(id, form);
        return r.isOk()
                ? ResponseEntity.status(HttpStatus.CREATED).body(Map.of("accepted", true))
                : errorBody(r);
    }

    private static ResponseEntity<Object> errorBody(ServiceResult<?> r) {
        return ResponseEntity.status(r.errorStatus()).body(Map.of("error", r.errorCode()));
    }
}

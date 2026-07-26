package org.example.contest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/contests")
public class ContestController {

    private final ContestService contests;

    public ContestController(ContestService contests) {
        this.contests = contests;
    }

    @GetMapping
    public List<Contest> list() {
        return contests.allActive();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Contest create(@ModelAttribute ContestForm form) {
        return contests.create(form);
    }
}

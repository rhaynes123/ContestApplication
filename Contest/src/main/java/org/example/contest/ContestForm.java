package org.example.contest;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

public record ContestForm(
        String contestName,
        Integer secretValue,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant deadline,
        String firstPrize,
        String secondPrize,
        String thirdPrize
) { }

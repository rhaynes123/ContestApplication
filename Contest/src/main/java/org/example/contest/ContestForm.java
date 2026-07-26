package org.example.contest;

public record ContestForm(
        String contestName,
        String contestSecret,
        String firstPrize,
        String secondPrize,
        String thirdPrize
) { }

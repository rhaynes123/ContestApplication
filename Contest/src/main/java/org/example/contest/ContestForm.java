package org.example.contest;

public record ContestForm(
        String contestName,
        Integer secretValue,
        String deadline,
        String firstPrize,
        String secondPrize,
        String thirdPrize
) { }

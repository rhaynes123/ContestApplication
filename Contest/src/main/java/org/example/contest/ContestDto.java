package org.example.contest;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

public record ContestDto(
        Long id,
        String name,
        Instant deadline,
        boolean isWon,
        List<PrizeDto> prizes,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer secretValue
) {
    public static ContestDto of(Contest c) {
        boolean closed = c.getResolvedAt() != null;
        List<PrizeDto> prizes = c.getPrizes().stream().map(PrizeDto::of).toList();
        return new ContestDto(
                c.getId(),
                c.getName(),
                c.getDeadline(),
                c.isWon(),
                prizes,
                closed ? c.getSecretValue() : null
        );
    }

    public record PrizeDto(Place place, String value, boolean isWon, String winnerName) {
        static PrizeDto of(Prize p) {
            return new PrizeDto(p.getPlace(), p.getValue(), p.isWon(), p.getWinnerName());
        }
    }
}

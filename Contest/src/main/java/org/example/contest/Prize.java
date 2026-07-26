package org.example.contest;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Prize {

    @Column(name = "prize_value", nullable = false)
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Place place;

    @Column(nullable = false)
    private boolean isWon;

    @Column
    private String winnerName;

    public Prize(String value, Place place) {
        this.value = value;
        this.place = place;
    }
}

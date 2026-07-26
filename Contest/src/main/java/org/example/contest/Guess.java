package org.example.contest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "guess",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_guess_contest_player",
                columnNames = {"contest_id", "player_name"}
        )
)
public class Guess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;

    @Column(name = "player_name", nullable = false)
    private String playerName;

    @Column(name = "guess_value", nullable = false)
    private int value;

    @Column(nullable = false)
    private Instant submittedAt;

    public Guess(Contest contest, String playerName, int value, Instant submittedAt) {
        this.contest = contest;
        this.playerName = playerName;
        this.value = value;
        this.submittedAt = submittedAt;
    }
}

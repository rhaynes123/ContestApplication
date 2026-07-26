package org.example.contest;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Contest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int secretValue;

    @Column(nullable = false)
    private Instant deadline;

    @Column(nullable = false)
    private boolean isWon;

    @Column
    private Instant resolvedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "contest_prizes", joinColumns = @JoinColumn(name = "contest_id"))
    private List<Prize> prizes = new ArrayList<>();

    public Contest(String name, int secretValue, Instant deadline, List<Prize> prizes) {
        this.name = name;
        this.secretValue = secretValue;
        this.deadline = deadline;
        this.prizes = new ArrayList<>(prizes);
    }
}

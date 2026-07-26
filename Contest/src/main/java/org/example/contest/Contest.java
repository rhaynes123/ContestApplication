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
    private String secret;
    @Column
    private Boolean isWon;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "contest_prizes", joinColumns = @JoinColumn(name = "contest_id"))
    private List<Prize> prizes = new ArrayList<>();

    public Contest(String name, List<Prize> prizes, String secret) {
        this.name = name;
        this.prizes = new ArrayList<>(prizes);
        this.secret = secret;
    }
}

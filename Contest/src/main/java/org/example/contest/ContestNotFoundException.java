package org.example.contest;

public class ContestNotFoundException extends RuntimeException {
    public ContestNotFoundException(long id) {
        super("contest " + id + " not found");
    }
}

package org.example.contest;

public class ContestClosedException extends RuntimeException {
    public ContestClosedException(long contestId) {
        super("contest " + contestId + " is closed");
    }
}

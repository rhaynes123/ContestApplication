package org.example.contest;

public class DuplicateGuessException extends RuntimeException {
    public DuplicateGuessException(long contestId, String playerName) {
        super("player " + playerName + " already guessed in contest " + contestId);
    }
}

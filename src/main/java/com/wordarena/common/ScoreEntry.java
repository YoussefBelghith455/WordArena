
package com.wordarena.common;

import java.io.Serializable;

public class ScoreEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private String playerId;
    private String username;
    private int score;
    private int rank;

    public ScoreEntry() {
    }

    public ScoreEntry(String playerId, String username,
                      int score, int rank) {
        this.playerId = playerId;
        this.username = username;
        this.score = score;
        this.rank = rank;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public int getScore() {
        return score;
    }

    public int getRank() {
        return rank;
    }
}


package com.wordarena.common;

import java.io.Serializable;

public class PlayerView implements Serializable {

    private static final long serialVersionUID = 1L;

    private String playerId;
    private String username;
    private boolean ready;
    private int attemptsUsed;
    private int score;
    private boolean solved;

    public PlayerView() {
    }

    public PlayerView(String playerId, String username,
                      boolean ready, int attemptsUsed,
                      int score, boolean solved) {
        this.playerId = playerId;
        this.username = username;
        this.ready = ready;
        this.attemptsUsed = attemptsUsed;
        this.score = score;
        this.solved = solved;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public boolean isReady() {
        return ready;
    }

    public int getAttemptsUsed() {
        return attemptsUsed;
    }

    public int getScore() {
        return score;
    }

    public boolean isSolved() {
        return solved;
    }
}


package com.wordarena.common;

import java.io.Serializable;

public class PlayerStats implements Serializable {

    private static final long serialVersionUID = 1L;

    private String playerId;
    private String username;
    private int gamesPlayed;
    private int gamesWon;
    private int gamesLost;
    private int totalScore;

    public PlayerStats() {
    }

    public PlayerStats(String playerId, String username,
                       int gamesPlayed, int gamesWon,
                       int gamesLost, int totalScore) {
        this.playerId = playerId;
        this.username = username;
        this.gamesPlayed = gamesPlayed;
        this.gamesWon = gamesWon;
        this.gamesLost = gamesLost;
        this.totalScore = totalScore;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public int getGamesLost() {
        return gamesLost;
    }

    public int getTotalScore() {
        return totalScore;
    }
}

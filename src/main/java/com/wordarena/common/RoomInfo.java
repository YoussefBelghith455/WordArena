
package com.wordarena.common;

import java.io.Serializable;

public class RoomInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String roomId;
    private String roomName;
    private GameMode gameMode;
    private int currentPlayers;
    private int maxPlayers;
    private boolean started;

    public RoomInfo() {
    }

    public RoomInfo(String roomId, String roomName,
                    GameMode gameMode, int currentPlayers,
                    int maxPlayers, boolean started) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.gameMode = gameMode;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.started = started;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean isStarted() {
        return started;
    }
}

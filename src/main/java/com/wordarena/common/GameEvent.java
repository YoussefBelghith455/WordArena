
package com.wordarena.common;

import java.io.Serializable;

public class GameEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private EventType type;
    private String roomId;
    private String playerId;
    private String message;
    private long timestamp;

    public GameEvent() {
    }

    public GameEvent(EventType type, String roomId,
                     String playerId, String message) {
        this.type = type;
        this.roomId = roomId;
        this.playerId = playerId;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public EventType getType() {
        return type;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}


package com.wordarena.common;

import java.io.Serializable;

public class GameMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;
    private String playerId;
    private String roomId;
    private String content;

    public GameMessage() {
    }

    public GameMessage(String type, String playerId,
                       String roomId, String content) {
        this.type = type;
        this.playerId = playerId;
        this.roomId = roomId;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getContent() {
        return content;
    }
}

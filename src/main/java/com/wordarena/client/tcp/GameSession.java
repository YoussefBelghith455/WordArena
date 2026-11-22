
package com.wordarena.client.tcp;

import com.wordarena.common.GameMessage;
import javafx.application.Platform;

import java.io.IOException;
import java.util.function.Consumer;

public class GameSession {

    private final GameTcpClient tcpClient;
    private final String playerId;
    private final String username;

    private String currentRoomId;
    private Consumer<GameMessage> messageHandler;
    private Consumer<Exception> errorHandler;

    public GameSession(
            GameTcpClient tcpClient,
            String playerId,
            String username
    ) {
        this.tcpClient = tcpClient;
        this.playerId = playerId;
        this.username = username;

        tcpClient.setMessageListener(message ->
                Platform.runLater(() -> handleMessage(message))
        );

        tcpClient.setErrorListener(exception ->
                Platform.runLater(() -> {

                    if (errorHandler != null) {
                        errorHandler.accept(exception);
                    } else {
                        System.err.println(
                                "TCP connection error: "
                                        + exception.getMessage()
                        );
                    }
                })
        );
    }

    private void handleMessage(GameMessage message) {

        if ("ROOM_STATE".equals(message.getType())) {

            String roomId = message.getRoomId();

            if (roomId != null && !roomId.isBlank()) {

                if (currentRoomId == null
                        || currentRoomId.equals(roomId)) {
                    currentRoomId = roomId;
                }
            }
        }

        if (messageHandler != null) {
            messageHandler.accept(message);
        }
    }

    public void send(
            String type,
            String roomId,
            String content
    ) throws IOException {

        tcpClient.send(
                new GameMessage(
                        type,
                        playerId,
                        roomId == null ? "" : roomId,
                        content == null ? "" : content
                )
        );
    }

    public void setMessageHandler(
            Consumer<GameMessage> handler
    ) {
        this.messageHandler = handler;
    }

    public void setErrorHandler(
            Consumer<Exception> handler
    ) {
        this.errorHandler = handler;
    }

    public GameTcpClient getTcpClient() {
        return tcpClient;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public String getCurrentRoomId() {
        return currentRoomId;
    }

    public void clearRoom() {
        currentRoomId = null;
    }
}

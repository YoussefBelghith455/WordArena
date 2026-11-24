package com.wordarena.client.tcp;

import com.wordarena.client.jms.GameJmsClient;
import com.wordarena.client.rmi.GameRmiClient;
import com.wordarena.common.GameEvent;
import com.wordarena.common.GameMessage;
import com.wordarena.common.PlayerStats;
import com.wordarena.common.PlayerView;
import com.wordarena.common.RoomInfo;
import com.wordarena.common.ScoreEntry;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class GameSession {

    private static final int MAX_JMS_EVENTS = 50;

    private final GameTcpClient tcpClient;
    private final GameJmsClient jmsClient;
    private final GameRmiClient rmiClient;
    private final String playerId;
    private final String username;

    private final ObservableList<GameEvent> jmsEvents =
            FXCollections.observableArrayList();

    private final ObservableList<GameEvent> readOnlyJmsEvents =
            FXCollections.unmodifiableObservableList(jmsEvents);

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

        this.jmsClient = new GameJmsClient(
                this::handleJmsEvent
        );

        this.rmiClient = new GameRmiClient();

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

    private void handleJmsEvent(GameEvent event) {

        if (event == null || currentRoomId == null) {
            return;
        }

        if (!currentRoomId.equals(event.getRoomId())) {
            return;
        }

        jmsEvents.add(event);

        while (jmsEvents.size() > MAX_JMS_EVENTS) {
            jmsEvents.remove(0);
        }
    }

    public void startJms() {
        jmsClient.start();
    }

    public void closeJms() {
        jmsClient.close();
    }

    public ReadOnlyStringProperty jmsStatusProperty() {
        return jmsClient.statusProperty();
    }

    public String getJmsStatus() {
        return jmsClient.getStatus();
    }

    public boolean isJmsConnected() {
        return jmsClient.isConnected();
    }

    public ObservableList<GameEvent> getJmsEvents() {
        return readOnlyJmsEvents;
    }

    public void startRmi() {
        rmiClient.start();
    }

    public void closeRmi() {
        rmiClient.close();
    }

    public ReadOnlyStringProperty rmiStatusProperty() {
        return rmiClient.statusProperty();
    }

    public String getRmiStatus() {
        return rmiClient.getStatus();
    }

    public boolean isRmiConnected() {
        return rmiClient.isConnected();
    }

    public ObservableList<RoomInfo> getAvailableRooms() {
        return rmiClient.getAvailableRooms();
    }

    public void refreshRooms() {
        rmiClient.refreshRooms();
    }

    public CompletableFuture<RoomInfo> getRoomInfo(
            String roomId
    ) {
        return rmiClient.getRoomInfo(roomId);
    }

    public CompletableFuture<List<PlayerView>> getRoomPlayers(
            String roomId
    ) {
        return rmiClient.getRoomPlayers(roomId);
    }

    public CompletableFuture<List<ScoreEntry>> getLeaderboard() {
        return rmiClient.getLeaderboard();
    }

    public CompletableFuture<PlayerStats> getPlayerStats(
            String playerId
    ) {
        return rmiClient.getPlayerStats(playerId);
    }

    public CompletableFuture<PlayerStats> getMyStats() {
        return rmiClient.getPlayerStats(playerId);
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
        jmsEvents.clear();
    }
}
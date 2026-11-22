
package com.wordarena.gui;

import com.wordarena.client.tcp.GameSession;
import com.wordarena.common.GameMessage;
import com.wordarena.common.GameMode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class JoinRoomView extends VBox {

    private final String username;
    private final GameSession gameSession;

    private final Label messageLabel = new Label();
    private final TextField roomIdField = new TextField();
    private final Button joinButton = new Button("JOIN ROOM");
    private final Button backButton = new Button("BACK TO LOBBY");

    private boolean joiningRoom;

    public JoinRoomView(String username) {
        this(username, null);
    }

    public JoinRoomView(
            String username,
            GameSession gameSession
    ) {
        this.username = username;
        this.gameSession = gameSession;

        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(40));
        getStyleClass().add("join-room-root");

        Label title = new Label("JOIN ROOM");
        title.getStyleClass().add("lobby-title");

        Label subtitle = new Label("ENTER THE BATTLE ARENA");
        subtitle.getStyleClass().add("lobby-subtitle");

        Label welcome = new Label("Player: " + username);
        welcome.getStyleClass().add("lobby-welcome");

        roomIdField.setPromptText("Enter room ID");
        roomIdField.setPrefWidth(300);
        roomIdField.setMaxWidth(300);

        joinButton.getStyleClass().add("neon-button");
        joinButton.setPrefWidth(300);

        backButton.getStyleClass().add("secondary-button");
        backButton.setPrefWidth(300);

        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("room-message");

        VBox content = new VBox(
                20,
                title,
                subtitle,
                welcome,
                roomIdField,
                joinButton,
                messageLabel,
                backButton
        );

        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(35));
        content.setMaxWidth(450);
        content.getStyleClass().add("join-room-panel");

        getChildren().add(content);

        joinButton.setOnAction(event -> joinRoom());

        backButton.setOnAction(event -> returnToLobby());
    }

    private void joinRoom() {

        if (joiningRoom) {
            return;
        }

        String roomId = roomIdField.getText().trim();

        if (roomId.isEmpty()) {
            messageLabel.setText("Please enter a room ID.");
            return;
        }

        if (!isConnected()) {
            messageLabel.setText(
                    "No authenticated server connection."
            );
            return;
        }

        joiningRoom = true;
        setControlsDisabled(true);

        messageLabel.setText(
                "Joining room " + roomId + "..."
        );

        gameSession.setMessageHandler(response ->
                handleServerResponse(response, roomId)
        );

        gameSession.setErrorHandler(exception -> {
            joiningRoom = false;
            setControlsDisabled(true);

            messageLabel.setText(
                    "Connection error: " + exception.getMessage()
            );
        });

        Thread joinThread = new Thread(() -> {
            try {
                gameSession.send(
                        "JOIN_ROOM",
                        roomId,
                        ""
                );
            } catch (IOException exception) {
                Platform.runLater(() -> {
                    resetControls();

                    messageLabel.setText(
                            "Unable to join room: "
                                    + exception.getMessage()
                    );

                    if (!isConnected()) {
                        setControlsDisabled(true);
                    }
                });
            }
        });

        joinThread.setName("wordarena-join-room");
        joinThread.setDaemon(true);
        joinThread.start();
    }

    private void handleServerResponse(
            GameMessage response,
            String requestedRoomId
    ) {

        if (!joiningRoom) {
            return;
        }

        if ("ERROR".equals(response.getType())) {
            resetControls();

            messageLabel.setText(
                    "Unable to join room: "
                            + response.getContent()
            );
            return;
        }

        if (!"ROOM_STATE".equals(response.getType())) {
            return;
        }

        if (!requestedRoomId.equals(response.getRoomId())) {
            return;
        }

        String content = response.getContent();

        if (content == null) {
            resetControls();
            messageLabel.setText(
                    "Invalid room state from server."
            );
            return;
        }

        String[] fields = content.split("\\|", -1);

        if (fields.length != 6) {
            resetControls();
            messageLabel.setText(
                    "Invalid room state from server."
            );
            return;
        }

        GameMode mode;

        try {
            mode = GameMode.valueOf(fields[1]);
        } catch (IllegalArgumentException exception) {
            resetControls();
            messageLabel.setText(
                    "Unknown game mode from server."
            );
            return;
        }

        String roomName = fields[0];

        boolean isHost =
                gameSession.getPlayerId().equals(fields[5]);

        joiningRoom = false;

        WaitingRoomView waitingRoom = new WaitingRoomView(
                username,
                roomName,
                mode,
                isHost,
                gameSession,
                requestedRoomId
        );

        Scene scene = new Scene(waitingRoom, 900, 700);
        applyTheme(scene);

        Stage stage = (Stage) getScene().getWindow();

        stage.setScene(scene);
        stage.setTitle("WordArena - Waiting Room");

        waitingRoom.updateStatus(
                "Joined room " + requestedRoomId
        );
    }

    private boolean isConnected() {
        return gameSession != null
                && gameSession.getTcpClient().isConnected();
    }

    private void setControlsDisabled(boolean disabled) {
        joinButton.setDisable(disabled);
        roomIdField.setDisable(disabled);
        backButton.setDisable(disabled);
    }

    private void resetControls() {
        joiningRoom = false;
        setControlsDisabled(false);
    }

    private void returnToLobby() {

        if (joiningRoom) {
            return;
        }

        if (gameSession != null) {
            gameSession.setMessageHandler(null);
            gameSession.setErrorHandler(null);
        }

        LobbyView lobby = new LobbyView(
                username,
                gameSession
        );

        Scene scene = new Scene(lobby, 800, 600);
        applyTheme(scene);

        Stage stage = (Stage) getScene().getWindow();

        stage.setScene(scene);
        stage.setTitle("WordArena - Lobby");
    }

    private void applyTheme(Scene scene) {
        scene.getStylesheets().add(
                getClass()
                        .getResource("/css/wordarena.css")
                        .toExternalForm()
        );
    }
}

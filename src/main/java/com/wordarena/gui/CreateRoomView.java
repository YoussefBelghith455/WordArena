
package com.wordarena.gui;

import com.wordarena.client.tcp.GameSession;
import com.wordarena.common.GameMessage;
import com.wordarena.common.GameMode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class CreateRoomView extends VBox {

    private final String username;
    private final GameSession gameSession;

    private final Label messageLabel = new Label();
    private final Button createButton = new Button("CREATE ROOM");
    private final Button backButton = new Button("BACK TO LOBBY");
    private final ComboBox<GameMode> modeSelector = new ComboBox<>();

    private boolean creatingRoom;

    public CreateRoomView(String username) {
        this(username, null);
    }

    public CreateRoomView(
            String username,
            GameSession gameSession
    ) {
        this.username = username;
        this.gameSession = gameSession;

        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(40));
        getStyleClass().add("create-room-root");

        Label title = new Label("CREATE ROOM");
        title.getStyleClass().add("lobby-title");

        Label subtitle = new Label("CHOOSE YOUR BATTLE MODE");
        subtitle.getStyleClass().add("lobby-subtitle");

        Label welcome = new Label("Player: " + username);
        welcome.getStyleClass().add("lobby-welcome");

        modeSelector.getItems().add(GameMode.DUEL);
        modeSelector.setValue(GameMode.DUEL);
        modeSelector.setPrefWidth(300);

        createButton.getStyleClass().add("neon-button");
        createButton.setPrefWidth(300);

        backButton.getStyleClass().add("secondary-button");
        backButton.setPrefWidth(300);

        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("room-message");

        VBox content = new VBox(
                20,
                title,
                subtitle,
                welcome,
                modeSelector,
                createButton,
                messageLabel,
                backButton
        );

        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(35));
        content.setMaxWidth(450);
        content.getStyleClass().add("create-room-panel");

        getChildren().add(content);

        createButton.setOnAction(event -> createRoom());

        backButton.setOnAction(event -> returnToLobby());
    }

    private void createRoom() {

        if (creatingRoom) {
            return;
        }

        GameMode mode = modeSelector.getValue();

        if (mode == null) {
            messageLabel.setText("Please select a game mode.");
            return;
        }

        if (!isConnected()) {
            messageLabel.setText(
                    "No authenticated server connection."
            );
            return;
        }

        String roomName = username + "'s Room";
        int maxPlayers = 2;

        String requestContent =
                roomName + "|" + mode.name() + "|" + maxPlayers;

        creatingRoom = true;
        setControlsDisabled(true);

        messageLabel.setText("Creating room on server...");

        gameSession.setMessageHandler(response ->
                handleServerResponse(
                        response,
                        roomName,
                        mode
                )
        );

        gameSession.setErrorHandler(exception -> {
            resetControls();

            messageLabel.setText(
                    "Connection error: " + exception.getMessage()
            );

            setControlsDisabled(true);
        });

        Thread requestThread = new Thread(() -> {
            try {
                gameSession.send(
                        "CREATE_ROOM",
                        "",
                        requestContent
                );
            } catch (IOException exception) {
                Platform.runLater(() -> {
                    resetControls();

                    messageLabel.setText(
                            "Unable to create room: "
                                    + exception.getMessage()
                    );

                    if (!isConnected()) {
                        setControlsDisabled(true);
                    }
                });
            }
        });

        requestThread.setName("wordarena-create-room");
        requestThread.setDaemon(true);
        requestThread.start();
    }

    private void handleServerResponse(
            GameMessage response,
            String requestedRoomName,
            GameMode requestedMode
    ) {

        if (!creatingRoom) {
            return;
        }

        if ("ERROR".equals(response.getType())) {
            resetControls();

            messageLabel.setText(
                    "Room creation failed: " + response.getContent()
            );
            return;
        }

        if (!"ROOM_STATE".equals(response.getType())) {
            return;
        }

        String roomId = response.getRoomId();

        if (roomId == null || roomId.isBlank()) {
            resetControls();
            messageLabel.setText(
                    "Server returned an invalid room ID."
            );
            return;
        }

        String content = response.getContent();

        if (content == null) {
            resetControls();
            messageLabel.setText(
                    "Server returned an invalid room state."
            );
            return;
        }

        String[] fields = content.split("\\|", -1);

        if (fields.length != 6) {
            resetControls();
            messageLabel.setText(
                    "Server returned an invalid room state."
            );
            return;
        }

        GameMode serverMode;

        try {
            serverMode = GameMode.valueOf(fields[1]);
        } catch (IllegalArgumentException exception) {
            resetControls();
            messageLabel.setText(
                    "Server returned an unknown game mode."
            );
            return;
        }

        if (!fields[0].equals(requestedRoomName)
                || serverMode != requestedMode
                || !gameSession.getPlayerId().equals(fields[5])) {
            return;
        }

        creatingRoom = false;

        WaitingRoomView waitingRoom = new WaitingRoomView(
                username,
                fields[0],
                serverMode,
                true,
                gameSession,
                roomId
        );

        Scene scene = new Scene(waitingRoom, 900, 700);
        applyTheme(scene);

        Stage stage = (Stage) getScene().getWindow();

        stage.setScene(scene);
        stage.setTitle("WordArena - Waiting Room");

        waitingRoom.updateStatus(
                "Room created. Room ID: " + roomId
        );
    }

    private boolean isConnected() {
        return gameSession != null
                && gameSession.getTcpClient().isConnected();
    }

    private void setControlsDisabled(boolean disabled) {
        createButton.setDisable(disabled);
        modeSelector.setDisable(disabled);
        backButton.setDisable(disabled);
    }

    private void resetControls() {
        creatingRoom = false;
        setControlsDisabled(false);
    }

    private void returnToLobby() {

        if (creatingRoom) {
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


package com.wordarena.gui;

import com.wordarena.common.GameMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class WaitingRoomView extends VBox {

    private final VBox playersList = new VBox(12);
    private final Label statusLabel = new Label();
    private final Button startButton = new Button("START GAME");
    private final Button chooseWordButton = new Button("CHOOSE SECRET WORD");

    private final String username;
    private final boolean isHost;
    private final String roomName;
    private final GameMode gameMode;

    private String opponentUsername = "Waiting for player...";
    private boolean wordLocked = false;
    private int playerCount = 1;

    public WaitingRoomView(
            String username,
            String roomName,
            GameMode gameMode,
            boolean isHost
    ) {

        this.username = username;
        this.isHost = isHost;
        this.roomName = roomName;
        this.gameMode = gameMode;

        getStyleClass().add("waiting-room-root");
        setSpacing(24);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(35));

        Label title = new Label("WAITING ROOM");
        title.getStyleClass().add("lobby-title");

        Label roomLabel = new Label(roomName);
        roomLabel.getStyleClass().add("waiting-room-name");

        Label modeLabel = new Label("MODE: " + gameMode);
        modeLabel.getStyleClass().add("waiting-room-mode");

        VBox roomInfo = new VBox(10, roomLabel, modeLabel);
        roomInfo.setAlignment(Pos.CENTER);

        Label playersTitle = new Label("PLAYERS");
        playersTitle.getStyleClass().add("waiting-section-title");

        playersList.setAlignment(Pos.CENTER_LEFT);

        VBox playersBox = new VBox(15, playersTitle, playersList);
        playersBox.getStyleClass().add("waiting-players-box");
        playersBox.setAlignment(Pos.CENTER_LEFT);
        playersBox.setMaxWidth(420);
        playersBox.setPrefWidth(420);

        statusLabel.getStyleClass().add("waiting-status");
        statusLabel.setWrapText(true);

        startButton.getStyleClass().add("neon-button");
        startButton.setPrefWidth(200);
        startButton.setVisible(isHost);
        startButton.setManaged(isHost);
        startButton.setDisable(true);

        chooseWordButton.getStyleClass().add("neon-button");
        chooseWordButton.setPrefWidth(220);

        boolean doubleHost = gameMode == GameMode.DOUBLE_HOST;

        chooseWordButton.setVisible(doubleHost);
        chooseWordButton.setManaged(doubleHost);

        Button leaveButton = new Button("LEAVE ROOM");
        leaveButton.getStyleClass().add("secondary-button");
        leaveButton.setPrefWidth(200);

        HBox buttons = new HBox(
                15,
                startButton,
                chooseWordButton,
                leaveButton
        );
        buttons.setAlignment(Pos.CENTER);

        getChildren().addAll(
                title,
                roomInfo,
                playersBox,
                statusLabel,
                buttons
        );

        updatePlayers(List.of(username), isHost ? username : "");
        updateStatus("Waiting for players to join...");

        chooseWordButton.setOnAction(event -> {

            Stage stage = (Stage) chooseWordButton.getScene().getWindow();

            Scene waitingScene = chooseWordButton.getScene();

            DoubleHostWordView wordView = new DoubleHostWordView(secretWord -> {

                wordLocked = true;

                chooseWordButton.setDisable(true);
                chooseWordButton.setText("WORD LOCKED");

                updateStatus(
                        "Your secret word is locked. Waiting for the other player..."
                );

                updateStartButton();

                stage.setScene(waitingScene);
                stage.setTitle("WordArena - Waiting Room");
            });

            VBox root = new VBox(wordView);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(30));
            root.getStyleClass().add("create-room-root");

            Scene wordScene = new Scene(root, 900, 700);
            applyTheme(wordScene);

            stage.setScene(wordScene);
            stage.setTitle("WordArena - Double Host Word");
        });

        startButton.setOnAction(event -> {

            if (gameMode == GameMode.DOUBLE_HOST && !wordLocked) {
                updateStatus("Choose and lock your secret word first.");
                return;
            }

            GameView gameView = new GameView(
                    username,
                    opponentUsername,
                    roomName,
                    gameMode
            );

            ScrollPane scrollPane = new ScrollPane(gameView);
            scrollPane.setFitToWidth(true);

            StackPane overlay = new StackPane(scrollPane);

            Scene scene = new Scene(overlay, 1100, 800);
            applyTheme(scene);

            Stage stage = (Stage) startButton.getScene().getWindow();

            stage.setScene(scene);
            stage.setTitle("WordArena - Game");
        });

        leaveButton.setOnAction(event -> {

            LobbyView lobby = new LobbyView(username);

            Scene scene = new Scene(lobby, 800, 600);
            applyTheme(scene);

            Stage stage = (Stage) leaveButton.getScene().getWindow();

            stage.setScene(scene);
            stage.setTitle("WordArena - Lobby");
        });
    }

    public void updatePlayers(List<String> players, String hostUsername) {

        playerCount = players.size();
        playersList.getChildren().clear();

        opponentUsername = players.stream()
                .filter(player -> !player.equals(username))
                .findFirst()
                .orElse("Waiting for player...");

        for (String player : players) {

            boolean playerIsHost = player.equals(hostUsername);

            Label playerLabel = new Label(
                    "●  " + player + (playerIsHost ? " (HOST)" : "")
            );

            playerLabel.getStyleClass().add("waiting-player");
            playersList.getChildren().add(playerLabel);
        }

        if (players.size() < 2) {

            Label emptySlot = new Label(
                    "○  Waiting for another player..."
            );

            emptySlot.getStyleClass().add("waiting-player-empty");
            playersList.getChildren().add(emptySlot);
        }

        updateStartButton();
    }

    private void updateStartButton() {

        boolean canStart = isHost && playerCount >= 2;

        if (gameMode == GameMode.DOUBLE_HOST) {
            canStart = canStart && wordLocked;
        }

        startButton.setDisable(!canStart);
    }

    public void updateStatus(String status) {
        statusLabel.setText(status);
    }

    private void applyTheme(Scene scene) {
        scene.getStylesheets().add(
                getClass()
                        .getResource("/css/wordarena.css")
                        .toExternalForm()
        );
    }
}

package com.wordarena.gui;

import com.wordarena.client.tcp.GameSession;
import com.wordarena.client.tcp.HostGameController;
import com.wordarena.client.tcp.DoubleHostGameController;
import com.wordarena.client.tcp.CooperativeGameController;
import com.wordarena.common.GameMessage;
import com.wordarena.common.GameMode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WaitingRoomView extends VBox {

    private final VBox playersList = new VBox(12);
    private final Label statusLabel = new Label();
    private final Button startButton = new Button("START GAME");
    private final Button readyButton = new Button("READY");
    private final Button leaveButton = new Button("LEAVE ROOM");
    private final Button chooseWordButton = new Button("CHOOSE SECRET WORD");

    private final String username;
    private final boolean isHost;
    private final String roomName;
    private final GameMode gameMode;
    private final GameSession gameSession;
    private final String roomId;

    private String opponentUsername = "Waiting for player...";
    private String hostPlayerId;
    private boolean wordLocked;
    private HostWordView hostWordView;
    private DoubleHostWordView doubleHostWordView;
    private boolean bothWordsLocked;
    private List<Node> waitingContent;
    private boolean ready;
    private boolean readyRequestPending;
    private boolean startRequestPending;
    private boolean leaveRequestPending;
    private boolean roundOpened;
    private boolean allPlayersReady;
    private boolean connected = true;
    private int playerCount = 1;

    public WaitingRoomView(
            String username,
            String roomName,
            GameMode gameMode,
            boolean isHost
    ) {
        this(username, roomName, gameMode, isHost, null, null);
    }

    public WaitingRoomView(
            String username,
            String roomName,
            GameMode gameMode,
            boolean isHost,
            GameSession gameSession,
            String roomId
    ) {
        this.username = username;
        this.isHost = isHost;
        this.roomName = roomName;
        this.gameMode = gameMode;
        this.gameSession = gameSession;
        this.roomId = roomId;

        getStyleClass().add("waiting-room-root");
        setSpacing(16);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(20));

        Label title = new Label("WAITING ROOM");
        title.getStyleClass().add("lobby-title");

        Label roomLabel = new Label(
                roomId == null ? roomName : roomName + " [" + roomId + "]"
        );
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

        readyButton.getStyleClass().add("neon-button");
        readyButton.setPrefWidth(200);
        readyButton.setVisible(gameSession != null);
        readyButton.setManaged(gameSession != null);

        chooseWordButton.getStyleClass().add("neon-button");
        chooseWordButton.setPrefWidth(220);

        boolean doubleHost = gameMode == GameMode.DOUBLE_HOST;
        boolean hostSelection = gameMode == GameMode.HOST && isHost && gameSession != null;
        chooseWordButton.setVisible(doubleHost || hostSelection);
        chooseWordButton.setManaged(doubleHost || hostSelection);
        chooseWordButton.setDisable(gameSession != null && !hostSelection && !doubleHost);

        leaveButton.getStyleClass().add("secondary-button");
        leaveButton.setPrefWidth(200);

        HBox buttons = new HBox(
                15,
                readyButton,
                startButton,
                chooseWordButton,
                leaveButton
        );
        buttons.setAlignment(Pos.CENTER);

        if (gameSession != null && roomId != null) {
            JmsEventFeed jmsEventFeed = new JmsEventFeed(gameSession);
            jmsEventFeed.setMaxWidth(340);

            HBox roomContent = new HBox(20, playersBox, jmsEventFeed);
            roomContent.setAlignment(Pos.CENTER);

            getChildren().addAll(
                    title,
                    roomInfo,
                    roomContent,
                    statusLabel,
                    buttons
            );
        } else {
            getChildren().addAll(
                    title,
                    roomInfo,
                    playersBox,
                    statusLabel,
                    buttons
            );
        }

        updatePlayers(List.of(username), isHost ? username : "");

        if (gameSession != null && roomId != null) {
            updateStatus("Connected to room " + roomId);

            gameSession.setMessageHandler(this::handleServerMessage);

            gameSession.setErrorHandler(exception -> {
                connected = false;
                chooseWordButton.setDisable(true);
                if (hostWordView != null) {
                    closeHostWordView();
                }
                closeDoubleHostWordView();
                readyRequestPending = false;
                startRequestPending = false;
                leaveRequestPending = false;

                readyButton.setDisable(true);
                startButton.setDisable(true);
                leaveButton.setDisable(true);

                updateStatus(
                        "Connection lost: " + exception.getMessage()
                );
            });
        } else {
            updateStatus("Waiting for players to join...");
        }

        readyButton.setOnAction(event -> setReady());
        startButton.setOnAction(event -> startRound());
        leaveButton.setOnAction(event -> leaveRoom());

        chooseWordButton.setOnAction(event -> {
            if (gameMode == GameMode.HOST) {
                openHostWordView();
                return;
            }
            if (gameSession != null) {
                if (gameMode == GameMode.DOUBLE_HOST) openDoubleHostWordView();
                return;
            }

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
    }

    private void openDoubleHostWordView() {
        if (!connected || wordLocked || doubleHostWordView != null
                || readyRequestPending || startRequestPending || leaveRequestPending
                || roundOpened || gameSession == null
                || !gameSession.getTcpClient().isConnected()) return;
        waitingContent = new ArrayList<>(getChildren());
        doubleHostWordView = new DoubleHostWordView(this::lockDoubleHostWord, this::closeDoubleHostWordView);
        getChildren().setAll(doubleHostWordView);
    }

    private void closeDoubleHostWordView() {
        if (doubleHostWordView == null) return;
        doubleHostWordView = null;
        getChildren().setAll(waitingContent);
        waitingContent = null;
        updateStartButton();
    }

    private void lockDoubleHostWord(String word) {
        DoubleHostWordView view = doubleHostWordView;
        if (view == null) return;
        if (!connected || !gameSession.getTcpClient().isConnected()) {
            view.showError("Connection unavailable.");
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                gameSession.send("LOCK_WORD", roomId, word);
            } catch (IOException exception) {
                Platform.runLater(() -> {
                    if (doubleHostWordView == view && view.isSubmitting()) {
                        view.showError("Unable to lock secret word.");
                    }
                });
            }
        }, "wordarena-lock-double-host-word");
        thread.setDaemon(true);
        thread.start();
    }

    private void openHostWordView() {
        if (!isHost || !connected || wordLocked || hostWordView != null
                || readyRequestPending || startRequestPending || leaveRequestPending
                || roundOpened || gameSession == null
                || !gameSession.getTcpClient().isConnected()) {
            return;
        }
        waitingContent = new ArrayList<>(getChildren());
        hostWordView = new HostWordView(this::lockHostWord, this::closeHostWordView);
        getChildren().setAll(hostWordView);
    }

    private void closeHostWordView() {
        if (hostWordView == null) {
            return;
        }
        hostWordView = null;
        getChildren().setAll(waitingContent);
        waitingContent = null;
        updateStartButton();
    }

    private void lockHostWord(String word) {
        HostWordView view = hostWordView;
        if (view == null) {
            return;
        }
        if (!connected || !gameSession.getTcpClient().isConnected()) {
            view.showError("Connection unavailable.");
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                gameSession.send("LOCK_WORD", roomId, word);
            } catch (IOException exception) {
                Platform.runLater(() -> {
                    if (hostWordView == view && view.isSubmitting()) {
                        view.showError("Unable to lock secret word.");
                    }
                });
            }
        }, "wordarena-lock-host-word");
        thread.setDaemon(true);
        thread.start();
    }

    private void setReady() {
        if (!connected
                || gameSession == null
                || roomId == null
                || readyRequestPending
                || startRequestPending
                || leaveRequestPending
                || roundOpened
                || !gameSession.getTcpClient().isConnected()) {
            return;
        }

        boolean requestedReady = !ready;

        readyRequestPending = true;
        readyButton.setDisable(true);

        updateStatus(
                requestedReady
                        ? "Updating readiness to READY..."
                        : "Updating readiness to NOT READY..."
        );

        Thread thread = new Thread(() -> {
            try {
                gameSession.send(
                        "SET_READY",
                        roomId,
                        Boolean.toString(requestedReady)
                );
            } catch (IOException exception) {
                Platform.runLater(() -> {
                    readyRequestPending = false;

                    readyButton.setDisable(
                            !connected || leaveRequestPending
                    );

                    updateStatus(
                            "Unable to update readiness: "
                                    + exception.getMessage()
                    );
                });
            }
        });

        thread.setName("wordarena-set-ready");
        thread.setDaemon(true);
        thread.start();
    }

    private void startRound() {
        if (!canStartRound()) {
            return;
        }

        startRequestPending = true;

        updateStartButton();

        readyButton.setDisable(true);
        leaveButton.setDisable(true);

        updateStatus("Starting " + gameMode.name() + " round...");

        Thread thread = new Thread(() -> {
            try {
                gameSession.send("START_ROUND", roomId, "");
            } catch (IOException exception) {
                Platform.runLater(() -> {
                    startRequestPending = false;

                    readyButton.setDisable(!connected);
                    leaveButton.setDisable(!connected);

                    updateStartButton();

                    updateStatus(
                            "Unable to start round: "
                                    + exception.getMessage()
                    );
                });
            }
        });

        thread.setName("wordarena-start-round");
        thread.setDaemon(true);
        thread.start();
    }

    private void leaveRoom() {
        if (leaveRequestPending || startRequestPending || roundOpened) {
            return;
        }

        if (gameSession == null || roomId == null) {
            openLobby();
            return;
        }

        if (!connected || !gameSession.getTcpClient().isConnected()) {
            updateStatus(
                    "Connection lost. Unable to leave room."
            );
            return;
        }

        leaveRequestPending = true;

        leaveButton.setDisable(true);
        readyButton.setDisable(true);
        startButton.setDisable(true);

        updateStatus("Leaving room...");

        Thread thread = new Thread(() -> {
            try {
                gameSession.send("LEAVE_ROOM", roomId, "");
            } catch (IOException exception) {
                Platform.runLater(() -> {
                    leaveRequestPending = false;

                    leaveButton.setDisable(!connected);
                    readyButton.setDisable(!connected);

                    updateStartButton();

                    updateStatus(
                            "Unable to leave room: "
                                    + exception.getMessage()
                    );
                });
            }
        });

        thread.setName("wordarena-leave-room");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleServerMessage(GameMessage message) {

        if ("ERROR".equals(message.getType())) {

            String content = message.getContent();

            if (content != null
                    && content.startsWith("ROOM_NOT_FOUND|")
                    && roomId != null
                    && roomId.equals(message.getRoomId())) {

                leaveRequestPending = false;
                readyRequestPending = false;
                startRequestPending = false;

                closeHostWordView();
                closeDoubleHostWordView();
                returnToLobbyAfterLeaving();
                return;
            }

            if (doubleHostWordView != null && doubleHostWordView.isSubmitting()
                    && (roomId.equals(message.getRoomId())
                    || message.getRoomId() == null || message.getRoomId().isBlank())) {
                doubleHostWordView.showError("Server rejected the secret word.");
                return;
            }

            if (hostWordView != null && hostWordView.isSubmitting()
                    && (roomId.equals(message.getRoomId())
                    || message.getRoomId() == null || message.getRoomId().isBlank())) {
                hostWordView.showError("Server rejected the secret word.");
                return;
            }

            if (leaveRequestPending) {
                leaveRequestPending = false;
                leaveButton.setDisable(!connected);
            }

            if (startRequestPending) {
                startRequestPending = false;
                leaveButton.setDisable(!connected);
            }

            readyRequestPending = false;
            readyButton.setDisable(!connected);

            updateStartButton();

            updateStatus("Server error: " + content);
            return;
        }

        if ("OK".equals(message.getType()) && leaveRequestPending) {

            if (roomId != null
                    && roomId.equals(message.getRoomId())
                    && gameSession.getPlayerId().equals(message.getPlayerId())
                    && "Successfully left the room.".equals(
                    message.getContent()
            )) {

                leaveRequestPending = false;
                returnToLobbyAfterLeaving();
            }

            return;
        }

        if (roomId == null || !roomId.equals(message.getRoomId())) {
            return;
        }

        switch (message.getType()) {
            case "WORD_LOCKED" -> {
                if (gameMode == GameMode.DOUBLE_HOST) {
                    String content = message.getContent();
                    if ("DOUBLE_HOST|LOCKED|true".equals(content)
                            || "DOUBLE_HOST|LOCKED|false".equals(content)) {
                        if ("DOUBLE_HOST|LOCKED|true".equals(content)) bothWordsLocked = true;
                        if (gameSession.getPlayerId().equals(message.getPlayerId())
                                && doubleHostWordView != null && doubleHostWordView.isSubmitting()) {
                            wordLocked = true;
                            doubleHostWordView.showLocked();
                            chooseWordButton.setText("WORD LOCKED");
                            chooseWordButton.setDisable(true);
                            closeDoubleHostWordView();
                        }
                        if (wordLocked) updateStatus(bothWordsLocked
                                ? "Both secret words locked."
                                : "Your word is locked. Waiting for opponent...");
                        updateStartButton();
                    }
                    return;
                }
                if (gameMode == GameMode.HOST && isHost
                        && hostWordView != null && hostWordView.isSubmitting()
                        && gameSession.getPlayerId().equals(message.getPlayerId())
                        && "HOST|LOCKED".equals(message.getContent())) {
                    wordLocked = true;
                    hostWordView.showLocked();
                    chooseWordButton.setText("WORD LOCKED");
                    chooseWordButton.setDisable(true);
                    closeHostWordView();
                    updateStatus("Secret word successfully locked.");
                }
            }

            case "ROOM_PLAYERS" ->
                    handleRoomPlayers(message.getContent());

            case "ROOM_STATE" ->
                    handleRoomState(message.getContent());

            case "ROUND_STATE" ->
                    handleRoundState(message.getContent());

            default -> {
            }
        }
    }

    private void returnToLobbyAfterLeaving() {
        if (gameSession != null) {
            gameSession.clearRoom();
            gameSession.setMessageHandler(null);
            gameSession.setErrorHandler(null);
        }

        openLobby();
    }

    private void handleRoomPlayers(String content) {
        if (content == null || content.isBlank()) {
            updateStatus("Invalid ROOM_PLAYERS response.");
            return;
        }

        String[] encodedPlayers = content.split(";", -1);

        List<String> usernames = new ArrayList<>();
        List<String> playerLabels = new ArrayList<>();

        Boolean confirmedReady = null;
        boolean everyoneReady = true;

        for (String encodedPlayer : encodedPlayers) {

            String[] fields = encodedPlayer.split("~", -1);

            if (fields.length != 6) {
                updateStatus("Invalid ROOM_PLAYERS response.");
                return;
            }

            String playerId = fields[0];
            String playerUsername = fields[1];

            boolean playerReady =
                    Boolean.parseBoolean(fields[2]);

            everyoneReady = everyoneReady && playerReady;

            usernames.add(playerUsername);

            String displayText = playerUsername
                    + (playerReady
                    ? " (READY)"
                    : " (NOT READY)");

            if (playerId.equals(hostPlayerId)) {
                displayText += " (HOST)";
            }

            playerLabels.add(displayText);

            if (gameSession != null
                    && playerId.equals(gameSession.getPlayerId())) {
                confirmedReady = playerReady;
            }
        }

        playerCount = usernames.size();
        if (gameMode == GameMode.DOUBLE_HOST && playerCount < 2) bothWordsLocked = false;
        allPlayersReady = everyoneReady;

        opponentUsername = usernames.stream()
                .filter(player -> !player.equals(username))
                .findFirst()
                .orElse("Waiting for player...");

        playersList.getChildren().clear();

        for (String displayText : playerLabels) {
            Label playerLabel = new Label(
                    "\u25CF  " + displayText
            );

            playerLabel.getStyleClass().add("waiting-player");
            playersList.getChildren().add(playerLabel);
        }

        if (playerCount < 2) {
            Label emptySlot = new Label(
                    "\u25CB  Waiting for another player..."
            );

            emptySlot.getStyleClass().add(
                    "waiting-player-empty"
            );

            playersList.getChildren().add(emptySlot);
        }

        if (confirmedReady != null) {
            ready = confirmedReady;
            readyRequestPending = false;

            readyButton.setText(
                    ready ? "NOT READY" : "READY"
            );

            readyButton.setDisable(
                    !connected
                            || leaveRequestPending
                            || startRequestPending
                            || roundOpened
            );
        }

        if (!leaveRequestPending) {
            updateStatus(
                    "Room " + roomId + " - "
                            + playerCount + " player(s) connected."
            );
        }

        updateStartButton();
    }

    private void handleRoomState(String content) {
        if (content == null) {
            return;
        }

        String[] fields = content.split("\\|", -1);

        if (fields.length != 6) {
            updateStatus("Invalid ROOM_STATE response.");
            return;
        }

        hostPlayerId = fields[5];

        try {
            int currentPlayers =
                    Integer.parseInt(fields[2]);

            int maxPlayers =
                    Integer.parseInt(fields[3]);

            if (!leaveRequestPending) {
                updateStatus(
                        "Room " + roomId + " - "
                                + currentPlayers + "/"
                                + maxPlayers + " players."
                );
            }
        } catch (NumberFormatException exception) {
            updateStatus(
                    "Invalid player count from server."
            );
        }
    }

    private void handleRoundState(String content) {
        if (roundOpened
                || leaveRequestPending
                || content == null) {
            return;
        }

        if (gameMode == GameMode.HOST) {
            openHostRound(content);
            return;
        }

        if (gameMode == GameMode.DOUBLE_HOST) {
            openDoubleHostRound(content);
            return;
        }

        if (gameMode == GameMode.COOPERATIVE) {
            openCooperativeRound(content);
            return;
        }

        if (gameMode != GameMode.DUEL) {
            return;
        }

        String[] fields = content.split("\\|", -1);

        if (fields.length != 3
                || !"ACTIVE".equals(fields[0])) {
            return;
        }

        final int maxAttempts;

        try {
            maxAttempts = Integer.parseInt(fields[1]);
            Long.parseLong(fields[2]);
        } catch (NumberFormatException exception) {
            updateStatus(
                    "Invalid ROUND_STATE response."
            );
            return;
        }

        if (maxAttempts <= 0) {
            updateStatus(
                    "Invalid maximum attempts."
            );
            return;
        }

        roundOpened = true;
        startRequestPending = false;

        readyButton.setDisable(true);
        startButton.setDisable(true);
        leaveButton.setDisable(true);

        DuelMultiplayerView gameView =
                new DuelMultiplayerView(
                        gameSession,
                        roomId,
                        roomName,
                        opponentUsername,
                        maxAttempts
                );

        ScrollPane scrollPane =
                new ScrollPane(gameView);

        scrollPane.setFitToWidth(true);

        StackPane overlay =
                new StackPane(scrollPane);

        Scene scene = new Scene(
                overlay,
                1100,
                800
        );

        applyTheme(scene);

        Stage stage =
                (Stage) getScene().getWindow();

        stage.setScene(scene);
        stage.setTitle(
                "WordArena - Multiplayer DUEL"
        );
    }

    private void openHostRound(String content) {
        String[] fields = content.split("\\|", -1);

        if (fields.length != 5
                || !"HOST".equals(fields[0])
                || !"ACTIVE".equals(fields[1])
                || fields[3].isBlank()
                || !fields[3].equals(hostPlayerId)) {
            return;
        }

        final int maxAttempts;

        try {
            maxAttempts = Integer.parseInt(fields[2]);
            Long.parseLong(fields[4]);
        } catch (NumberFormatException exception) {
            updateStatus("Invalid HOST ROUND_STATE response.");
            return;
        }

        if (maxAttempts != 6 || getScene() == null
                || !(getScene().getWindow() instanceof Stage stage)) {
            return;
        }

        HostMultiplayerView gameView = new HostMultiplayerView(
                gameSession, roomId, roomName,
                gameSession.getPlayerId().equals(fields[3]), maxAttempts
        );

        HostGameController controller = new HostGameController(
                gameSession, gameView
        );

        ScrollPane scrollPane = new ScrollPane(gameView);
        scrollPane.setFitToWidth(true);
        StackPane overlay = new StackPane(scrollPane);
        Scene scene = new Scene(overlay, 1100, 800);
        applyTheme(scene);

        roundOpened = true;
        startRequestPending = false;
        readyButton.setDisable(true);
        startButton.setDisable(true);
        leaveButton.setDisable(true);
        chooseWordButton.setDisable(true);

        stage.setScene(scene);
        stage.setTitle("WordArena - Multiplayer HOST");
        controller.start();
    }

    private void openDoubleHostRound(String content) {
        String[] fields = content.split("\\|", -1);

        if (fields.length != 4
                || !"DOUBLE_HOST".equals(fields[0])
                || !"ACTIVE".equals(fields[1])) {
            return;
        }

        final int maxAttempts;

        try {
            maxAttempts = Integer.parseInt(fields[2]);
            Long.parseLong(fields[3]);
        } catch (NumberFormatException exception) {
            updateStatus("Invalid DOUBLE_HOST ROUND_STATE response.");
            return;
        }

        if (maxAttempts != 6 || gameSession == null || getScene() == null
                || !(getScene().getWindow() instanceof Stage stage)) {
            return;
        }

        DoubleHostMultiplayerView gameView = new DoubleHostMultiplayerView(
                gameSession, roomId, roomName, opponentUsername, maxAttempts
        );

        DoubleHostGameController controller = new DoubleHostGameController(
                gameSession, gameView
        );
        controller.setResultHandler(gameView::showFinalResults);

        ScrollPane scrollPane = new ScrollPane(gameView);
        scrollPane.setFitToWidth(true);
        StackPane overlay = new StackPane(scrollPane);
        Scene scene = new Scene(overlay, 1100, 800);
        applyTheme(scene);

        roundOpened = true;
        startRequestPending = false;
        readyButton.setDisable(true);
        startButton.setDisable(true);
        leaveButton.setDisable(true);
        chooseWordButton.setDisable(true);

        stage.setScene(scene);
        stage.setTitle("WordArena - Multiplayer DOUBLE HOST");
        controller.start();
    }

    private void openCooperativeRound(String content) {
        String[] fields = content.split("\\|", -1);

        if (fields.length != 4
                || !"COOPERATIVE".equals(fields[0])
                || !"ACTIVE".equals(fields[1])) {
            return;
        }

        final int maxAttempts;

        try {
            maxAttempts = Integer.parseInt(fields[2]);
            Long.parseLong(fields[3]);
        } catch (NumberFormatException exception) {
            updateStatus("Invalid COOPERATIVE ROUND_STATE response.");
            return;
        }

        if (maxAttempts != 6 || gameSession == null || getScene() == null
                || !(getScene().getWindow() instanceof Stage stage)) {
            return;
        }

        CooperativeMultiplayerView gameView = new CooperativeMultiplayerView(
                gameSession, roomId, roomName, opponentUsername, maxAttempts
        );

        CooperativeGameController controller = new CooperativeGameController(
                gameSession, gameView
        );
        controller.setResultHandler(gameView::showFinalResults);

        ScrollPane scrollPane = new ScrollPane(gameView);
        scrollPane.setFitToWidth(true);
        StackPane overlay = new StackPane(scrollPane);
        Scene scene = new Scene(overlay, 1100, 800);
        applyTheme(scene);

        roundOpened = true;
        startRequestPending = false;
        readyButton.setDisable(true);
        startButton.setDisable(true);
        leaveButton.setDisable(true);
        chooseWordButton.setDisable(true);

        stage.setScene(scene);
        stage.setTitle("WordArena - Multiplayer COOPERATIVE");
        controller.start();
    }

    private boolean canStartRound() {
        return connected
                && gameSession != null
                && roomId != null
                && (gameMode == GameMode.DUEL
                || (gameMode == GameMode.COOPERATIVE && playerCount == 2)
                || (gameMode == GameMode.HOST && wordLocked
                && hostWordView == null)
                || (gameMode == GameMode.DOUBLE_HOST && wordLocked
                && bothWordsLocked && doubleHostWordView == null && playerCount == 2))
                && isHost
                && playerCount >= 2
                && allPlayersReady
                && !readyRequestPending
                && !startRequestPending
                && !leaveRequestPending
                && !roundOpened
                && gameSession.getTcpClient().isConnected();
    }

    private void updateStartButton() {
        if (gameSession != null) {
            startButton.setDisable(
                    !canStartRound()
            );
            return;
        }

        boolean canStart =
                isHost && playerCount >= 2;

        if (gameMode == GameMode.DOUBLE_HOST) {
            canStart = canStart && wordLocked;
        }

        startButton.setDisable(!canStart);
    }

    public void updatePlayers(
            List<String> players,
            String hostUsername
    ) {
        playerCount = players.size();

        playersList.getChildren().clear();

        opponentUsername = players.stream()
                .filter(player -> !player.equals(username))
                .findFirst()
                .orElse("Waiting for player...");

        for (String player : players) {

            boolean playerIsHost =
                    player.equals(hostUsername);

            Label playerLabel = new Label(
                    "\u25CF  " + player
                            + (playerIsHost
                            ? " (HOST)"
                            : "")
            );

            playerLabel.getStyleClass().add(
                    "waiting-player"
            );

            playersList.getChildren().add(
                    playerLabel
            );
        }

        if (players.size() < 2) {
            Label emptySlot = new Label(
                    "\u25CB  Waiting for another player..."
            );

            emptySlot.getStyleClass().add(
                    "waiting-player-empty"
            );

            playersList.getChildren().add(
                    emptySlot
            );
        }

        updateStartButton();
    }

    public void updateStatus(String status) {
        statusLabel.setText(status);
    }

    private void openLobby() {
        LobbyView lobby = new LobbyView(
                username,
                gameSession
        );

        Scene scene = new Scene(
                lobby,
                800,
                600
        );

        applyTheme(scene);

        Stage stage =
                (Stage) getScene().getWindow();

        stage.setScene(scene);
        stage.setTitle(
                "WordArena - Lobby"
        );
    }

    private void applyTheme(Scene scene) {
        scene.getStylesheets().add(
                getClass()
                        .getResource("/css/wordarena.css")
                        .toExternalForm()
        );
    }
}

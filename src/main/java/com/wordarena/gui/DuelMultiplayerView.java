
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class DuelMultiplayerView extends VBox {

    private final GameSession gameSession;
    private final String roomId;
    private final String username;
    private final int maxAttempts;

    private String opponentUsername;

    private final Label[][] cells;
    private final Label attemptsLabel = new Label();
    private final Label messageLabel = new Label();
    private final TextField guessField = new TextField();
    private final Button submitButton = new Button("SUBMIT GUESS");
    private final Button backButton = new Button("BACK TO LOBBY");

    private final GameHeaderView gameHeader;
    private final PlayerPanelView playerPanel;
    private final EventFeedView eventFeed;
    private final OnScreenKeyboard keyboard;

    private int currentAttempt;
    private int playerScore;
    private int opponentAttempts;
    private int opponentScore;

    private boolean opponentStatsInitialized;
    private boolean guessPending;
    private boolean roundFinished;
    private boolean resultShown;
    private boolean leavingRoom;
    private boolean connected = true;
    private boolean matchInterrupted;
    private boolean opponentSeen = true;
    private boolean awaitingFinalResult;

    public DuelMultiplayerView(
            GameSession gameSession,
            String roomId,
            String roomName,
            String opponentUsername,
            int maxAttempts
    ) {
        this.gameSession = gameSession;
        this.roomId = roomId;
        this.username = gameSession.getUsername();
        this.opponentUsername = opponentUsername;
        this.maxAttempts = maxAttempts;
        this.cells = new Label[maxAttempts][5];

        getStyleClass().add("game-root");
        setSpacing(20);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(30));

        Label title = new Label("WORDARENA");
        title.getStyleClass().add("lobby-title");

        gameHeader = new GameHeaderView(roomName, GameMode.DUEL);
        gameHeader.updateStatus("ACTIVE");
        gameHeader.updateConnectionStatus("CONNECTED");

        playerPanel = new PlayerPanelView(username);
        playerPanel.updateOpponentName(opponentUsername);

        eventFeed = new EventFeedView();
        eventFeed.addEvent("DUEL round started");
        eventFeed.addEvent("Room: " + roomId);

        attemptsLabel.setText(
                "Attempts remaining: " + maxAttempts
        );
        attemptsLabel.getStyleClass().add("game-attempts");

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        grid.setAlignment(Pos.CENTER);

        for (int row = 0; row < maxAttempts; row++) {
            for (int col = 0; col < 5; col++) {
                Label cell = new Label("");
                cell.setMinSize(55, 55);
                cell.setPrefSize(55, 55);
                cell.setMaxSize(55, 55);
                cell.setAlignment(Pos.CENTER);
                cell.getStyleClass().add("wordle-cell");

                cells[row][col] = cell;
                grid.add(cell, col, row);
            }
        }

        HBox gameLayout = new HBox(
                20,
                playerPanel,
                grid,
                eventFeed
        );
        gameLayout.setAlignment(Pos.CENTER);

        guessField.setPromptText("Enter a 5-letter word");
        guessField.setMaxWidth(250);

        submitButton.setPrefWidth(250);
        submitButton.getStyleClass().add("neon-button");

        keyboard = new OnScreenKeyboard(key -> {
            if (guessPending
                    || roundFinished
                    || leavingRoom
                    || matchInterrupted
                    || !connected) {
                return;
            }

            if (key.equals("ENTER")) {
                submitButton.fire();
            } else if (key.equals("BACKSPACE")) {
                String current = guessField.getText();

                if (!current.isEmpty()) {
                    guessField.setText(
                            current.substring(0, current.length() - 1)
                    );
                }
            } else if (guessField.getText().length() < 5) {
                guessField.appendText(key);
            }
        });

        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("waiting-status");

        backButton.setPrefWidth(250);
        backButton.getStyleClass().add("secondary-button");
        backButton.setDisable(true);

        getChildren().addAll(
                title,
                gameHeader,
                attemptsLabel,
                gameLayout,
                guessField,
                keyboard,
                submitButton,
                messageLabel,
                backButton
        );

        gameSession.setMessageHandler(this::handleServerMessage);

        gameSession.setErrorHandler(exception -> {
            connected = false;
            guessPending = false;
            leavingRoom = false;
            roundFinished = true;
            matchInterrupted = true;

            disableGuessInput();
            backButton.setDisable(true);

            gameHeader.updateConnectionStatus("DISCONNECTED");
            gameHeader.updateStatus("INTERRUPTED");

            messageLabel.setText(
                    "Connection lost: " + exception.getMessage()
            );

            eventFeed.addEvent("Disconnected from the server.");
        });

        submitButton.setOnAction(event -> submitGuess());
        guessField.setOnAction(event -> submitGuess());
        backButton.setOnAction(event -> leaveRoom());

        messageLabel.setText("Enter your first guess.");
    }

    private void submitGuess() {
        if (roundFinished
                || matchInterrupted
                || guessPending
                || leavingRoom
                || !connected) {
            return;
        }

        String guess = guessField.getText().trim().toUpperCase();

        if (!guess.matches("[A-Z]{5}")) {
            messageLabel.setText("Enter exactly 5 English letters.");
            return;
        }

        if (currentAttempt >= maxAttempts) {
            messageLabel.setText("No attempts remaining.");
            return;
        }

        guessPending = true;
        disableGuessInput();
        messageLabel.setText("Submitting guess...");

        Thread guessThread = new Thread(() -> {
            try {
                gameSession.send("GUESS", roomId, guess);
            } catch (IOException exception) {
                Platform.runLater(() -> {
                    guessPending = false;

                    if (!gameSession.getTcpClient().isConnected()) {
                        handleConnectionFailure(exception);
                    } else if (!roundFinished && !matchInterrupted) {
                        enableGuessInput();
                        messageLabel.setText(
                                "Unable to submit guess: "
                                        + exception.getMessage()
                        );
                    }
                });
            }
        });

        guessThread.setName("wordarena-submit-guess");
        guessThread.setDaemon(true);
        guessThread.start();
    }

    private void handleConnectionFailure(Exception exception) {
        if (!connected) {
            return;
        }

        connected = false;
        guessPending = false;
        leavingRoom = false;
        roundFinished = true;
        matchInterrupted = true;

        disableGuessInput();
        backButton.setDisable(true);

        gameHeader.updateConnectionStatus("DISCONNECTED");
        gameHeader.updateStatus("INTERRUPTED");

        messageLabel.setText(
                "Connection lost: " + exception.getMessage()
        );

        eventFeed.addEvent("Disconnected from the server.");
    }

    private void handleServerMessage(GameMessage message) {
        if ("OK".equals(message.getType())
                && leavingRoom
                && (roomId.equals(message.getRoomId())
                || message.getRoomId() == null
                || message.getRoomId().isBlank())) {
            returnToLobby();
            return;
        }

        if ("ERROR".equals(message.getType())) {
            String error = message.getContent() == null
                    ? ""
                    : message.getContent();

            if (error.startsWith("ROOM_NOT_FOUND|")
                    && roomId.equals(message.getRoomId())) {
                if (!leavingRoom) {
                    eventFeed.addEvent(
                            "The host disconnected or closed the room."
                    );
                }

                returnToLobby();
                return;
            }

            if (leavingRoom) {
                leavingRoom = false;
                backButton.setDisable(false);

                messageLabel.setText(
                        "Unable to leave room: " + error
                );
                return;
            }

            guessPending = false;

            if (!roundFinished && !matchInterrupted) {
                enableGuessInput();
            }

            messageLabel.setText("Server error: " + error);
            return;
        }

        if (!roomId.equals(message.getRoomId())) {
            return;
        }

        switch (message.getType()) {
            case "GUESS_RESULT" ->
                    handleGuessResult(message.getContent());
            case "ROUND_STATE" ->
                    handleRoundState(message.getContent());
            case "ROOM_PLAYERS" ->
                    handleRoomPlayers(message.getContent());
            case "GAME_OVER" ->
                    handleGameOver(message.getContent());
            default -> {
            }
        }
    }

    private void handleGuessResult(String content) {
        if (roundFinished || content == null) {
            return;
        }

        String[] fields = content.split("\\|", -1);

        if (fields.length != 6) {
            messageLabel.setText("Invalid GUESS_RESULT response.");
            return;
        }

        String word = fields[0];
        String[] feedback = fields[1].split(",", -1);

        if (!word.matches("[A-Z]{5}") || feedback.length != 5) {
            messageLabel.setText("Invalid guess feedback.");
            return;
        }

        final int attemptsUsed;
        final int attemptsRemaining;
        final int score;

        try {
            attemptsUsed = Integer.parseInt(fields[2]);
            attemptsRemaining = Integer.parseInt(fields[3]);
            score = Integer.parseInt(fields[4]);
        } catch (NumberFormatException exception) {
            messageLabel.setText("Invalid guess statistics.");
            return;
        }

        if (attemptsUsed < 1 || attemptsUsed > maxAttempts) {
            messageLabel.setText("Invalid attempt number.");
            return;
        }

        int[] feedbackValues = new int[5];

        for (int col = 0; col < 5; col++) {
            feedbackValues[col] = feedbackValue(feedback[col]);

            if (feedbackValues[col] < 0) {
                messageLabel.setText("Invalid letter feedback.");
                return;
            }
        }

        int row = attemptsUsed - 1;

        for (int col = 0; col < 5; col++) {
            int value = feedbackValues[col];
            Label cell = cells[row][col];

            cell.setText(String.valueOf(word.charAt(col)));

            keyboard.updateKeyFeedback(
                    String.valueOf(word.charAt(col)),
                    value
            );

            String color = switch (value) {
                case 2 -> "#16a34a";
                case 1 -> "#ca8a04";
                default -> "#64748b";
            };

            cell.setStyle(
                    "-fx-background-color: " + color + ";" +
                            "-fx-border-color: " + color + ";" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-radius: 8;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 24px;" +
                            "-fx-font-weight: bold;"
            );
        }

        currentAttempt = attemptsUsed;
        playerScore = score;
        guessPending = false;

        playerPanel.updatePlayerScore(score);
        playerPanel.updatePlayerProgress(
                (double) attemptsUsed / maxAttempts
        );

        attemptsLabel.setText(
                "Attempts remaining: " + attemptsRemaining
        );

        eventFeed.addEvent(username + " guessed " + word);

        messageLabel.setText(
                "Guess: " + word + " | Score: " + score
        );

        guessField.clear();

        boolean solved = Boolean.parseBoolean(fields[5]);

        if (solved) {
            awaitingFinalResult = true;
            messageLabel.setText(
                    "You solved the word! Waiting for the final result..."
            );
            disableGuessInput();
        } else if (attemptsRemaining <= 0) {
            awaitingFinalResult = true;
            messageLabel.setText(
                    "No attempts remaining. Waiting for the final result..."
            );
            disableGuessInput();
        } else {
            enableGuessInput();
        }
    }

    private int feedbackValue(String feedback) {
        return switch (feedback) {
            case "CORRECT" -> 2;
            case "PRESENT" -> 1;
            case "ABSENT" -> 0;
            default -> -1;
        };
    }

    private void handleRoundState(String content) {
        if (content == null || matchInterrupted) {
            return;
        }

        String[] fields = content.split("\\|", -1);

        if (fields.length == 2 && "FINISHED".equals(fields[0])) {
            gameHeader.updateStatus("FINISHED");
            disableGuessInput();
        }
    }

    private void handleRoomPlayers(String content) {
        if (content == null || matchInterrupted) {
            return;
        }

        String[] players = content.split(";", -1);
        boolean foundOpponent = false;
        boolean foundSelf = false;

        for (String player : players) {
            String[] fields = player.split("~", -1);

            if (fields.length != 6) {
                continue;
            }

            String playerId = fields[0];
            String playerUsername = fields[1];

            try {
                int attemptsUsed = Integer.parseInt(fields[3]);
                int score = Integer.parseInt(fields[4]);

                double progress = (double) attemptsUsed / maxAttempts;

                if (playerId.equals(gameSession.getPlayerId())) {
                    foundSelf = true;

                    currentAttempt = Math.max(
                            currentAttempt,
                            attemptsUsed
                    );

                    playerScore = score;
                    playerPanel.updatePlayerScore(score);
                    playerPanel.updatePlayerProgress(progress);
                } else {
                    foundOpponent = true;

                    opponentUsername = playerUsername;
                    opponentScore = score;

                    playerPanel.updateOpponentName(playerUsername);
                    playerPanel.updateOpponentScore(score);
                    playerPanel.updateOpponentProgress(progress);

                    if (opponentStatsInitialized
                            && attemptsUsed > opponentAttempts) {
                        eventFeed.addEvent(
                                playerUsername
                                        + " made a guess ("
                                        + attemptsUsed
                                        + "/"
                                        + maxAttempts
                                        + " attempts)"
                        );
                    }

                    opponentAttempts = attemptsUsed;
                    opponentStatsInitialized = true;
                }

            } catch (NumberFormatException exception) {
                messageLabel.setText(
                        "Invalid player statistics received from server."
                );
                return;
            }
        }

        if (foundOpponent) {
            opponentSeen = true;
        }

        if (foundSelf
                && !foundOpponent
                && opponentSeen
                && !roundFinished
                && !resultShown) {
            interruptMatch();
        }
    }

    private void interruptMatch() {
        if (matchInterrupted || resultShown) {
            return;
        }

        matchInterrupted = true;
        roundFinished = true;
        guessPending = false;

        disableGuessInput();

        gameHeader.updateStatus("INTERRUPTED");

        messageLabel.setText(
                "Opponent disconnected - match interrupted."
        );

        eventFeed.addEvent(
                opponentUsername + " disconnected from the match."
        );

        backButton.setDisable(
                !connected
                        || !gameSession.getTcpClient().isConnected()
        );
    }

    private void handleGameOver(String content) {
        if (resultShown || matchInterrupted || content == null) {
            return;
        }

        String[] fields = content.split("\\|", -1);

        if (fields.length != 3) {
            messageLabel.setText("Invalid GAME_OVER response.");
            return;
        }

        String serverResult = fields[0];
        String winnerPlayerId = fields[1];
        String secretWord = fields[2];

        if (!"WINNER".equals(serverResult)
                && !"NO_WINNER".equals(serverResult)) {
            messageLabel.setText("Invalid game result.");
            return;
        }

        resultShown = true;
        roundFinished = true;
        awaitingFinalResult = false;

        disableGuessInput();
        gameHeader.updateStatus("FINISHED");
        backButton.setDisable(!connected);

        String result;

        if ("NO_WINNER".equals(serverResult)) {
            result = "DRAW";
            messageLabel.setText("ROUND FINISHED - DRAW");
            eventFeed.addEvent("Round finished without a winner.");
        } else if (winnerPlayerId.equals(gameSession.getPlayerId())) {
            result = "WIN";
            messageLabel.setText("YOU WON!");
            eventFeed.addEvent(username + " won the DUEL!");
        } else {
            result = "LOSS";
            messageLabel.setText("YOU LOST!");
            eventFeed.addEvent(opponentUsername + " won the DUEL!");
        }

        showResultsPopup(result, secretWord);
    }

    private void showResultsPopup(
            String result,
            String secretWord
    ) {
        if (getScene() == null
                || !(getScene().getRoot() instanceof StackPane overlay)) {
            return;
        }

        GameResultsView resultsView = new GameResultsView();

        resultsView.showMultiplayerResult(
                result,
                secretWord,
                username,
                playerScore,
                currentAttempt,
                opponentUsername,
                opponentScore,
                opponentAttempts,
                maxAttempts
        );

        Button backToLobby = new Button("BACK TO LOBBY");
        backToLobby.getStyleClass().add("secondary-button");
        backToLobby.setPrefWidth(220);
        backToLobby.setDisable(!connected);

        backToLobby.setOnAction(event -> {
            if (!leavingRoom) {
                leaveRoom();
            }
        });

        VBox popup = new VBox(
                20,
                resultsView,
                backToLobby
        );

        popup.setAlignment(Pos.CENTER);
        popup.setMaxWidth(550);
        popup.setMaxHeight(Region.USE_PREF_SIZE);
        popup.getStyleClass().add("results-popup");

        StackPane backdrop = new StackPane(popup);

        backdrop.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.78);"
        );

        backdrop.setPickOnBounds(true);

        overlay.getChildren().add(backdrop);
        StackPane.setAlignment(backdrop, Pos.CENTER);
    }

    private void leaveRoom() {
        if ((!roundFinished && !matchInterrupted)
                || leavingRoom
                || !connected
                || !gameSession.getTcpClient().isConnected()) {
            return;
        }

        leavingRoom = true;
        backButton.setDisable(true);
        messageLabel.setText("Leaving room...");

        Thread leaveThread = new Thread(() -> {
            try {
                gameSession.send("LEAVE_ROOM", roomId, "");
            } catch (IOException exception) {
                Platform.runLater(() -> {
                    leavingRoom = false;

                    if (!gameSession.getTcpClient().isConnected()) {
                        handleConnectionFailure(exception);
                    } else {
                        backButton.setDisable(false);
                        messageLabel.setText(
                                "Unable to leave room: "
                                        + exception.getMessage()
                        );
                    }
                });
            }
        });

        leaveThread.setName("wordarena-leave-room");
        leaveThread.setDaemon(true);
        leaveThread.start();
    }

    private void returnToLobby() {
        leavingRoom = false;
        gameSession.clearRoom();
        gameSession.setMessageHandler(null);
        gameSession.setErrorHandler(null);
        openLobby();
    }

    private void openLobby() {
        LobbyView lobby = new LobbyView(username, gameSession);

        Scene scene = new Scene(lobby, 800, 600);

        scene.getStylesheets().add(
                getClass()
                        .getResource("/css/wordarena.css")
                        .toExternalForm()
        );

        Stage stage = (Stage) getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("WordArena - Lobby");
    }

    private void enableGuessInput() {
        if (roundFinished
                || matchInterrupted
                || awaitingFinalResult
                || guessPending
                || currentAttempt >= maxAttempts
                || !connected
                || !gameSession.getTcpClient().isConnected()) {
            return;
        }

        submitButton.setDisable(false);
        guessField.setDisable(false);
        keyboard.setKeyboardDisabled(false);
    }

    private void disableGuessInput() {
        submitButton.setDisable(true);
        guessField.setDisable(true);
        keyboard.setKeyboardDisabled(true);
    }
}

package com.wordarena.gui;

import com.wordarena.client.tcp.GameSession;
import com.wordarena.common.GameMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.function.Consumer;

public class DoubleHostMultiplayerView extends VBox {

    private final GameSession gameSession;
    private final String roomId;
    private final String opponentUsername;
    private final Label opponentProgress = new Label();
    private final Label ownScore = new Label("Your score: 0");
    private final Label opponentScore = new Label("Opponent score: 0");
    private final int maxAttempts;

    private final Label[][] cells;
    private final Label[] guesserLabels;

    private final Label attemptsLabel = new Label();
    private final Label remainingLabel = new Label();
    private final Label messageLabel = new Label();
    private final Label lastGuessLabel = new Label();

    private final ProgressBar progressBar =
            new ProgressBar(0);

    private final TextField guessField =
            new TextField();

    private final Button submitButton =
            new Button("SUBMIT GUESS");

    private final Button backButton =
            new Button("BACK TO LOBBY");

    private final GameHeaderView gameHeader;
    private final EventFeedView eventFeed;
    private final OnScreenKeyboard keyboard;

    private Consumer<String> guessHandler;
    private Runnable leaveHandler;

    private int attemptsUsed;
    private int playerScoreValue;
    private int opponentScoreValue;
    private int opponentAttemptsValue;
    private GameResultsView resultsView;
    private String finalOutcome;
    private String finalOwnWord;
    private String finalTargetWord;
    private boolean guessPending;
    private boolean roundFinished;
    private boolean finalResultReceived;
    private boolean connected;

    public DoubleHostMultiplayerView(
            GameSession gameSession,
            String roomId,
            String roomName,
            String opponentUsername,
            int maxAttempts
    ) {

        this.gameSession = gameSession;
        this.roomId = roomId;
        this.opponentUsername = opponentUsername;
        this.maxAttempts = maxAttempts;

        this.cells = new Label[maxAttempts][5];
        this.guesserLabels = new Label[maxAttempts];

        this.connected =
                gameSession.getTcpClient().isConnected();

        getStyleClass().add("game-root");

        setSpacing(15);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(25));

        Label title = new Label("WORDARENA");
        title.getStyleClass().add("lobby-title");

        gameHeader = new GameHeaderView(
                roomName,
                GameMode.DOUBLE_HOST
        );

        gameHeader.updateStatus("PLAYING");

        gameHeader.updateConnectionStatus(
                connected ? "CONNECTED" : "DISCONNECTED"
        );

        Label roleLabel = new Label(
                "GUESS YOUR OPPONENT'S WORD"
        );

        roleLabel.getStyleClass().add("section-title");

        eventFeed = new EventFeedView();

        eventFeed.addEvent("DOUBLE_HOST round started");
        eventFeed.addEvent("Room: " + roomId);

        GridPane grid = new GridPane();

        grid.setHgap(6);
        grid.setVgap(6);
        grid.setAlignment(Pos.CENTER);

        for (int row = 0; row < maxAttempts; row++) {

            Label guesserLabel = new Label("");

            guesserLabel.setPrefWidth(100);
            guesserLabel.setWrapText(true);

            guesserLabel.setStyle(
                    "-fx-text-fill: #C4B5FD;"
                            + "-fx-font-size: 12px;"
            );

            guesserLabels[row] = guesserLabel;

            grid.add(guesserLabel, 0, row);

            for (int col = 0; col < 5; col++) {

                Label cell = new Label("");

                cell.setMinSize(55, 55);
                cell.setPrefSize(55, 55);
                cell.setMaxSize(55, 55);
                cell.setAlignment(Pos.CENTER);

                cell.getStyleClass().add("wordle-cell");

                cells[row][col] = cell;

                grid.add(cell, col + 1, row);
            }
        }

        VBox progressPanel = createProgressPanel();

        JmsEventFeed jmsEventFeed =
                new JmsEventFeed(gameSession);

        VBox eventsPanel = new VBox(
                10,
                eventFeed,
                jmsEventFeed
        );

        eventsPanel.setAlignment(Pos.TOP_CENTER);

        HBox gameLayout = new HBox(
                20,
                progressPanel,
                grid,
                eventsPanel
        );

        gameLayout.setAlignment(Pos.CENTER);

        guessField.setPromptText(
                "Enter a 5-letter word"
        );

        guessField.setMaxWidth(250);

        submitButton.setPrefWidth(250);
        submitButton.getStyleClass().add("neon-button");

        keyboard = new OnScreenKeyboard(key -> {

            if (!canSubmitGuess()) {
                return;
            }

            if ("ENTER".equals(key)) {

                submitGuess();

            } else if ("BACKSPACE".equals(key)) {

                String current = guessField.getText();

                if (!current.isEmpty()) {

                    guessField.setText(
                            current.substring(
                                    0,
                                    current.length() - 1
                            )
                    );
                }

            } else if (guessField.getText().length() < 5) {

                guessField.appendText(key);
            }
        });

        submitButton.setOnAction(
                event -> submitGuess()
        );

        guessField.setOnAction(
                event -> submitGuess()
        );

        VBox guessControls = new VBox(
                12,
                guessField,
                keyboard,
                submitButton
        );

        guessControls.setAlignment(Pos.CENTER);



        messageLabel.getStyleClass().add("waiting-status");
        messageLabel.setWrapText(true);
        messageLabel.setAlignment(Pos.CENTER);

        messageLabel.setText(
                "Enter your first guess."
        );

        backButton.getStyleClass().add("secondary-button");
        backButton.setPrefWidth(250);
        backButton.setDisable(true);

        backButton.setOnAction(event -> {

            if (leaveHandler != null) {
                leaveHandler.run();
            }
        });

        getChildren().addAll(
                title,
                gameHeader,
                roleLabel,
                gameLayout,
                guessControls,
                messageLabel,
                backButton
        );

        updateOpponentStats(0, 0);
        updateAttempts(0);
        refreshControls();
    }

    private VBox createProgressPanel() {

        VBox panel = new VBox(15);

        panel.getStyleClass().add("cooperative-panel");

        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(20));
        panel.setPrefWidth(240);
        panel.setMaxWidth(260);

        Label title = new Label("PLAYER PROGRESS");

        title.getStyleClass().add("cooperative-title");

        Label playerLabel = new Label(
                gameSession.getUsername()
        );

        playerLabel.getStyleClass().add(
                "cooperative-players"
        );

        playerLabel.setWrapText(true);

        attemptsLabel.getStyleClass().add(
                "cooperative-attempts"
        );

        remainingLabel.getStyleClass().add(
                "cooperative-status"
        );

        progressBar.setPrefWidth(200);

        progressBar.getStyleClass().add(
                "cooperative-progress"
        );

        lastGuessLabel.setText("Last guess: None");

        lastGuessLabel.getStyleClass().add(
                "cooperative-last-guess"
        );

        lastGuessLabel.setWrapText(true);

        panel.getChildren().addAll(
                title,
                playerLabel,
                attemptsLabel,
                remainingLabel,
                progressBar,
                lastGuessLabel,
                ownScore,
                opponentProgress,
                opponentScore
        );

        return panel;
    }

    private void submitGuess() {

        if (!canSubmitGuess()) {
            return;
        }

        String guess = guessField.getText()
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!guess.matches("[A-Z]{5}")) {

            messageLabel.setText(
                    "Enter exactly 5 English letters."
            );

            return;
        }

        guessPending = true;

        refreshControls();

        messageLabel.setText("Submitting guess...");

        try {

            guessHandler.accept(guess);

        } catch (RuntimeException exception) {

            showError("Unable to submit guess.");
        }
    }

    private boolean canSubmitGuess() {

        return connected
                && !guessPending
                && !roundFinished
                && attemptsUsed < maxAttempts
                && guessHandler != null
                && gameSession.getTcpClient().isConnected();
    }

    private void refreshControls() {

        boolean disabled = !canSubmitGuess();

        guessField.setDisable(disabled);
        submitButton.setDisable(disabled);
        keyboard.setKeyboardDisabled(disabled);

        backButton.setDisable(
                !finalResultReceived
                        || !connected
                        || !gameSession.getTcpClient().isConnected()
                        || leaveHandler == null
        );
    }

    public void displayGuess(
            String word,
            String[] feedback,
            int attemptNumber,
            String playerId,
            String playerUsername,
            boolean solved
    ) {

        if (!gameSession.getPlayerId().equals(playerId)
                || word == null
                || !word.matches("[A-Z]{5}")
                || feedback == null
                || feedback.length != 5
                || attemptNumber < 1
                || attemptNumber > maxAttempts) {

            return;
        }

        int[] values = new int[5];

        for (int col = 0; col < 5; col++) {

            values[col] = switch (feedback[col]) {

                case "CORRECT" -> 2;
                case "PRESENT" -> 1;
                case "ABSENT" -> 0;
                default -> -1;
            };

            if (values[col] < 0) {

                messageLabel.setText(
                        "Invalid letter feedback."
                );

                return;
            }
        }

        int row = attemptNumber - 1;

        if (!cells[row][0].getText().isEmpty()) {
            return;
        }

        guesserLabels[row].setText(playerUsername);

        for (int col = 0; col < 5; col++) {

            Label cell = cells[row][col];

            String letter =
                    String.valueOf(word.charAt(col));

            cell.setText(letter);

            String color = switch (values[col]) {

                case 2 -> "#16a34a";
                case 1 -> "#ca8a04";
                default -> "#64748b";
            };

            cell.setStyle(
                    "-fx-background-color: " + color + ";"
                            + "-fx-border-color: " + color + ";"
                            + "-fx-background-radius: 8;"
                            + "-fx-border-radius: 8;"
                            + "-fx-text-fill: white;"
                            + "-fx-font-size: 24px;"
                            + "-fx-font-weight: bold;"
            );

            keyboard.updateKeyFeedback(
                    letter,
                    values[col]
            );
        }

        updateAttempts(
                Math.max(attemptsUsed, attemptNumber)
        );

        lastGuessLabel.setText(
                "Last guess: "
                        + playerUsername
                        + " - "
                        + word
        );

        eventFeed.addEvent(
                playerUsername + " guessed " + word
        );

        if (gameSession.getPlayerId().equals(playerId)) {

            guessPending = false;

            guessField.clear();
        }

        if (!roundFinished && !finalResultReceived) {
            messageLabel.setText(playerUsername + " guessed " + word);
        }

        if (finalResultReceived) {
            refreshControls();
            return;
        }

        if (roundFinished || solved || attemptsUsed >= maxAttempts) {

            roundFinished = true;

            messageLabel.setText(
                    (solved || attemptsUsed < maxAttempts) ? "Waiting for the final result..." : "No attempts remaining. Waiting for opponent or final result..."
            );

            guessField.setDisable(true);
            submitButton.setDisable(true);
            keyboard.setKeyboardDisabled(true);
            backButton.setDisable(true);

        } else {

            refreshControls();
        }
    }

    public void updateAttempts(int attempts) {

        attemptsUsed = Math.max(
                attemptsUsed,
                Math.min(attempts, maxAttempts)
        );

        attemptsLabel.setText(
                "Your attempts: "
                        + attemptsUsed
                        + " / "
                        + maxAttempts
        );

        remainingLabel.setText(
                "Remaining: "
                        + (maxAttempts - attemptsUsed)
        );

        progressBar.setProgress(
                (double) attemptsUsed / maxAttempts
        );
        refreshResults();

        refreshControls();
    }

    public void setGuessHandler(
            Consumer<String> guessHandler
    ) {

        this.guessHandler = guessHandler;

        refreshControls();
    }

    public void setLeaveHandler(Runnable leaveHandler) {

        this.leaveHandler = leaveHandler;

        refreshControls();
    }

    public void showError(String message) {

        guessPending = false;

        messageLabel.setText(message);

        refreshControls();
    }

    public void updatePlayerScore(int score) {
        playerScoreValue = Math.max(playerScoreValue, score);
        ownScore.setText("Your score: " + playerScoreValue);
        refreshResults();
    }

    public void updateOpponentStats(int attempts, int score) {
        if (attempts < 0 || attempts > maxAttempts || score < 0) return;
        opponentAttemptsValue = Math.max(opponentAttemptsValue, attempts);
        opponentScoreValue = Math.max(opponentScoreValue, score);
        opponentProgress.setText(opponentUsername + ": " + opponentAttemptsValue + " / " + maxAttempts + " attempts");
        opponentScore.setText("Opponent score: " + opponentScoreValue);
        refreshResults();
    }

    public void awaitFinalResult() {
        if (finalResultReceived) return;
        roundFinished = true;
        messageLabel.setText("Waiting for the final result...");
        refreshControls();
    }

    public void finishRound(String message) {
        finalResultReceived = true;

        roundFinished = true;
        guessPending = false;

        gameHeader.updateStatus("FINISHED");

        messageLabel.setText(message);

        eventFeed.addEvent(message);

        refreshControls();
    }

    public void showFinalResults(String[] fields) {
        if (!finalResultReceived || fields == null || fields.length != 7 || resultsView != null) return;
        boolean first = gameSession.getPlayerId().equals(fields[3]);
        if (!first && !gameSession.getPlayerId().equals(fields[5])) return;
        finalTargetWord = first ? fields[4] : fields[6];
        finalOwnWord = first ? fields[6] : fields[4];
        finalOutcome = "NO_WINNER".equals(fields[1]) ? "DRAW"
                : gameSession.getPlayerId().equals(fields[2]) ? "WIN" : "LOSS";
        resultsView = new GameResultsView();
        getChildren().add(getChildren().indexOf(backButton), resultsView);
        refreshResults();
    }

    private void refreshResults() {
        if (resultsView == null) return;
        resultsView.showDoubleHostResult(finalOutcome, finalOwnWord, finalTargetWord,
                gameSession.getUsername(), playerScoreValue, attemptsUsed,
                opponentUsername, opponentScoreValue, opponentAttemptsValue, maxAttempts);
    }

    public void handleConnectionFailure(String message) {

        connected = false;
        guessPending = false;

        gameHeader.updateConnectionStatus(
                "DISCONNECTED"
        );

        gameHeader.updateStatus("INTERRUPTED");

        messageLabel.setText(message);

        eventFeed.addEvent(
                "Disconnected from the server."
        );

        refreshControls();
    }

    public String getRoomId() {
        return roomId;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public int getAttemptsUsed() {
        return attemptsUsed;
    }
}

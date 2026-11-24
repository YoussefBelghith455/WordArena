package com.wordarena.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class GameResultsView extends VBox {

    private final Label resultTitle = new Label();
    private final Label resultMessage = new Label();
    private final Label wordValue = new Label();
    private final Label attemptsValue = new Label();
    private final Label attemptsLabel = new Label("YOUR ATTEMPTS");
    private final Label hostSummary = new Label();

    private final Label playerName = new Label();
    private final Label playerScore = new Label();
    private final Label playerAttempts = new Label();

    private final Label opponentName = new Label();
    private final Label opponentScore = new Label();
    private final Label opponentAttempts = new Label();

    private final HBox multiplayerStats = new HBox(15);

    public GameResultsView() {
        getStyleClass().add("game-results-panel");
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(30));
        setMaxWidth(500);

        resultTitle.getStyleClass().add("game-results-title");
        resultMessage.getStyleClass().add("game-results-message");

        Label wordLabel = new Label("SECRET WORD");
        wordLabel.getStyleClass().add("game-results-label");

        wordValue.getStyleClass().add("game-results-value");

        VBox wordBox = new VBox(8, wordLabel, wordValue);
        wordBox.setAlignment(Pos.CENTER);
        wordBox.getStyleClass().add("game-results-stat");

        attemptsLabel.getStyleClass().add("game-results-label");

        attemptsValue.getStyleClass().add("game-results-value");

        VBox attemptsBox = new VBox(
                8,
                attemptsLabel,
                attemptsValue
        );

        attemptsBox.setAlignment(Pos.CENTER);
        attemptsBox.getStyleClass().add("game-results-stat");

        HBox stats = new HBox(15, wordBox, attemptsBox);
        stats.setAlignment(Pos.CENTER);

        multiplayerStats.setAlignment(Pos.CENTER);
        multiplayerStats.setVisible(false);
        multiplayerStats.setManaged(false);

        VBox playerBox = createPlayerBox(
                playerName,
                playerScore,
                playerAttempts
        );

        VBox opponentBox = createPlayerBox(
                opponentName,
                opponentScore,
                opponentAttempts
        );

        multiplayerStats.getChildren().addAll(
                playerBox,
                opponentBox
        );

        hostSummary.getStyleClass().add("game-results-label");
        hostSummary.setWrapText(true);
        hostSummary.setVisible(false);
        hostSummary.setManaged(false);

        getChildren().addAll(
                resultTitle,
                resultMessage,
                stats,
                multiplayerStats,
                hostSummary
        );

        showResult(false, "", 0);
    }

    private VBox createPlayerBox(
            Label name,
            Label score,
            Label attempts
    ) {
        name.getStyleClass().add("game-results-label");
        score.getStyleClass().add("game-results-value");
        attempts.getStyleClass().add("game-results-label");

        VBox box = new VBox(
                8,
                name,
                score,
                attempts
        );

        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15));
        box.getStyleClass().add("game-results-stat");
        box.setMinWidth(175);

        return box;
    }

    public void showResult(
            boolean won,
            String secretWord,
            int attempts
    ) {
        resetHostSummary();
        resultTitle.setText("ROUND COMPLETED");

        resultMessage.setText(
                won ? "WORD SOLVED!" : "GAME OVER!"
        );

        updateResultStyle(won ? "WIN" : "LOSS");

        wordValue.setText(secretWord);
        attemptsValue.setText(attempts + " / 6");

        multiplayerStats.setVisible(false);
        multiplayerStats.setManaged(false);
    }

    public void showMultiplayerResult(
            String result,
            String secretWord,
            String username,
            int score,
            int attempts,
            String opponentUsername,
            int opponentFinalScore,
            int opponentFinalAttempts,
            int maxAttempts
    ) {
        resetHostSummary();
        resultTitle.setText("DUEL COMPLETED");

        switch (result) {
            case "WIN" -> resultMessage.setText("VICTORY!");
            case "LOSS" -> resultMessage.setText("DEFEAT!");
            case "DRAW" -> resultMessage.setText("DRAW!");
            default -> resultMessage.setText("GAME OVER!");
        }

        updateResultStyle(result);

        wordValue.setText(secretWord);
        attemptsValue.setText(
                attempts + " / " + maxAttempts
        );

        playerName.setText(
                username + ("WIN".equals(result) ? " â˜…" : "")
        );

        playerScore.setText(
                score + " POINTS"
        );

        playerAttempts.setText(
                attempts + " / " + maxAttempts + " attempts"
        );

        opponentName.setText(
                opponentUsername
                        + ("LOSS".equals(result) ? " â˜…" : "")
        );

        opponentScore.setText(
                opponentFinalScore + " POINTS"
        );

        opponentAttempts.setText(
                opponentFinalAttempts
                        + " / "
                        + maxAttempts
                        + " attempts"
        );

        multiplayerStats.setVisible(true);
        multiplayerStats.setManaged(true);
    }

    public void showHostResult(boolean host, boolean hostWon,
                               String secretWord, int attempts, int maxAttempts) {
        boolean won = host == hostWon;
        resultTitle.setText("HOST MODE COMPLETED");
        resultMessage.setText(won ? "VICTORY!" : "DEFEAT!");
        updateResultStyle(won ? "WIN" : "LOSS");
        wordValue.setText(secretWord);
        attemptsLabel.setText("SHARED ATTEMPTS");
        attemptsValue.setText(attempts + " / " + maxAttempts);
        multiplayerStats.setVisible(false);
        multiplayerStats.setManaged(false);
        hostSummary.setText("Winning side: " + (hostWon ? "HOST" : "GUESSING TEAM")
                + "\nPoints earned: +" + (won ? 600 : 0));
        hostSummary.setVisible(true);
        hostSummary.setManaged(true);
    }

    public void showDoubleHostResult(String result, String ownWord, String targetWord,
                                     String username, int score, int attempts,
                                     String opponentUsername, int opponentFinalScore,
                                     int opponentFinalAttempts, int maxAttempts) {
        showMultiplayerResult(result, targetWord, username, score, attempts,
                opponentUsername, opponentFinalScore, opponentFinalAttempts, maxAttempts);
        resultTitle.setText("DOUBLE HOST COMPLETED");
        playerName.setText(username + " (YOU)");
        opponentName.setText(opponentUsername);
        hostSummary.setText("Your secret word: " + ownWord + "\nOpponent's secret word: " + targetWord);
        hostSummary.setVisible(true);
        hostSummary.setManaged(true);
    }

    public void showCooperativeResult(boolean won, String secretWord, int attempts, int maxAttempts) {
        resetHostSummary();
        resultTitle.setText("COOPERATIVE COMPLETED");
        resultMessage.setText(won ? "TEAM VICTORY!" : "TEAM DEFEAT!");
        updateResultStyle(won ? "WIN" : "LOSS");
        wordValue.setText(secretWord);
        attemptsLabel.setText("SHARED ATTEMPTS");
        attemptsValue.setText(attempts + " / " + maxAttempts);
        multiplayerStats.setVisible(false);
        multiplayerStats.setManaged(false);
        hostSummary.setText("Points earned per player: +" + (won ? 600 : 0));
        hostSummary.setVisible(true);
        hostSummary.setManaged(true);
    }

    private void resetHostSummary() {
        attemptsLabel.setText("YOUR ATTEMPTS");
        hostSummary.setVisible(false);
        hostSummary.setManaged(false);
    }

    private void updateResultStyle(String result) {
        resultMessage.getStyleClass().removeAll(
                "game-results-win",
                "game-results-loss"
        );

        if ("WIN".equals(result)) {
            resultMessage.getStyleClass().add(
                    "game-results-win"
            );
        } else if ("LOSS".equals(result)) {
            resultMessage.getStyleClass().add(
                    "game-results-loss"
            );
        }
    }
}


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

        Label attemptsLabel = new Label("YOUR ATTEMPTS");
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

        getChildren().addAll(
                resultTitle,
                resultMessage,
                stats,
                multiplayerStats
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
                username + ("WIN".equals(result) ? " ★" : "")
        );

        playerScore.setText(
                score + " POINTS"
        );

        playerAttempts.setText(
                attempts + " / " + maxAttempts + " attempts"
        );

        opponentName.setText(
                opponentUsername
                        + ("LOSS".equals(result) ? " ★" : "")
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

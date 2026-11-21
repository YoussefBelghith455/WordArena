
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

    public GameResultsView() {

        getStyleClass().add("game-results-panel");
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(30));
        setMaxWidth(450);

        resultTitle.getStyleClass().add("game-results-title");
        resultMessage.getStyleClass().add("game-results-message");

        Label wordLabel = new Label("SECRET WORD");
        wordLabel.getStyleClass().add("game-results-label");

        wordValue.getStyleClass().add("game-results-value");

        VBox wordBox = new VBox(8, wordLabel, wordValue);
        wordBox.setAlignment(Pos.CENTER);
        wordBox.getStyleClass().add("game-results-stat");

        Label attemptsLabel = new Label("ATTEMPTS");
        attemptsLabel.getStyleClass().add("game-results-label");

        attemptsValue.getStyleClass().add("game-results-value");

        VBox attemptsBox = new VBox(8, attemptsLabel, attemptsValue);
        attemptsBox.setAlignment(Pos.CENTER);
        attemptsBox.getStyleClass().add("game-results-stat");

        HBox stats = new HBox(15, wordBox, attemptsBox);
        stats.setAlignment(Pos.CENTER);

        getChildren().addAll(
                resultTitle,
                resultMessage,
                stats
        );

        showResult(false, "", 0);
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

        resultMessage.getStyleClass().removeAll(
                "game-results-win",
                "game-results-loss"
        );

        resultMessage.getStyleClass().add(
                won ? "game-results-win" : "game-results-loss"
        );

        wordValue.setText(secretWord);
        attemptsValue.setText(attempts + " / 6");
    }
}


package com.wordarena.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class CooperativePanelView extends VBox {

    private final Label playersLabel = new Label();
    private final Label attemptsLabel = new Label();
    private final Label statusLabel = new Label();
    private final Label lastGuessLabel = new Label();
    private final ProgressBar progressBar = new ProgressBar(0);

    public CooperativePanelView(
            String username,
            String teammateUsername
    ) {

        getStyleClass().add("cooperative-panel");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(18);
        setPadding(new Insets(20));
        setPrefWidth(260);
        setMaxWidth(300);

        Label title = new Label("TEAM PROGRESS");
        title.getStyleClass().add("cooperative-title");

        Label subtitle = new Label("COOPERATIVE MODE");
        subtitle.getStyleClass().add("cooperative-subtitle");

        playersLabel.setText(
                username + " + " + teammateUsername
        );
        playersLabel.getStyleClass().add("cooperative-players");
        playersLabel.setWrapText(true);

        attemptsLabel.setText("Shared attempts: 0 / 6");
        attemptsLabel.getStyleClass().add("cooperative-attempts");

        progressBar.setPrefWidth(220);
        progressBar.setProgress(0);
        progressBar.getStyleClass().add("cooperative-progress");

        statusLabel.setText("Waiting for team guesses...");
        statusLabel.getStyleClass().add("cooperative-status");
        statusLabel.setWrapText(true);

        lastGuessLabel.setText("Last guess: None");
        lastGuessLabel.getStyleClass().add("cooperative-last-guess");

        getChildren().addAll(
                title,
                subtitle,
                playersLabel,
                attemptsLabel,
                progressBar,
                statusLabel,
                lastGuessLabel
        );
    }

    public void updateSharedAttempts(int attempts) {

        Platform.runLater(() -> {

            int safeAttempts = Math.max(
                    0,
                    Math.min(attempts, 6)
            );

            attemptsLabel.setText(
                    "Shared attempts: " + safeAttempts + " / 6"
            );

            progressBar.setProgress(safeAttempts / 6.0);
        });
    }

    public void updateLastGuess(String player, String guess) {

        Platform.runLater(() -> {

            lastGuessLabel.setText(
                    "Last guess: " + player + " - " + guess
            );
        });
    }

    public void updateStatus(String status) {

        Platform.runLater(() -> {
            statusLabel.setText(status);
        });
    }
}

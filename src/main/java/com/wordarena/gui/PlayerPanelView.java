
package com.wordarena.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class PlayerPanelView extends VBox {

    private final Label playerScore = new Label("0");
    private final Label opponentScore = new Label("0");

    private final ProgressBar playerProgress = new ProgressBar(0);
    private final ProgressBar opponentProgress = new ProgressBar(0);

    private final Label opponentName = new Label("Waiting for player...");

    public PlayerPanelView(String username) {

        getStyleClass().add("player-panel");
        setSpacing(18);
        setPadding(new Insets(20));
        setPrefWidth(240);
        setMaxWidth(260);

        Label title = new Label("PLAYERS");
        title.getStyleClass().add("player-panel-title");

        VBox currentPlayer = createPlayerCard(
                username,
                "YOU",
                playerScore,
                playerProgress
        );

        VBox opponent = createPlayerCard(
                "Waiting for player...",
                "OPPONENT",
                opponentScore,
                opponentProgress
        );

        Label displayedOpponentName = (Label) opponent.getChildren().get(0);
        displayedOpponentName.textProperty().bind(opponentName.textProperty());

        getChildren().addAll(
                title,
                currentPlayer,
                opponent
        );
    }

    private VBox createPlayerCard(
            String username,
            String role,
            Label score,
            ProgressBar progress
    ) {

        Label nameLabel = new Label(username);
        nameLabel.getStyleClass().add("player-name");
        nameLabel.setWrapText(true);

        Label roleLabel = new Label(role);
        roleLabel.getStyleClass().add("player-role");

        Label scoreTitle = new Label("Score");
        scoreTitle.getStyleClass().add("player-score-title");

        score.getStyleClass().add("player-score");

        HBox scoreRow = new HBox(
                8,
                scoreTitle,
                score
        );
        scoreRow.setAlignment(Pos.CENTER_LEFT);

        progress.setProgress(0);
        progress.setPrefWidth(190);
        progress.getStyleClass().add("player-progress");

        VBox card = new VBox(
                10,
                nameLabel,
                roleLabel,
                scoreRow,
                progress
        );

        card.getStyleClass().add("player-card");

        return card;
    }

    public void updatePlayerScore(int score) {
        playerScore.setText(String.valueOf(score));
    }

    public void updateOpponentScore(int score) {
        opponentScore.setText(String.valueOf(score));
    }

    public void updatePlayerProgress(double progress) {
        playerProgress.setProgress(
                Math.max(0, Math.min(1, progress))
        );
    }

    public void updateOpponentProgress(double progress) {
        opponentProgress.setProgress(
                Math.max(0, Math.min(1, progress))
        );
    }

    public void updateOpponentName(String name) {
        opponentName.setText(name);
    }
}

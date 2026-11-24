package com.wordarena.gui;

import com.wordarena.client.tcp.GameSession;
import com.wordarena.common.PlayerStats;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Locale;

public class PlayerStatisticsView extends VBox {

    private final String username;
    private final GameSession gameSession;

    private final Label playerNameLabel = new Label();

    private final Label gamesPlayedLabel = new Label("--");
    private final Label gamesWonLabel = new Label("--");
    private final Label gamesLostLabel = new Label("--");
    private final Label totalScoreLabel = new Label("--");

    private final Label winRateLabel = new Label("--");

    private final ProgressBar winRateBar =
            new ProgressBar(0);

    private final Label messageLabel = new Label();

    private final Button refreshButton =
            new Button("REFRESH");

    private boolean loading;
    private long requestId;

    public PlayerStatisticsView(
            String username,
            GameSession gameSession
    ) {

        this.username = username;
        this.gameSession = gameSession;

        getStyleClass().add("lobby-root");

        setAlignment(Pos.CENTER);
        setSpacing(15);
        setPadding(new Insets(25));

        Label title = new Label("WORDARENA");
        title.getStyleClass().add("lobby-title");

        Label subtitle = new Label("MY STATISTICS");
        subtitle.getStyleClass().add("section-title");

        playerNameLabel.setText(username);

        playerNameLabel.setStyle(
                "-fx-text-fill: #DDD6FE;"
                        + "-fx-font-size: 24px;"
                        + "-fx-font-weight: bold;"
        );

        VBox gamesPlayedCard = createStatisticsCard(
                "GAMES PLAYED",
                gamesPlayedLabel,
                "#C4B5FD"
        );

        VBox gamesWonCard = createStatisticsCard(
                "GAMES WON",
                gamesWonLabel,
                "#4ADE80"
        );

        VBox gamesLostCard = createStatisticsCard(
                "GAMES LOST",
                gamesLostLabel,
                "#F87171"
        );

        VBox totalScoreCard = createStatisticsCard(
                "TOTAL SCORE",
                totalScoreLabel,
                "#FACC15"
        );

        GridPane statisticsGrid = new GridPane();

        statisticsGrid.setAlignment(Pos.CENTER);
        statisticsGrid.setHgap(15);
        statisticsGrid.setVgap(15);
        statisticsGrid.setMaxWidth(600);

        statisticsGrid.add(gamesPlayedCard, 0, 0);
        statisticsGrid.add(gamesWonCard, 1, 0);
        statisticsGrid.add(gamesLostCard, 0, 1);
        statisticsGrid.add(totalScoreCard, 1, 1);

        GridPane.setHgrow(
                gamesPlayedCard,
                Priority.ALWAYS
        );

        GridPane.setHgrow(
                gamesWonCard,
                Priority.ALWAYS
        );

        GridPane.setHgrow(
                gamesLostCard,
                Priority.ALWAYS
        );

        GridPane.setHgrow(
                totalScoreCard,
                Priority.ALWAYS
        );

        Label winRateTitle = new Label("WIN RATE");

        winRateTitle.setStyle(
                "-fx-text-fill: #A78BFA;"
                        + "-fx-font-weight: bold;"
                        + "-fx-font-size: 13px;"
        );

        winRateLabel.setStyle(
                "-fx-text-fill: #4ADE80;"
                        + "-fx-font-size: 26px;"
                        + "-fx-font-weight: bold;"
        );

        winRateBar.setMaxWidth(Double.MAX_VALUE);
        winRateBar.setPrefHeight(15);

        winRateBar.setStyle(
                "-fx-accent: #4ADE80;"
        );

        VBox winRateCard = new VBox(
                10,
                winRateTitle,
                winRateLabel,
                winRateBar
        );

        winRateCard.setAlignment(Pos.CENTER);
        winRateCard.setPadding(new Insets(18));
        winRateCard.setMaxWidth(600);

        winRateCard.setStyle(
                "-fx-background-color: #20172F;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: #50396D;"
                        + "-fx-border-radius: 10;"
        );

        refreshButton.getStyleClass()
                .add("neon-button");

        refreshButton.setPrefWidth(150);

        messageLabel.setWrapText(true);
        messageLabel.setAlignment(Pos.CENTER);

        messageLabel.setStyle(
                "-fx-text-fill: #C4B5FD;"
                        + "-fx-font-size: 12px;"
        );

        Label rmiStatusLabel = new Label();

        rmiStatusLabel.getStyleClass()
                .add("lobby-welcome");

        if (gameSession != null) {

            rmiStatusLabel.textProperty().bind(
                    Bindings.concat(
                            "RMI CONNECTION: ",
                            gameSession.rmiStatusProperty()
                    )
            );

            rmiStatusLabel.styleProperty().bind(
                    Bindings.createStringBinding(
                            () -> getConnectionStyle(
                                    gameSession.getRmiStatus()
                            ),
                            gameSession.rmiStatusProperty()
                    )
            );

        } else {

            rmiStatusLabel.setText(
                    "RMI CONNECTION: DISCONNECTED"
            );

            rmiStatusLabel.setStyle(
                    getConnectionStyle("DISCONNECTED")
            );
        }

        Button backButton = new Button(
                "BACK TO LOBBY"
        );

        backButton.getStyleClass()
                .add("secondary-button");

        backButton.setPrefWidth(250);

        refreshButton.setOnAction(
                event -> loadStatistics()
        );

        backButton.setOnAction(
                event -> returnToLobby()
        );

        getChildren().addAll(
                title,
                subtitle,
                playerNameLabel,
                statisticsGrid,
                winRateCard,
                refreshButton,
                messageLabel,
                rmiStatusLabel,
                backButton
        );

        sceneProperty().addListener(
                (observable, oldScene, newScene) -> {

                    if (newScene != null) {

                        loadStatistics();

                    } else {

                        requestId++;
                        loading = false;
                        refreshButton.setDisable(false);
                    }
                }
        );
    }

    private VBox createStatisticsCard(
            String title,
            Label valueLabel,
            String valueColor
    ) {

        Label titleLabel = new Label(title);

        titleLabel.setStyle(
                "-fx-text-fill: #A5A0BC;"
                        + "-fx-font-size: 12px;"
                        + "-fx-font-weight: bold;"
        );

        valueLabel.setStyle(
                "-fx-text-fill: " + valueColor + ";"
                        + "-fx-font-size: 30px;"
                        + "-fx-font-weight: bold;"
        );

        VBox card = new VBox(
                10,
                titleLabel,
                valueLabel
        );

        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(18));

        card.setPrefWidth(280);
        card.setMinWidth(150);
        card.setMaxWidth(Double.MAX_VALUE);

        card.setStyle(
                "-fx-background-color: #20172F;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: #50396D;"
                        + "-fx-border-radius: 10;"
        );

        return card;
    }

    private void loadStatistics() {

        if (loading) {
            return;
        }

        if (gameSession == null
                || !gameSession.isRmiConnected()) {

            clearStatistics();

            messageLabel.setText(
                    "RMI service unavailable. Try refreshing."
            );

            return;
        }

        loading = true;

        long currentRequest = ++requestId;

        refreshButton.setDisable(true);

        messageLabel.setText(
                "Loading player statistics..."
        );

        gameSession.getMyStats()
                .whenComplete((statistics, exception) ->
                        Platform.runLater(() -> {

                            if (currentRequest != requestId
                                    || getScene() == null) {

                                return;
                            }

                            loading = false;

                            refreshButton.setDisable(false);

                            if (exception != null) {

                                clearStatistics();

                                messageLabel.setText(
                                        "Unable to load statistics. "
                                                + "Please try again."
                                );

                                return;
                            }

                            displayStatistics(statistics);
                        })
                );
    }

    private void displayStatistics(
            PlayerStats statistics
    ) {

        if (statistics == null) {

            clearStatistics();

            messageLabel.setText(
                    "No statistics available yet."
            );

            return;
        }

        playerNameLabel.setText(
                statistics.getUsername()
        );

        gamesPlayedLabel.setText(
                String.valueOf(
                        statistics.getGamesPlayed()
                )
        );

        gamesWonLabel.setText(
                String.valueOf(
                        statistics.getGamesWon()
                )
        );

        gamesLostLabel.setText(
                String.valueOf(
                        statistics.getGamesLost()
                )
        );

        totalScoreLabel.setText(
                String.valueOf(
                        statistics.getTotalScore()
                )
        );

        double winRate = 0;

        if (statistics.getGamesPlayed() > 0) {

            winRate =
                    (double) statistics.getGamesWon()
                            / statistics.getGamesPlayed();
        }

        winRateLabel.setText(
                String.format(
                        Locale.ROOT,
                        "%.1f%%",
                        winRate * 100
                )
        );

        winRateBar.setProgress(
                Math.max(0, Math.min(1, winRate))
        );

        messageLabel.setText(
                statistics.getGamesPlayed() == 0
                        ? "Play your first game to build your statistics."
                        : "Statistics updated successfully."
        );
    }

    private void clearStatistics() {

        playerNameLabel.setText(username);

        gamesPlayedLabel.setText("--");
        gamesWonLabel.setText("--");
        gamesLostLabel.setText("--");
        totalScoreLabel.setText("--");

        winRateLabel.setText("--");
        winRateBar.setProgress(0);
    }

    private String getConnectionStyle(
            String status
    ) {

        return switch (status) {

            case "CONNECTED" ->
                    "-fx-text-fill: #4ADE80;";

            case "CONNECTING" ->
                    "-fx-text-fill: #FACC15;";

            default ->
                    "-fx-text-fill: #F87171;";
        };
    }

    private void returnToLobby() {

        requestId++;

        LobbyView lobby = new LobbyView(
                username,
                gameSession
        );

        Scene scene = new Scene(
                lobby,
                800,
                650
        );

        applyTheme(scene);

        Stage stage =
                (Stage) getScene().getWindow();

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
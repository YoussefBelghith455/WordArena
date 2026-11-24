package com.wordarena.gui;

import com.wordarena.client.tcp.GameSession;
import com.wordarena.common.ScoreEntry;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;

public class LeaderboardView extends VBox {

    private final String username;
    private final GameSession gameSession;

    private final ListView<ScoreEntry> leaderboardList =
            new ListView<>();

    private final Label messageLabel = new Label();

    private final Button refreshButton =
            new Button("REFRESH");

    private boolean loading;
    private long requestId;

    public LeaderboardView(
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

        Label subtitle = new Label(
                "GLOBAL LEADERBOARD"
        );

        subtitle.getStyleClass().add("section-title");

        Label description = new Label(
                "Discover the highest-ranked players."
        );

        description.getStyleClass().add("lobby-subtitle");

        refreshButton.getStyleClass().add("neon-button");
        refreshButton.setPrefWidth(150);

        Label rankHeader = new Label("RANK");

        rankHeader.setPrefWidth(75);
        rankHeader.setStyle(
                "-fx-text-fill: #A78BFA;"
                        + "-fx-font-weight: bold;"
        );

        Label playerHeader = new Label("PLAYER");

        playerHeader.setStyle(
                "-fx-text-fill: #A78BFA;"
                        + "-fx-font-weight: bold;"
        );

        Label scoreHeader = new Label("SCORE");

        scoreHeader.setStyle(
                "-fx-text-fill: #A78BFA;"
                        + "-fx-font-weight: bold;"
        );

        Region headerSpacer = new Region();

        HBox.setHgrow(
                headerSpacer,
                Priority.ALWAYS
        );

        HBox tableHeader = new HBox(
                15,
                rankHeader,
                playerHeader,
                headerSpacer,
                scoreHeader
        );

        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setPadding(new Insets(10, 20, 10, 20));

        leaderboardList.setPrefHeight(320);
        leaderboardList.setMinHeight(180);

        leaderboardList.setStyle(
                "-fx-background-color: #151020;"
                        + "-fx-border-color: #50396D;"
                        + "-fx-border-radius: 8;"
                        + "-fx-background-radius: 8;"
        );

        Label placeholder = new Label(
                "No rankings available."
        );

        placeholder.setStyle(
                "-fx-text-fill: #A5A0BC;"
        );

        leaderboardList.setPlaceholder(placeholder);

        leaderboardList.setCellFactory(
                list -> new LeaderboardCell()
        );

        VBox leaderboardPanel = new VBox(
                5,
                tableHeader,
                leaderboardList
        );

        leaderboardPanel.setMaxWidth(650);

        VBox.setVgrow(
                leaderboardList,
                Priority.ALWAYS
        );

        VBox.setVgrow(
                leaderboardPanel,
                Priority.ALWAYS
        );

        messageLabel.setWrapText(true);
        messageLabel.setAlignment(Pos.CENTER);

        messageLabel.setStyle(
                "-fx-text-fill: #C4B5FD;"
                        + "-fx-font-size: 12px;"
        );

        Label rmiStatusLabel = new Label();

        rmiStatusLabel.getStyleClass()
                .add("lobby-welcome");

        Button backButton = new Button(
                "BACK TO LOBBY"
        );

        backButton.getStyleClass()
                .add("secondary-button");

        backButton.setPrefWidth(250);

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

        refreshButton.setOnAction(
                event -> loadLeaderboard()
        );

        backButton.setOnAction(
                event -> returnToLobby()
        );

        getChildren().addAll(
                title,
                subtitle,
                description,
                refreshButton,
                leaderboardPanel,
                messageLabel,
                rmiStatusLabel,
                backButton
        );

        sceneProperty().addListener(
                (observable, oldScene, newScene) -> {

                    if (newScene != null) {

                        loadLeaderboard();

                    } else {

                        requestId++;
                        loading = false;
                        refreshButton.setDisable(false);
                    }
                }
        );
    }

    private void loadLeaderboard() {

        if (loading) {
            return;
        }

        if (gameSession == null
                || !gameSession.isRmiConnected()) {

            leaderboardList.getItems().clear();

            messageLabel.setText(
                    "RMI service unavailable. Try refreshing."
            );

            return;
        }

        loading = true;

        long currentRequest = ++requestId;

        refreshButton.setDisable(true);

        messageLabel.setText(
                "Loading leaderboard..."
        );

        gameSession.getLeaderboard()
                .whenComplete((entries, exception) ->
                        Platform.runLater(() -> {

                            if (currentRequest != requestId
                                    || getScene() == null) {

                                return;
                            }

                            loading = false;

                            refreshButton.setDisable(false);

                            if (exception != null) {

                                leaderboardList.getItems()
                                        .clear();

                                messageLabel.setText(
                                        "Unable to load leaderboard. "
                                                + "Please try again."
                                );

                                return;
                            }

                            displayLeaderboard(entries);
                        })
                );
    }

    private void displayLeaderboard(
            List<ScoreEntry> entries
    ) {

        if (entries == null || entries.isEmpty()) {

            leaderboardList.getItems().clear();

            messageLabel.setText(
                    "No rankings available yet."
            );

            return;
        }

        List<ScoreEntry> sortedEntries = entries.stream()
                .sorted(
                        Comparator.comparingInt(
                                ScoreEntry::getRank
                        )
                )
                .toList();

        leaderboardList.setItems(
                FXCollections.observableArrayList(
                        sortedEntries
                )
        );

        messageLabel.setText(
                sortedEntries.size()
                        + " ranked player(s)"
        );
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

    private class LeaderboardCell
            extends ListCell<ScoreEntry> {

        @Override
        protected void updateItem(
                ScoreEntry entry,
                boolean empty
        ) {

            super.updateItem(entry, empty);

            setText(null);
            setGraphic(null);

            setStyle(
                    "-fx-background-color: transparent;"
                            + "-fx-padding: 4;"
            );

            if (empty || entry == null) {
                return;
            }

            String rankColor = switch (entry.getRank()) {

                case 1 -> "#FACC15";
                case 2 -> "#CBD5E1";
                case 3 -> "#D97745";
                default -> "#A78BFA";
            };

            Label rankLabel = new Label(
                    "#" + entry.getRank()
            );

            rankLabel.setPrefWidth(75);

            rankLabel.setStyle(
                    "-fx-text-fill: " + rankColor + ";"
                            + "-fx-font-size: 18px;"
                            + "-fx-font-weight: bold;"
            );

            boolean currentPlayer =
                    gameSession != null
                            && gameSession.getPlayerId()
                            .equals(entry.getPlayerId());

            Label usernameLabel = new Label(
                    entry.getUsername()
                            + (currentPlayer ? " (YOU)" : "")
            );

            usernameLabel.setStyle(
                    "-fx-text-fill: #DDD6FE;"
                            + "-fx-font-size: 14px;"
                            + "-fx-font-weight: bold;"
            );

            Label scoreLabel = new Label(
                    String.valueOf(entry.getScore())
            );

            scoreLabel.setStyle(
                    "-fx-text-fill: #C4B5FD;"
                            + "-fx-font-size: 16px;"
                            + "-fx-font-weight: bold;"
            );

            Region spacer = new Region();

            HBox.setHgrow(
                    spacer,
                    Priority.ALWAYS
            );

            HBox playerRow = new HBox(
                    15,
                    rankLabel,
                    usernameLabel,
                    spacer,
                    scoreLabel
            );

            playerRow.setAlignment(Pos.CENTER_LEFT);

            playerRow.setPadding(new Insets(15));

            playerRow.setStyle(
                    "-fx-background-color: #20172F;"
                            + "-fx-background-radius: 8;"
                            + "-fx-border-radius: 8;"
                            + "-fx-border-color: "
                            + (currentPlayer
                            ? "#A78BFA;"
                            : "#50396D;")
            );

            setGraphic(playerRow);
        }
    }
}
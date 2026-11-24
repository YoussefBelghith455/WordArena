package com.wordarena.gui;

import com.wordarena.client.tcp.GameSession;
import com.wordarena.common.RoomInfo;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LobbyView extends VBox {

    private final GameSession gameSession;

    public LobbyView(String username) {
        this(username, null);
    }

    public LobbyView(
            String username,
            GameSession gameSession
    ) {

        this.gameSession = gameSession;

        getStyleClass().add("lobby-root");

        setAlignment(Pos.CENTER);
        setSpacing(10);
        setPadding(new Insets(15));

        Label title = new Label("WORDARENA");
        title.getStyleClass().add("lobby-title");

        Label subtitle = new Label(
                "MULTIPLAYER WORD BATTLE"
        );

        subtitle.getStyleClass().add("lobby-subtitle");

        Label welcome = new Label(
                "Welcome, " + username + "!"
        );

        welcome.getStyleClass().add("lobby-welcome");

        Label roomsLabel = new Label(
                "MULTIPLAYER LOBBY"
        );

        roomsLabel.getStyleClass().add("section-title");

        Label informationLabel = new Label(
                "Select an available room, create a room "
                        + "or join using a room ID."
        );

        informationLabel.getStyleClass()
                .add("lobby-subtitle");

        informationLabel.setWrapText(true);
        informationLabel.setAlignment(Pos.CENTER);
        informationLabel.setMaxWidth(600);

        PublicRoomsView publicRoomsView =
                new PublicRoomsView(gameSession);

        Label connectionLabel = new Label();

        connectionLabel.getStyleClass()
                .add("lobby-welcome");

        boolean connected = isSessionConnected();

        connectionLabel.setText(
                connected
                        ? "SERVER CONNECTION: CONNECTED"
                        : "SERVER CONNECTION: DISCONNECTED"
        );

        connectionLabel.setStyle(
                getConnectionStatusStyle(
                        connected
                                ? "CONNECTED"
                                : "DISCONNECTED"
                )
        );

        Label jmsConnectionLabel = new Label();

        jmsConnectionLabel.getStyleClass()
                .add("lobby-welcome");

        Label rmiConnectionLabel = new Label();

        rmiConnectionLabel.getStyleClass()
                .add("lobby-welcome");

        if (gameSession != null) {

            jmsConnectionLabel.textProperty().bind(
                    Bindings.concat(
                            "JMS CONNECTION: ",
                            gameSession.jmsStatusProperty()
                    )
            );

            jmsConnectionLabel.styleProperty().bind(
                    Bindings.createStringBinding(
                            () -> getConnectionStatusStyle(
                                    gameSession.getJmsStatus()
                            ),
                            gameSession.jmsStatusProperty()
                    )
            );

            rmiConnectionLabel.textProperty().bind(
                    Bindings.concat(
                            "RMI CONNECTION: ",
                            gameSession.rmiStatusProperty()
                    )
            );

            rmiConnectionLabel.styleProperty().bind(
                    Bindings.createStringBinding(
                            () -> getConnectionStatusStyle(
                                    gameSession.getRmiStatus()
                            ),
                            gameSession.rmiStatusProperty()
                    )
            );

        } else {

            jmsConnectionLabel.setText(
                    "JMS CONNECTION: DISCONNECTED"
            );

            jmsConnectionLabel.setStyle(
                    getConnectionStatusStyle("DISCONNECTED")
            );

            rmiConnectionLabel.setText(
                    "RMI CONNECTION: DISCONNECTED"
            );

            rmiConnectionLabel.setStyle(
                    getConnectionStatusStyle("DISCONNECTED")
            );
        }

        Button createRoomButton = new Button(
                "CREATE ROOM"
        );

        createRoomButton.getStyleClass()
                .add("neon-button");

        createRoomButton.setPrefWidth(190);
        createRoomButton.setDisable(!connected);

        Button joinRoomButton = new Button(
                "JOIN ROOM BY ID"
        );

        joinRoomButton.getStyleClass()
                .add("neon-button");

        joinRoomButton.setPrefWidth(190);
        joinRoomButton.setDisable(!connected);

        Button joinSelectedRoomButton = new Button(
                "JOIN SELECTED ROOM"
        );

        joinSelectedRoomButton.getStyleClass()
                .add("neon-button");

        joinSelectedRoomButton.setPrefWidth(300);

        Button leaderboardButton = new Button(
                "LEADERBOARD"
        );

        leaderboardButton.getStyleClass()
                .add("secondary-button");

        leaderboardButton.setPrefWidth(190);

        Button statisticsButton = new Button(
                "MY STATISTICS"
        );

        statisticsButton.getStyleClass()
                .add("secondary-button");

        statisticsButton.setPrefWidth(190);

        Label selectionLabel = new Label();

        selectionLabel.setWrapText(true);
        selectionLabel.setAlignment(Pos.CENTER);
        selectionLabel.setMaxWidth(600);

        selectionLabel.setStyle(
                "-fx-text-fill: #C4B5FD;"
                        + "-fx-font-size: 12px;"
        );

        if (gameSession != null) {

            joinSelectedRoomButton.disableProperty().bind(
                    Bindings.createBooleanBinding(
                            () -> !isSessionConnected()
                                    || !gameSession.isRmiConnected()
                                    || !publicRoomsView
                                    .isSelectedRoomJoinable(),
                            publicRoomsView.selectedRoomProperty(),
                            gameSession.rmiStatusProperty(),
                            createRoomButton.disableProperty()
                    )
            );

            selectionLabel.textProperty().bind(
                    Bindings.createStringBinding(
                            () -> getSelectionMessage(
                                    publicRoomsView
                                            .getSelectedRoom()
                            ),
                            publicRoomsView.selectedRoomProperty(),
                            gameSession.rmiStatusProperty()
                    )
            );

            leaderboardButton.disableProperty().bind(
                    Bindings.createBooleanBinding(
                            () -> !gameSession.isRmiConnected(),
                            gameSession.rmiStatusProperty()
                    )
            );

            statisticsButton.disableProperty().bind(
                    Bindings.createBooleanBinding(
                            () -> !gameSession.isRmiConnected(),
                            gameSession.rmiStatusProperty()
                    )
            );

        } else {

            joinSelectedRoomButton.setDisable(true);
            leaderboardButton.setDisable(true);
            statisticsButton.setDisable(true);

            selectionLabel.setText(
                    "No authenticated server connection."
            );
        }

        HBox actions = new HBox(
                15,
                createRoomButton,
                joinRoomButton
        );

        actions.setAlignment(Pos.CENTER);

        HBox statisticsActions = new HBox(
                15,
                leaderboardButton,
                statisticsButton
        );

        statisticsActions.setAlignment(Pos.CENTER);

        VBox header = new VBox(
                5,
                title,
                subtitle,
                welcome
        );

        header.setAlignment(Pos.CENTER);

        VBox roomSection = new VBox(
                6,
                roomsLabel,
                informationLabel,
                publicRoomsView,
                joinSelectedRoomButton,
                selectionLabel
        );

        roomSection.setAlignment(Pos.CENTER);

        VBox connectionSection = new VBox(
                3,
                connectionLabel,
                jmsConnectionLabel,
                rmiConnectionLabel
        );

        connectionSection.setAlignment(Pos.CENTER);

        getChildren().addAll(
                header,
                roomSection,
                actions,
                statisticsActions,
                connectionSection
        );

        createRoomButton.setOnAction(event -> {

            if (!isSessionConnected()) {

                showDisconnected(
                        connectionLabel,
                        createRoomButton,
                        joinRoomButton
                );

                return;
            }

            CreateRoomView createRoomView =
                    new CreateRoomView(
                            username,
                            gameSession
                    );

            Scene scene = new Scene(
                    createRoomView,
                    800,
                    500
            );

            applyTheme(scene);

            Stage stage =
                    (Stage) getScene().getWindow();

            stage.setScene(scene);
            stage.setTitle("WordArena - Create Room");
        });

        joinRoomButton.setOnAction(event -> {

            if (!isSessionConnected()) {

                showDisconnected(
                        connectionLabel,
                        createRoomButton,
                        joinRoomButton
                );

                return;
            }

            openJoinRoomView(username, null);
        });

        joinSelectedRoomButton.setOnAction(event -> {

            if (!isSessionConnected()) {

                showDisconnected(
                        connectionLabel,
                        createRoomButton,
                        joinRoomButton
                );

                return;
            }

            if (!gameSession.isRmiConnected()) {
                return;
            }

            RoomInfo selectedRoom =
                    publicRoomsView.getSelectedRoom();

            if (selectedRoom == null
                    || !publicRoomsView
                    .isSelectedRoomJoinable()) {

                return;
            }

            openJoinRoomView(
                    username,
                    selectedRoom.getRoomId()
            );
        });

        leaderboardButton.setOnAction(event -> {

            if (gameSession == null
                    || !gameSession.isRmiConnected()) {

                return;
            }

            openLeaderboardView(username);
        });

        statisticsButton.setOnAction(event -> {

            if (gameSession == null
                    || !gameSession.isRmiConnected()) {

                return;
            }

            openPlayerStatisticsView(username);
        });

        sceneProperty().addListener(
                (observable, oldScene, newScene) -> {

                    if (newScene != null) {

                        javafx.application.Platform.runLater(() -> {

                            if (getScene() == newScene
                                    && newScene.getWindow()
                                    instanceof Stage stage) {

                                stage.setHeight(
                                        Math.max(
                                                stage.getHeight(),
                                                750
                                        )
                                );
                            }
                        });
                    }
                }
        );
    }

    private String getSelectionMessage(RoomInfo room) {

        if (gameSession == null
                || !gameSession.isRmiConnected()) {

            return "Room discovery is unavailable.";
        }

        if (room == null) {

            return "Select a room to join.";
        }

        if (room.isStarted()) {

            return "This room is already in game.";
        }

        if (room.getCurrentPlayers()
                >= room.getMaxPlayers()) {

            return "This room is full.";
        }

        return "Selected: " + room.getRoomName();
    }

    private void openJoinRoomView(
            String username,
            String selectedRoomId
    ) {

        JoinRoomView joinRoomView =
                new JoinRoomView(
                        username,
                        gameSession,
                        selectedRoomId
                );

        Scene scene = new Scene(
                joinRoomView,
                800,
                500
        );

        applyTheme(scene);

        Stage stage =
                (Stage) getScene().getWindow();

        stage.setScene(scene);
        stage.setTitle("WordArena - Join Room");
    }

    private void openLeaderboardView(String username) {

        LeaderboardView leaderboardView =
                new LeaderboardView(
                        username,
                        gameSession
                );

        Scene scene = new Scene(
                leaderboardView,
                800,
                700
        );

        applyTheme(scene);

        Stage stage =
                (Stage) getScene().getWindow();

        stage.setScene(scene);
        stage.setTitle("WordArena - Leaderboard");
    }

    private void openPlayerStatisticsView(
            String username
    ) {

        PlayerStatisticsView statisticsView =
                new PlayerStatisticsView(
                        username,
                        gameSession
                );

        Scene scene = new Scene(
                statisticsView,
                800,
                750
        );

        applyTheme(scene);

        Stage stage =
                (Stage) getScene().getWindow();

        stage.setScene(scene);
        stage.setTitle("WordArena - My Statistics");
    }

    private boolean isSessionConnected() {

        return gameSession != null
                && gameSession.getTcpClient()
                .isConnected();
    }

    private void showDisconnected(
            Label connectionLabel,
            Button createRoomButton,
            Button joinRoomButton
    ) {

        connectionLabel.setText(
                "SERVER CONNECTION: DISCONNECTED"
        );

        connectionLabel.setStyle(
                getConnectionStatusStyle("DISCONNECTED")
        );

        createRoomButton.setDisable(true);
        joinRoomButton.setDisable(true);
    }

    private String getConnectionStatusStyle(
            String status
    ) {

        return switch (status) {

            case "CONNECTED" ->
                    "-fx-text-fill: #4ADE80;";

            case "CONNECTING" ->
                    "-fx-text-fill: #FACC15;";

            case "NOT CONFIGURED" ->
                    "-fx-text-fill: #FB923C;";

            default ->
                    "-fx-text-fill: #F87171;";
        };
    }

    public GameSession getGameSession() {

        return gameSession;
    }

    private void applyTheme(Scene scene) {

        scene.getStylesheets().add(
                getClass()
                        .getResource("/css/wordarena.css")
                        .toExternalForm()
        );
    }
}
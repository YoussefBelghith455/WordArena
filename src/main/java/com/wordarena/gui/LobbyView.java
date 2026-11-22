
package com.wordarena.gui;

import com.wordarena.client.tcp.GameSession;
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
        setSpacing(25);
        setPadding(new Insets(40));

        Label title = new Label("WORDARENA");
        title.getStyleClass().add("lobby-title");

        Label subtitle = new Label("MULTIPLAYER WORD BATTLE");
        subtitle.getStyleClass().add("lobby-subtitle");

        Label welcome = new Label(
                "Welcome back, " + username + "!"
        );
        welcome.getStyleClass().add("lobby-welcome");

        Label roomsLabel = new Label("MULTIPLAYER LOBBY");
        roomsLabel.getStyleClass().add("section-title");

        Label informationLabel = new Label(
                "Create a new room or join a room using its ID."
        );
        informationLabel.getStyleClass().add("lobby-subtitle");
        informationLabel.setWrapText(true);
        informationLabel.setAlignment(Pos.CENTER);
        informationLabel.setMaxWidth(460);

        Label roomInfoLabel = new Label(
                "Public room discovery will be available "
                        + "when the RMI lobby is integrated."
        );
        roomInfoLabel.getStyleClass().add("room-card");
        roomInfoLabel.setWrapText(true);
        roomInfoLabel.setAlignment(Pos.CENTER);
        roomInfoLabel.setMaxWidth(460);

        boolean sessionAvailable =
                gameSession != null
                        && gameSession.getTcpClient().isConnected();

        Label connectionLabel = new Label(
                sessionAvailable
                        ? "SERVER CONNECTION: CONNECTED"
                        : "SERVER CONNECTION: DISCONNECTED"
        );
        connectionLabel.getStyleClass().add("lobby-welcome");

        Button createButton = new Button("CREATE ROOM");
        Button joinButton = new Button("JOIN ROOM");

        createButton.getStyleClass().add("neon-button");
        joinButton.getStyleClass().add("neon-button");

        createButton.setPrefWidth(190);
        joinButton.setPrefWidth(190);

        createButton.setDisable(!sessionAvailable);
        joinButton.setDisable(!sessionAvailable);

        HBox actions = new HBox(
                15,
                createButton,
                joinButton
        );
        actions.setAlignment(Pos.CENTER);

        VBox header = new VBox(
                10,
                title,
                subtitle,
                welcome
        );
        header.setAlignment(Pos.CENTER);

        VBox roomSection = new VBox(
                15,
                roomsLabel,
                informationLabel,
                roomInfoLabel
        );
        roomSection.setAlignment(Pos.CENTER);

        getChildren().addAll(
                header,
                roomSection,
                actions,
                connectionLabel
        );

        createButton.setOnAction(event -> {
            if (!isSessionConnected()) {
                showDisconnected(
                        connectionLabel,
                        createButton,
                        joinButton
                );
                return;
            }

            CreateRoomView view = new CreateRoomView(
                    username,
                    gameSession
            );

            Scene scene = new Scene(view, 800, 500);
            applyTheme(scene);

            Stage stage = (Stage) getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("WordArena - Create Room");
        });

        joinButton.setOnAction(event -> {
            if (!isSessionConnected()) {
                showDisconnected(
                        connectionLabel,
                        createButton,
                        joinButton
                );
                return;
            }

            JoinRoomView view = new JoinRoomView(
                    username,
                    gameSession
            );

            Scene scene = new Scene(view, 800, 500);
            applyTheme(scene);

            Stage stage = (Stage) getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("WordArena - Join Room");
        });
    }

    private boolean isSessionConnected() {
        return gameSession != null
                && gameSession.getTcpClient().isConnected();
    }

    private void showDisconnected(
            Label connectionLabel,
            Button createButton,
            Button joinButton
    ) {
        connectionLabel.setText(
                "SERVER CONNECTION: DISCONNECTED"
        );

        createButton.setDisable(true);
        joinButton.setDisable(true);
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

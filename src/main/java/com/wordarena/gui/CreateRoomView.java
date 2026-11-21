
package com.wordarena.gui;

import com.wordarena.common.GameMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CreateRoomView extends VBox {

    public CreateRoomView(String username) {

        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(40));
        getStyleClass().add("create-room-root");

        Label title = new Label("CREATE ROOM");
        title.getStyleClass().add("lobby-title");

        Label subtitle = new Label("CHOOSE YOUR BATTLE MODE");
        subtitle.getStyleClass().add("lobby-subtitle");

        Label welcome = new Label("Player: " + username);
        welcome.getStyleClass().add("lobby-welcome");

        ComboBox<GameMode> modeSelector = new ComboBox<>();
        modeSelector.getItems().addAll(GameMode.values());
        modeSelector.setPromptText("Select game mode");
        modeSelector.setPrefWidth(300);

        Button createButton = new Button("CREATE ROOM");
        createButton.getStyleClass().add("neon-button");
        createButton.setPrefWidth(300);

        Button backButton = new Button("BACK TO LOBBY");
        backButton.getStyleClass().add("secondary-button");
        backButton.setPrefWidth(300);

        Label messageLabel = new Label();
        messageLabel.setWrapText(true);

        VBox content = new VBox(
                20,
                title,
                subtitle,
                welcome,
                modeSelector,
                createButton,
                messageLabel,
                backButton
        );

        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(35));
        content.setMaxWidth(450);
        content.getStyleClass().add("create-room-panel");

        getChildren().add(content);

        createButton.setOnAction(event -> {

            GameMode mode = modeSelector.getValue();

            if (mode == null) {
                messageLabel.setText("Please select a game mode");
                return;
            }

            String roomName = username + "'s Room";

            Stage stage = (Stage) createButton.getScene().getWindow();

            if (mode == GameMode.HOST) {

                HostWordView hostWordView = new HostWordView(secretWord -> {

                    WaitingRoomView waitingRoom = new WaitingRoomView(
                            username,
                            roomName,
                            mode,
                            true
                    );

                    Scene waitingScene = new Scene(
                            waitingRoom,
                            900,
                            700
                    );

                    applyTheme(waitingScene);

                    stage.setScene(waitingScene);
                    stage.setTitle("WordArena - Waiting Room");
                });

                VBox hostRoot = new VBox(hostWordView);
                hostRoot.setAlignment(Pos.CENTER);
                hostRoot.setPadding(new Insets(30));
                hostRoot.getStyleClass().add("create-room-root");

                Scene hostScene = new Scene(hostRoot, 900, 700);
                applyTheme(hostScene);

                stage.setScene(hostScene);
                stage.setTitle("WordArena - Host Word");

                return;
            }

            WaitingRoomView waitingRoom = new WaitingRoomView(
                    username,
                    roomName,
                    mode,
                    true
            );

            Scene scene = new Scene(waitingRoom, 900, 700);
            applyTheme(scene);

            stage.setScene(scene);
            stage.setTitle("WordArena - Waiting Room");
        });

        backButton.setOnAction(event -> {

            LobbyView lobby = new LobbyView(username);

            Scene scene = new Scene(lobby, 800, 600);
            applyTheme(scene);

            Stage stage = (Stage) backButton.getScene().getWindow();

            stage.setScene(scene);
            stage.setTitle("WordArena - Lobby");
        });
    }

    private void applyTheme(Scene scene) {

        scene.getStylesheets().add(
                getClass()
                        .getResource("/css/wordarena.css")
                        .toExternalForm()
        );
    }
}

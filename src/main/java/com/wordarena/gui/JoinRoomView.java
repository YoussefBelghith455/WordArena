
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

public class JoinRoomView extends VBox {

    public JoinRoomView(String username) {

        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(40));
        getStyleClass().add("join-room-root");

        Label title = new Label("JOIN ROOM");
        title.getStyleClass().add("lobby-title");

        Label subtitle = new Label("ENTER THE BATTLE ARENA");
        subtitle.getStyleClass().add("lobby-subtitle");

        Label welcome = new Label("Player: " + username);
        welcome.getStyleClass().add("lobby-welcome");

        ComboBox<String> roomSelector = new ComboBox<>();
        roomSelector.getItems().addAll(
                "Room 1 - DUEL",
                "Room 2 - COOPERATIVE"
        );
        roomSelector.setPromptText("Select a room");
        roomSelector.setPrefWidth(300);

        Button joinButton = new Button("JOIN SELECTED ROOM");
        joinButton.getStyleClass().add("neon-button");
        joinButton.setPrefWidth(300);

        Label messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("room-message");

        Button backButton = new Button("BACK TO LOBBY");
        backButton.getStyleClass().add("secondary-button");
        backButton.setPrefWidth(300);

        VBox content = new VBox(
                20,
                title,
                subtitle,
                welcome,
                roomSelector,
                joinButton,
                messageLabel,
                backButton
        );

        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(35));
        content.setMaxWidth(450);
        content.getStyleClass().add("join-room-panel");

        getChildren().add(content);

        joinButton.setOnAction(event -> {

            String selectedRoom = roomSelector.getValue();

            if (selectedRoom == null) {
                messageLabel.setText("Please select a room");
                return;
            }

            GameMode mode;
            String roomName;

            if (selectedRoom.equals("Room 1 - DUEL")) {
                mode = GameMode.DUEL;
                roomName = "Room 1";
            } else {
                mode = GameMode.COOPERATIVE;
                roomName = "Room 2";
            }

            WaitingRoomView waitingRoom = new WaitingRoomView(
                    username,
                    roomName,
                    mode,
                    false
            );

            Scene scene = new Scene(waitingRoom, 900, 700);

            scene.getStylesheets().add(
                    getClass().getResource("/css/wordarena.css").toExternalForm()
            );

            Stage stage = (Stage) joinButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("WordArena - Waiting Room");
        });

        backButton.setOnAction(event -> {

            LobbyView lobby = new LobbyView(username);
            Scene scene = new Scene(lobby, 800, 600);

            scene.getStylesheets().add(
                    getClass().getResource("/css/wordarena.css").toExternalForm()
            );

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("WordArena - Lobby");
        });
    }
}

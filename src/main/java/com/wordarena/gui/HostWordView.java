
package com.wordarena.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class HostWordView extends VBox {

    private final PasswordField secretField = new PasswordField();
    private final TextField visibleField = new TextField();
    private final Label messageLabel = new Label();
    private final Button confirmButton = new Button("LOCK SECRET WORD");

    private boolean wordLocked = false;

    public HostWordView(Consumer<String> onWordConfirmed) {

        getStyleClass().add("host-word-root");
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(35));
        setMaxWidth(500);

        Label title = new Label("HOST MODE");
        title.getStyleClass().add("lobby-title");

        Label subtitle = new Label("CHOOSE THE SECRET WORD");
        subtitle.getStyleClass().add("lobby-subtitle");

        Label instructions = new Label(
                "Enter a 5-letter word for your opponent to guess."
        );
        instructions.getStyleClass().add("host-word-instructions");
        instructions.setWrapText(true);

        secretField.setPromptText("Enter secret word");
        secretField.setMaxWidth(300);

        visibleField.setPromptText("Enter secret word");
        visibleField.setMaxWidth(300);

        visibleField.textProperty().bindBidirectional(
                secretField.textProperty()
        );

        visibleField.setVisible(false);
        visibleField.setManaged(false);

        Button revealButton = new Button("SHOW WORD");
        revealButton.getStyleClass().add("secondary-button");
        revealButton.setPrefWidth(140);

        revealButton.setOnAction(event -> {

            boolean show = !visibleField.isVisible();

            visibleField.setVisible(show);
            visibleField.setManaged(show);

            secretField.setVisible(!show);
            secretField.setManaged(!show);

            revealButton.setText(
                    show ? "HIDE WORD" : "SHOW WORD"
            );
        });

        confirmButton.getStyleClass().add("neon-button");
        confirmButton.setPrefWidth(300);

        messageLabel.getStyleClass().add("host-word-message");
        messageLabel.setWrapText(true);

        confirmButton.setOnAction(event -> {

            String word = secretField.getText()
                    .trim()
                    .toUpperCase();

            if (!word.matches("[A-Z]{5}")) {
                messageLabel.setText(
                        "Secret word must contain exactly 5 letters."
                );
                return;
            }

            if (wordLocked) {
                return;
            }

            wordLocked = true;

            secretField.setText("");
            visibleField.setText("");

            secretField.setDisable(true);
            visibleField.setDisable(true);
            revealButton.setDisable(true);
            confirmButton.setDisable(true);

            messageLabel.setText(
                    "Secret word locked. Waiting for the game to start..."
            );

            if (onWordConfirmed != null) {
                onWordConfirmed.accept(word);
            }
        });

        HBox revealBox = new HBox(10, revealButton);
        revealBox.setAlignment(Pos.CENTER);

        getChildren().addAll(
                title,
                subtitle,
                instructions,
                secretField,
                visibleField,
                revealBox,
                confirmButton,
                messageLabel
        );
    }
}

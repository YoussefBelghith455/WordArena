
package com.wordarena.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class DoubleHostWordView extends VBox {

    private final Label statusLabel = new Label();
    private boolean wordLocked = false;

    public DoubleHostWordView(Consumer<String> onWordConfirmed) {

        getStyleClass().add("double-host-root");
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(35));
        setMaxWidth(500);

        Label title = new Label("DOUBLE HOST MODE");
        title.getStyleClass().add("lobby-title");

        Label subtitle = new Label("CHOOSE YOUR SECRET WORD");
        subtitle.getStyleClass().add("lobby-subtitle");

        Label instructions = new Label(
                "Choose a 5-letter word for your opponent to guess."
        );
        instructions.getStyleClass().add("double-host-instructions");
        instructions.setWrapText(true);
        instructions.setMaxWidth(400);

        PasswordField secretField = new PasswordField();
        secretField.setPromptText("Enter secret word");
        secretField.setMaxWidth(300);

        TextField visibleField = new TextField();
        visibleField.setPromptText("Enter secret word");
        visibleField.setMaxWidth(300);

        visibleField.textProperty().bindBidirectional(
                secretField.textProperty()
        );

        visibleField.setVisible(false);
        visibleField.setManaged(false);

        Button revealButton = new Button("SHOW WORD");
        revealButton.getStyleClass().add("secondary-button");
        revealButton.setPrefWidth(160);

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

        Button confirmButton = new Button("LOCK SECRET WORD");
        confirmButton.getStyleClass().add("neon-button");
        confirmButton.setPrefWidth(300);

        statusLabel.getStyleClass().add("double-host-status");
        statusLabel.setWrapText(true);

        confirmButton.setOnAction(event -> {

            if (wordLocked) {
                return;
            }

            String word = secretField.getText()
                    .trim()
                    .toUpperCase();

            if (!word.matches("[A-Z]{5}")) {
                statusLabel.setText(
                        "Secret word must contain exactly 5 letters."
                );
                return;
            }

            wordLocked = true;

            secretField.clear();
            secretField.setDisable(true);
            visibleField.setDisable(true);
            revealButton.setDisable(true);
            confirmButton.setDisable(true);

            statusLabel.setText(
                    "Secret word locked. Waiting for the other player..."
            );

            if (onWordConfirmed != null) {
                onWordConfirmed.accept(word);
            }
        });

        getChildren().addAll(
                title,
                subtitle,
                instructions,
                secretField,
                visibleField,
                revealButton,
                confirmButton,
                statusLabel
        );
    }

    public boolean isWordLocked() {
        return wordLocked;
    }

    public void updateStatus(String status) {
        statusLabel.setText(status);
    }
}

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
    private boolean wordLocked;
    private boolean submitting;
    private final PasswordField secretField = new PasswordField();
    private final TextField visibleField = new TextField();
    private final Button revealButton = new Button("SHOW WORD");
    private final Button confirmButton = new Button("LOCK SECRET WORD");
    private final Button backButton = new Button("BACK");

    public DoubleHostWordView(Consumer<String> onWordConfirmed) {

        this(onWordConfirmed, null);
    }

    public DoubleHostWordView(Consumer<String> onWordConfirmed, Runnable onBack) {
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

        secretField.setPromptText("Enter secret word");
        secretField.setMaxWidth(300);

        visibleField.setPromptText("Enter secret word");
        visibleField.setMaxWidth(300);

        visibleField.textProperty().bindBidirectional(
                secretField.textProperty()
        );

        visibleField.setVisible(false);
        visibleField.setManaged(false);

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

        confirmButton.getStyleClass().add("neon-button");
        confirmButton.setPrefWidth(300);

        statusLabel.getStyleClass().add("double-host-status");
        statusLabel.setWrapText(true);

        confirmButton.setOnAction(event -> {

            if (wordLocked || submitting) {
                return;
            }

            String word = secretField.getText()
                    .trim()
                    .toUpperCase(java.util.Locale.ROOT);

            if (!word.matches("[A-Z]{5}")) {
                statusLabel.setText(
                        "Secret word must contain exactly 5 letters."
                );
                return;
            }

            setSubmitting(true);
            if (onWordConfirmed != null) {
                try {
                    onWordConfirmed.accept(word);
                    if (onBack == null) showLocked();
                } catch (RuntimeException exception) {
                    showError("Unable to submit secret word.");
                }
            } else {
                showError("No word submission handler.");
            }
        });

        backButton.getStyleClass().add("secondary-button");
        backButton.setVisible(onBack != null);
        backButton.setManaged(onBack != null);
        backButton.setOnAction(event -> { if (!submitting && onBack != null) onBack.run(); });
        getChildren().addAll(
                title,
                subtitle,
                instructions,
                secretField,
                visibleField,
                revealButton,
                confirmButton,
                statusLabel, backButton
        );
    }

    public boolean isSubmitting() { return submitting; }

    public void setSubmitting(boolean value) {
        submitting = value;
        boolean disabled = value || wordLocked;
        secretField.setDisable(disabled);
        visibleField.setDisable(disabled);
        revealButton.setDisable(disabled);
        confirmButton.setDisable(disabled);
        backButton.setDisable(value);
        if (value) updateStatus("Waiting for server confirmation...");
    }

    public void showError(String message) {
        setSubmitting(false);
        updateStatus(message);
    }

    public void showLocked() {
        wordLocked = true;
        secretField.clear();
        setSubmitting(false);
        updateStatus("Secret word locked.");
    }

    public boolean isWordLocked() {
        return wordLocked;
    }

    public void updateStatus(String status) {
        statusLabel.setText(status);
    }
}

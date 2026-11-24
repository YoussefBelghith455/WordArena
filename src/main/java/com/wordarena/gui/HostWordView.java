package com.wordarena.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.function.Consumer;

public class HostWordView extends VBox {

    private final PasswordField secretField = new PasswordField();
    private final TextField visibleField = new TextField();

    private final Label messageLabel = new Label();

    private final Button confirmButton =
            new Button("LOCK SECRET WORD");

    private final Button revealButton =
            new Button("SHOW WORD");

    private final Button backButton =
            new Button("BACK TO WAITING ROOM");

    private boolean wordLocked;
    private boolean submitting;

    public HostWordView(Consumer<String> onWordConfirmed) {
        this(onWordConfirmed, null);
    }

    public HostWordView(
            Consumer<String> onWordConfirmed,
            Runnable onBack
    ) {

        getStyleClass().add("host-word-root");

        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(35));
        setMaxWidth(500);

        Label title = new Label("HOST MODE");

        title.getStyleClass().add("lobby-title");

        Label subtitle = new Label(
                "CHOOSE THE SECRET WORD"
        );

        subtitle.getStyleClass().add("lobby-subtitle");

        Label instructions = new Label(
                "Enter a 5-letter word for the guessing team."
        );

        instructions.getStyleClass().add(
                "host-word-instructions"
        );

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

        revealButton.getStyleClass().add(
                "secondary-button"
        );

        revealButton.setPrefWidth(140);

        revealButton.setOnAction(event -> {

            if (submitting || wordLocked) {
                return;
            }

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
        confirmButton.setDisable(true);

        backButton.getStyleClass().add("secondary-button");
        backButton.setPrefWidth(300);

        backButton.setVisible(onBack != null);
        backButton.setManaged(onBack != null);

        messageLabel.getStyleClass().add(
                "host-word-message"
        );

        messageLabel.setWrapText(true);
        messageLabel.setAlignment(Pos.CENTER);

        messageLabel.setText(
                "Your secret word will remain hidden from other players."
        );

        secretField.textProperty().addListener(
                (observable, oldValue, newValue) -> {

                    if (submitting || wordLocked) {
                        return;
                    }

                    String word = newValue.trim()
                            .toUpperCase(Locale.ROOT);

                    confirmButton.setDisable(
                            !word.matches("[A-Z]{5}")
                    );

                    messageLabel.setStyle("");

                    if (word.isEmpty()) {

                        messageLabel.setText(
                                "Enter a five-letter secret word."
                        );

                    } else if (!word.matches("[A-Z]{5}")) {

                        messageLabel.setText(
                                "Secret word must contain exactly 5 English letters."
                        );

                    } else {

                        messageLabel.setText(
                                "Your word is ready to be locked."
                        );
                    }
                }
        );

        confirmButton.setOnAction(event -> {

            if (submitting || wordLocked) {
                return;
            }

            String word = secretField.getText()
                    .trim()
                    .toUpperCase(Locale.ROOT);

            if (!word.matches("[A-Z]{5}")) {

                showError(
                        "Secret word must contain exactly 5 English letters."
                );

                return;
            }

            if (onWordConfirmed == null) {

                showError(
                        "Secret word submission is unavailable."
                );

                return;
            }

            setSubmitting(true);

            messageLabel.setStyle("");

            messageLabel.setText(
                    "Locking secret word..."
            );

            try {

                onWordConfirmed.accept(word);

            } catch (RuntimeException exception) {

                showError(
                        "Unable to submit the secret word."
                );
            }
        });

        secretField.setOnAction(event -> {

            if (!confirmButton.isDisabled()) {
                confirmButton.fire();
            }
        });

        visibleField.setOnAction(event -> {

            if (!confirmButton.isDisabled()) {
                confirmButton.fire();
            }
        });

        backButton.setOnAction(event -> {

            if (!submitting
                    && !wordLocked
                    && onBack != null) {

                secretField.clear();

                onBack.run();
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
                messageLabel,
                backButton
        );
    }

    public void setSubmitting(boolean submitting) {

        this.submitting = submitting;

        boolean disabled = submitting || wordLocked;

        secretField.setDisable(disabled);
        visibleField.setDisable(disabled);
        revealButton.setDisable(disabled);
        backButton.setDisable(disabled);

        String word = secretField.getText()
                .trim()
                .toUpperCase(Locale.ROOT);

        confirmButton.setDisable(
                disabled || !word.matches("[A-Z]{5}")
        );
    }

    public void showError(String message) {

        if (wordLocked) {
            return;
        }

        setSubmitting(false);

        messageLabel.setText(message);

        messageLabel.setStyle(
                "-fx-text-fill: #F87171;"
        );
    }

    public void showLocked() {

        wordLocked = true;

        secretField.clear();

        setSubmitting(false);

        confirmButton.setText("WORD LOCKED");

        messageLabel.setText(
                "Secret word successfully locked."
        );

        messageLabel.setStyle(
                "-fx-text-fill: #4ADE80;"
        );
    }

    public boolean isWordLocked() {
        return wordLocked;
    }

    public boolean isSubmitting() {
        return submitting;
    }
}
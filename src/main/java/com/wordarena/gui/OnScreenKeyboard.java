
package com.wordarena.gui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class OnScreenKeyboard extends VBox {

    private final Consumer<String> keyHandler;
    private final Map<String, Button> letterButtons = new HashMap<>();
    private final Map<String, Integer> letterStates = new HashMap<>();

    public OnScreenKeyboard(Consumer<String> keyHandler) {

        this.keyHandler = keyHandler;

        setAlignment(Pos.CENTER);
        setSpacing(7);

        createRow("QWERTYUIOP");
        createRow("ASDFGHJKL");

        HBox lastRow = new HBox(5);
        lastRow.setAlignment(Pos.CENTER);

        Button enterButton = createButton("ENTER");
        enterButton.getStyleClass().add("keyboard-special");
        lastRow.getChildren().add(enterButton);

        for (char letter : "ZXCVBNM".toCharArray()) {
            lastRow.getChildren().add(
                    createButton(String.valueOf(letter))
            );
        }

        Button backspaceButton = createButton("⌫");
        backspaceButton.getStyleClass().add("keyboard-special");
        lastRow.getChildren().add(backspaceButton);

        getChildren().add(lastRow);
    }

    private void createRow(String letters) {

        HBox row = new HBox(5);
        row.setAlignment(Pos.CENTER);

        for (char letter : letters.toCharArray()) {
            row.getChildren().add(
                    createButton(String.valueOf(letter))
            );
        }

        getChildren().add(row);
    }

    private Button createButton(String text) {

        Button button = new Button(text);

        button.getStyleClass().add("keyboard-key");
        button.setPrefHeight(48);
        button.setMinWidth(text.length() > 1 ? 65 : 38);
        button.setFocusTraversable(false);

        if (text.length() == 1 && text.matches("[A-Z]")) {
            letterButtons.put(text, button);
            letterStates.put(text, -1);
        }

        button.setOnAction(event -> {

            if (text.equals("ENTER")) {
                keyHandler.accept("ENTER");

            } else if (text.equals("⌫")) {
                keyHandler.accept("BACKSPACE");

            } else {
                keyHandler.accept(text);
            }
        });

        return button;
    }

    public void updateKeyFeedback(String letter, int feedback) {

        Button button = letterButtons.get(letter);

        if (button == null) {
            return;
        }

        int previousState = letterStates.get(letter);

        if (feedback <= previousState) {
            return;
        }

        letterStates.put(letter, feedback);

        button.getStyleClass().removeAll(
                "keyboard-correct",
                "keyboard-present",
                "keyboard-absent"
        );

        switch (feedback) {
            case 2 -> button.getStyleClass().add("keyboard-correct");
            case 1 -> button.getStyleClass().add("keyboard-present");
            case 0 -> button.getStyleClass().add("keyboard-absent");
            default -> {
                letterStates.put(letter, previousState);
            }
        }
    }

    public void setKeyboardDisabled(boolean disabled) {
        setDisable(disabled);
    }
}

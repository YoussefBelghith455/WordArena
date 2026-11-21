package com.wordarena.server.game;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

public class WordBank {

    private static final List<String> DEFAULT_WORDS = List.of(
            "APPLE",
            "BEACH",
            "BRAIN",
            "CHAIR",
            "CLOUD",
            "DREAM",
            "EARTH",
            "FLAME",
            "GRAPE",
            "HOUSE",
            "LIGHT",
            "MOUSE",
            "NIGHT",
            "OCEAN",
            "PLANT",
            "QUEEN",
            "RIVER",
            "SMILE",
            "STONE",
            "TRAIN",
            "WATER",
            "WORLD"
    );

    private final List<String> words;
    private final Random random;

    public WordBank() {
        this(DEFAULT_WORDS, new Random());
    }

    WordBank(
            List<String> words,
            Random random
    ) {
        Objects.requireNonNull(
                words,
                "Word list is required."
        );

        this.random = Objects.requireNonNull(
                random,
                "Random generator is required."
        );

        if (words.isEmpty()) {
            throw new IllegalArgumentException(
                    "Word list cannot be empty."
            );
        }

        this.words = words.stream()
                .map(WordBank::validateWord)
                .distinct()
                .toList();

        if (this.words.isEmpty()) {
            throw new IllegalArgumentException(
                    "Word list must contain at least one valid word."
            );
        }
    }

    public String selectRandomWord() {
        int selectedIndex = random.nextInt(words.size());
        return words.get(selectedIndex);
    }

    public boolean contains(String word) {
        if (word == null || word.isBlank()) {
            return false;
        }

        String normalizedWord =
                word.trim().toUpperCase(Locale.ROOT);

        return words.contains(normalizedWord);
    }

    public int size() {
        return words.size();
    }

    private static String validateWord(String word) {
        if (word == null || word.isBlank()) {
            throw new IllegalArgumentException(
                    "Word-bank entries cannot be empty."
            );
        }

        String normalizedWord =
                word.trim().toUpperCase(Locale.ROOT);

        if (normalizedWord.length()
                != WordEvaluator.WORD_LENGTH) {

            throw new IllegalArgumentException(
                    "Every word-bank entry must contain exactly "
                            + WordEvaluator.WORD_LENGTH
                            + " letters."
            );
        }

        if (!normalizedWord.matches("[A-Z]+")) {
            throw new IllegalArgumentException(
                    "Word-bank entries may contain only English letters."
            );
        }

        return normalizedWord;
    }
}
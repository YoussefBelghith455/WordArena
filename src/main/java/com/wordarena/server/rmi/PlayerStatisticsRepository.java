package com.wordarena.server.rmi;

import com.wordarena.common.PlayerStats;
import com.wordarena.common.ScoreEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStatisticsRepository {

    private final Map<String, MutablePlayerStatistics>
            statisticsByPlayerId;

    public PlayerStatisticsRepository() {
        this.statisticsByPlayerId =
                new ConcurrentHashMap<>();
    }

    public void registerPlayer(
            String playerId,
            String username
    ) {
        String validatedPlayerId = requireText(
                playerId,
                "Player ID"
        );

        String validatedUsername = requireText(
                username,
                "Username"
        );

        statisticsByPlayerId.compute(
                validatedPlayerId,
                (ignoredPlayerId, existingStatistics) -> {
                    if (existingStatistics == null) {
                        return new MutablePlayerStatistics(
                                validatedPlayerId,
                                validatedUsername
                        );
                    }

                    existingStatistics.updateUsername(
                            validatedUsername
                    );

                    return existingStatistics;
                }
        );
    }

    public void recordGameResult(
            String playerId,
            String username,
            boolean won,
            int earnedScore
    ) {
        if (earnedScore < 0) {
            throw new IllegalArgumentException(
                    "Earned score cannot be negative."
            );
        }

        registerPlayer(playerId, username);

        MutablePlayerStatistics statistics =
                statisticsByPlayerId.get(playerId);

        statistics.recordResult(
                won,
                earnedScore
        );
    }

    public Optional<PlayerStats> findPlayerStats(
            String playerId
    ) {
        if (playerId == null || playerId.isBlank()) {
            return Optional.empty();
        }

        MutablePlayerStatistics statistics =
                statisticsByPlayerId.get(
                        playerId.trim()
                );

        if (statistics == null) {
            return Optional.empty();
        }

        return Optional.of(
                statistics.toPlayerStats()
        );
    }

    public List<ScoreEntry> getLeaderboard() {
        List<PlayerStats> orderedStatistics =
                statisticsByPlayerId.values()
                        .stream()
                        .map(
                                MutablePlayerStatistics::
                                        toPlayerStats
                        )
                        .sorted(
                                Comparator.comparingInt(
                                                PlayerStats::
                                                        getTotalScore
                                        )
                                        .reversed()
                                        .thenComparing(
                                                PlayerStats::
                                                        getUsername,
                                                String.CASE_INSENSITIVE_ORDER
                                        )
                        )
                        .toList();

        List<ScoreEntry> leaderboard =
                new ArrayList<>();

        for (int index = 0;
             index < orderedStatistics.size();
             index++) {

            PlayerStats statistics =
                    orderedStatistics.get(index);

            leaderboard.add(
                    new ScoreEntry(
                            statistics.getPlayerId(),
                            statistics.getUsername(),
                            statistics.getTotalScore(),
                            index + 1
                    )
            );
        }

        return List.copyOf(leaderboard);
    }

    public int getRegisteredPlayerCount() {
        return statisticsByPlayerId.size();
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        return value.trim();
    }

    private static final class MutablePlayerStatistics {

        private final String playerId;

        private String username;
        private int gamesPlayed;
        private int gamesWon;
        private int gamesLost;
        private int totalScore;

        private MutablePlayerStatistics(
                String playerId,
                String username
        ) {
            this.playerId = playerId;
            this.username = username;
        }

        private synchronized void updateUsername(
                String username
        ) {
            this.username = username;
        }

        private synchronized void recordResult(
                boolean won,
                int earnedScore
        ) {
            gamesPlayed++;

            if (won) {
                gamesWon++;
            } else {
                gamesLost++;
            }

            totalScore += earnedScore;
        }

        private synchronized PlayerStats toPlayerStats() {
            return new PlayerStats(
                    playerId,
                    username,
                    gamesPlayed,
                    gamesWon,
                    gamesLost,
                    totalScore
            );
        }
    }
}
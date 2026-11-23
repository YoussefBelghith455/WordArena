package com.wordarena.server.rmi;

import com.wordarena.server.game.DuelRound;
import com.wordarena.server.game.GamePlayer;
import com.wordarena.server.game.GameRoom;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameStatisticsService {

    private final PlayerStatisticsRepository
            statisticsRepository;

    private final Set<String> recordedRoundKeys;

    public GameStatisticsService(
            PlayerStatisticsRepository statisticsRepository
    ) {
        this.statisticsRepository =
                Objects.requireNonNull(
                        statisticsRepository,
                        "Statistics repository is required."
                );

        this.recordedRoundKeys =
                ConcurrentHashMap.newKeySet();
    }

    public void registerPlayer(
            String playerId,
            String username
    ) {
        statisticsRepository.registerPlayer(
                playerId,
                username
        );
    }

    public boolean recordCompletedDuel(
            GameRoom room,
            DuelRound duelRound
    ) {
        Objects.requireNonNull(
                room,
                "Game room is required."
        );

        Objects.requireNonNull(
                duelRound,
                "Duel round is required."
        );

        if (!duelRound.isFinished()) {
            throw new IllegalStateException(
                    "Duel statistics cannot be recorded "
                            + "before the round finishes."
            );
        }

        String roundKey = createRoundKey(
                room,
                duelRound
        );

        boolean firstRecording =
                recordedRoundKeys.add(roundKey);

        if (!firstRecording) {
            return false;
        }

        String winnerPlayerId =
                duelRound.getWinnerPlayerId()
                        .orElse("");

        for (String playerId :
                duelRound.getPlayerIds()) {

            GamePlayer player =
                    room.requirePlayer(playerId);

            boolean won =
                    playerId.equals(winnerPlayerId);

            statisticsRepository.recordGameResult(
                    player.getPlayerId(),
                    player.getUsername(),
                    won,
                    player.getScore()
            );
        }

        return true;
    }

    public PlayerStatisticsRepository
    getStatisticsRepository() {

        return statisticsRepository;
    }

    public int getRecordedRoundCount() {
        return recordedRoundKeys.size();
    }

    private String createRoundKey(
            GameRoom room,
            DuelRound duelRound
    ) {
        return room.getRoomId()
                + ":"
                + duelRound.getStartedAtTimestamp();
    }
}
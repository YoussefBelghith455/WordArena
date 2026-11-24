package com.wordarena.server.rmi;

import com.wordarena.server.game.CooperativeRound;
import com.wordarena.server.game.DoubleHostRound;
import com.wordarena.server.game.DuelRound;
import com.wordarena.server.game.GamePlayer;
import com.wordarena.server.game.GameRoom;
import com.wordarena.server.game.HostRound;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameStatisticsService {

    private final PlayerStatisticsRepository statisticsRepository;
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
        requireFinished(
                room,
                duelRound != null
                        && duelRound.isFinished(),
                "Duel statistics cannot be recorded "
                        + "before the round finishes."
        );

        String roundKey =
                room.getRoomId()
                        + ":DUEL:"
                        + duelRound.getStartedAtTimestamp();

        if (!recordedRoundKeys.add(roundKey)) {
            return false;
        }

        String winnerId =
                duelRound.getWinnerPlayerId()
                        .orElse("");

        for (String playerId :
                duelRound.getPlayerIds()) {

            recordPlayer(
                    room,
                    playerId,
                    playerId.equals(winnerId)
            );
        }

        return true;
    }

    public boolean recordCompletedCooperative(
            GameRoom room,
            CooperativeRound cooperativeRound
    ) {
        requireFinished(
                room,
                cooperativeRound != null
                        && cooperativeRound.isFinished(),
                "Cooperative statistics cannot be recorded "
                        + "before the round finishes."
        );

        String roundKey =
                room.getRoomId()
                        + ":COOPERATIVE:"
                        + cooperativeRound
                        .getStartedAtTimestamp();

        if (!recordedRoundKeys.add(roundKey)) {
            return false;
        }

        for (String playerId :
                cooperativeRound.getPlayerIds()) {

            recordPlayer(
                    room,
                    playerId,
                    cooperativeRound.isSolved()
            );
        }

        return true;
    }

    public boolean recordCompletedHost(
            GameRoom room,
            HostRound hostRound
    ) {
        requireFinished(
                room,
                hostRound != null
                        && hostRound.isFinished(),
                "Host statistics cannot be recorded "
                        + "before the round finishes."
        );

        String roundKey =
                room.getRoomId()
                        + ":HOST:"
                        + hostRound.getStartedAtTimestamp();

        if (!recordedRoundKeys.add(roundKey)) {
            return false;
        }

        for (String playerId :
                hostRound.getParticipantPlayerIds()) {

            boolean won =
                    hostRound.isSolved()
                            ? !playerId.equals(
                            hostRound
                                    .getHostPlayerId()
                    )
                            : playerId.equals(
                            hostRound
                                    .getHostPlayerId()
                    );

            recordPlayer(
                    room,
                    playerId,
                    won
            );
        }

        return true;
    }

    public boolean recordCompletedDoubleHost(
            GameRoom room,
            DoubleHostRound doubleHostRound
    ) {
        requireFinished(
                room,
                doubleHostRound != null
                        && doubleHostRound.isFinished(),
                "Double Host statistics cannot be recorded "
                        + "before the round finishes."
        );

        String roundKey =
                room.getRoomId()
                        + ":DOUBLE_HOST:"
                        + doubleHostRound
                        .getStartedAtTimestamp();

        if (!recordedRoundKeys.add(roundKey)) {
            return false;
        }

        String winnerId =
                doubleHostRound.getWinnerPlayerId()
                        .orElse("");

        for (String playerId :
                doubleHostRound.getPlayerIds()) {

            recordPlayer(
                    room,
                    playerId,
                    playerId.equals(winnerId)
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

    private void recordPlayer(
            GameRoom room,
            String playerId,
            boolean won
    ) {
        GamePlayer player =
                room.requirePlayer(playerId);

        statisticsRepository.recordGameResult(
                player.getPlayerId(),
                player.getUsername(),
                won,
                player.getScore()
        );
    }

    private void requireFinished(
            GameRoom room,
            boolean finished,
            String errorMessage
    ) {
        Objects.requireNonNull(
                room,
                "Game room is required."
        );

        if (!finished) {
            throw new IllegalStateException(
                    errorMessage
            );
        }
    }
}
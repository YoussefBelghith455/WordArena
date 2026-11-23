package com.wordarena.server.rmi;

import com.wordarena.common.PlayerStats;
import com.wordarena.common.PlayerView;
import com.wordarena.common.RoomInfo;
import com.wordarena.common.ScoreEntry;
import com.wordarena.common.remote.WordArenaRemoteService;
import com.wordarena.server.game.GameRoom;
import com.wordarena.server.game.RoomManager;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Objects;

public class WordArenaRemoteServiceImpl
        extends UnicastRemoteObject
        implements WordArenaRemoteService {

    private final RoomManager roomManager;
    private final PlayerStatisticsRepository
            statisticsRepository;

    public WordArenaRemoteServiceImpl(
            RoomManager roomManager,
            PlayerStatisticsRepository statisticsRepository
    ) throws RemoteException {
        super();

        this.roomManager = Objects.requireNonNull(
                roomManager,
                "Room manager is required."
        );

        this.statisticsRepository =
                Objects.requireNonNull(
                        statisticsRepository,
                        "Statistics repository is required."
                );
    }

    @Override
    public List<RoomInfo> getAvailableRooms()
            throws RemoteException {

        try {
            return List.copyOf(
                    roomManager.getAvailableRooms()
            );
        } catch (RuntimeException exception) {
            throw remoteFailure(
                    "Unable to retrieve available rooms.",
                    exception
            );
        }
    }

    @Override
    public RoomInfo getRoomInfo(String roomId)
            throws RemoteException {

        try {
            GameRoom room =
                    roomManager.requireRoom(roomId);

            return room.toRoomInfo();
        } catch (IllegalArgumentException
                 | IllegalStateException exception) {

            throw remoteFailure(
                    exception.getMessage(),
                    exception
            );
        }
    }

    @Override
    public List<PlayerView> getRoomPlayers(
            String roomId
    ) throws RemoteException {

        try {
            GameRoom room =
                    roomManager.requireRoom(roomId);

            return List.copyOf(
                    room.getPlayerViews()
            );
        } catch (IllegalArgumentException
                 | IllegalStateException exception) {

            throw remoteFailure(
                    exception.getMessage(),
                    exception
            );
        }
    }

    @Override
    public List<ScoreEntry> getLeaderboard()
            throws RemoteException {

        try {
            return List.copyOf(
                    statisticsRepository.getLeaderboard()
            );
        } catch (RuntimeException exception) {
            throw remoteFailure(
                    "Unable to retrieve the leaderboard.",
                    exception
            );
        }
    }

    @Override
    public PlayerStats getPlayerStats(
            String playerId
    ) throws RemoteException {

        if (playerId == null || playerId.isBlank()) {
            throw new RemoteException(
                    "Player ID is required."
            );
        }

        return statisticsRepository.findPlayerStats(
                        playerId.trim()
                )
                .orElseThrow(
                        () -> new RemoteException(
                                "Player statistics not found."
                        )
                );
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }

    private RemoteException remoteFailure(
            String message,
            Exception cause
    ) {
        String safeMessage =
                message == null || message.isBlank()
                        ? "RMI service operation failed."
                        : message;

        return new RemoteException(
                safeMessage,
                cause
        );
    }
}
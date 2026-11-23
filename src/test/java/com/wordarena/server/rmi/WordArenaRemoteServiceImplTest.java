package com.wordarena.server.rmi;

import com.wordarena.common.GameMode;
import com.wordarena.common.PlayerStats;
import com.wordarena.common.PlayerView;
import com.wordarena.common.RoomInfo;
import com.wordarena.common.ScoreEntry;
import com.wordarena.server.game.GamePlayer;
import com.wordarena.server.game.GameRoom;
import com.wordarena.server.game.RoomManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordArenaRemoteServiceImplTest {

    private RoomManager roomManager;
    private PlayerStatisticsRepository statisticsRepository;
    private WordArenaRemoteServiceImpl remoteService;

    @BeforeEach
    void setUp() throws RemoteException {
        roomManager = new RoomManager();

        statisticsRepository =
                new PlayerStatisticsRepository();

        remoteService =
                new WordArenaRemoteServiceImpl(
                        roomManager,
                        statisticsRepository
                );
    }

    @AfterEach
    void tearDown() throws Exception {
        UnicastRemoteObject.unexportObject(
                remoteService,
                true
        );
    }

    @Test
    void shouldRespondToPing() throws RemoteException {
        assertTrue(remoteService.ping());
    }

    @Test
    void shouldReturnAvailableRooms()
            throws RemoteException {

        GamePlayer host = new GamePlayer(
                "host-one",
                "Hedi"
        );

        GameRoom room = roomManager.createRoom(
                "RMI Duel",
                GameMode.DUEL,
                2,
                host
        );

        List<RoomInfo> availableRooms =
                remoteService.getAvailableRooms();

        assertEquals(
                1,
                availableRooms.size()
        );

        assertEquals(
                room.getRoomId(),
                availableRooms.getFirst().getRoomId()
        );

        assertEquals(
                "RMI Duel",
                availableRooms.getFirst().getRoomName()
        );
    }

    @Test
    void shouldReturnSpecificRoomInformation()
            throws RemoteException {

        GamePlayer host = new GamePlayer(
                "host-one",
                "Hedi"
        );

        GameRoom room = roomManager.createRoom(
                "Room Details",
                GameMode.COOPERATIVE,
                4,
                host
        );

        RoomInfo roomInfo =
                remoteService.getRoomInfo(
                        room.getRoomId()
                );

        assertEquals(
                room.getRoomId(),
                roomInfo.getRoomId()
        );

        assertEquals(
                GameMode.COOPERATIVE,
                roomInfo.getGameMode()
        );

        assertEquals(
                1,
                roomInfo.getCurrentPlayers()
        );

        assertFalse(roomInfo.isStarted());
    }

    @Test
    void shouldReturnRoomPlayers()
            throws RemoteException {

        GamePlayer host = new GamePlayer(
                "host-one",
                "Hedi"
        );

        GameRoom room = roomManager.createRoom(
                "Player List",
                GameMode.DUEL,
                2,
                host
        );

        GamePlayer secondPlayer = new GamePlayer(
                "player-two",
                "Youssef"
        );

        roomManager.joinRoom(
                room.getRoomId(),
                secondPlayer
        );

        room.setPlayerReady(
                host.getPlayerId(),
                true
        );

        List<PlayerView> players =
                remoteService.getRoomPlayers(
                        room.getRoomId()
                );

        assertEquals(
                2,
                players.size()
        );

        assertEquals(
                "Hedi",
                players.getFirst().getUsername()
        );

        assertTrue(
                players.getFirst().isReady()
        );

        assertEquals(
                "Youssef",
                players.get(1).getUsername()
        );
    }

    @Test
    void shouldReturnLeaderboardAndPlayerStatistics()
            throws RemoteException {

        statisticsRepository.recordGameResult(
                "player-one",
                "Hedi",
                true,
                900
        );

        statisticsRepository.recordGameResult(
                "player-two",
                "Youssef",
                false,
                0
        );

        List<ScoreEntry> leaderboard =
                remoteService.getLeaderboard();

        assertEquals(
                2,
                leaderboard.size()
        );

        assertEquals(
                "Hedi",
                leaderboard.getFirst().getUsername()
        );

        assertEquals(
                900,
                leaderboard.getFirst().getScore()
        );

        PlayerStats statistics =
                remoteService.getPlayerStats(
                        "player-one"
                );

        assertEquals(
                1,
                statistics.getGamesPlayed()
        );

        assertEquals(
                1,
                statistics.getGamesWon()
        );

        assertEquals(
                900,
                statistics.getTotalScore()
        );
    }

    @Test
    void shouldRejectUnknownRoom() {
        RemoteException exception =
                assertThrows(
                        RemoteException.class,
                        () -> remoteService.getRoomInfo(
                                "UNKNOWN"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .startsWith(
                                "Room not found."
                        )
        );

        assertTrue(
                exception.getCause()
                        instanceof IllegalStateException
        );
    }

    @Test
    void shouldRejectUnknownPlayerStatistics() {
        RemoteException exception =
                assertThrows(
                        RemoteException.class,
                        () -> remoteService.getPlayerStats(
                                "unknown-player"
                        )
                );

        assertEquals(
                "Player statistics not found.",
                exception.getMessage()
        );
    }
}
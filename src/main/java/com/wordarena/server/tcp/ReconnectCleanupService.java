package com.wordarena.server.tcp;

import com.wordarena.server.game.GameRoom;
import com.wordarena.server.game.RoomManager;
import com.wordarena.server.jms.GameEventNotifier;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReconnectCleanupService {

    public static final long DEFAULT_CLEANUP_INTERVAL_MILLIS =
            5_000L;

    private final ReconnectSessionManager
            reconnectSessionManager;

    private final RoomManager roomManager;

    private final PlayerSessionRegistry
            playerSessionRegistry;

    private final GameEventNotifier
            gameEventNotifier;

    private final ScheduledExecutorService
            cleanupExecutor;

    private final long cleanupIntervalMillis;

    private volatile boolean running;

    public ReconnectCleanupService(
            ReconnectSessionManager reconnectSessionManager,
            RoomManager roomManager,
            PlayerSessionRegistry playerSessionRegistry,
            GameEventNotifier gameEventNotifier
    ) {
        this(
                reconnectSessionManager,
                roomManager,
                playerSessionRegistry,
                gameEventNotifier,
                DEFAULT_CLEANUP_INTERVAL_MILLIS
        );
    }

    ReconnectCleanupService(
            ReconnectSessionManager reconnectSessionManager,
            RoomManager roomManager,
            PlayerSessionRegistry playerSessionRegistry,
            GameEventNotifier gameEventNotifier,
            long cleanupIntervalMillis
    ) {
        this.reconnectSessionManager =
                Objects.requireNonNull(
                        reconnectSessionManager,
                        "Reconnect session manager is required."
                );

        this.roomManager =
                Objects.requireNonNull(
                        roomManager,
                        "Room manager is required."
                );

        this.playerSessionRegistry =
                Objects.requireNonNull(
                        playerSessionRegistry,
                        "Player session registry is required."
                );

        this.gameEventNotifier =
                Objects.requireNonNull(
                        gameEventNotifier,
                        "Game event notifier is required."
                );

        if (cleanupIntervalMillis <= 0) {
            throw new IllegalArgumentException(
                    "Cleanup interval must be positive."
            );
        }

        this.cleanupIntervalMillis =
                cleanupIntervalMillis;

        this.cleanupExecutor =
                Executors.newSingleThreadScheduledExecutor(
                        runnable -> {
                            Thread thread =
                                    new Thread(
                                            runnable,
                                            "wordarena-reconnect-cleanup"
                                    );

                            thread.setDaemon(true);

                            return thread;
                        }
                );
    }

    public synchronized void start() {
        if (running) {
            return;
        }

        cleanupExecutor.scheduleWithFixedDelay(
                this::cleanupExpiredSessionsSafely,
                cleanupIntervalMillis,
                cleanupIntervalMillis,
                TimeUnit.MILLISECONDS
        );

        running = true;

        System.out.println(
                "Reconnect cleanup service started."
        );
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }

        cleanupExecutor.shutdownNow();
        running = false;

        System.out.println(
                "Reconnect cleanup service stopped."
        );
    }

    public void cleanupExpiredSessions() {
        List<ReconnectSession> expiredSessions =
                reconnectSessionManager
                        .removeExpiredSessions();

        for (ReconnectSession session :
                expiredSessions) {

            cleanupExpiredSession(
                    session
            );
        }
    }

    public boolean isRunning() {
        return running;
    }

    private void cleanupExpiredSessionsSafely() {
        try {
            cleanupExpiredSessions();
        } catch (RuntimeException exception) {
            System.err.println(
                    "Reconnect cleanup failed: "
                            + exception.getMessage()
            );
        }
    }

    private void cleanupExpiredSession(
            ReconnectSession session
    ) {
        session.getRoomId().ifPresent(
                roomId -> cleanupRoomMembership(
                        session,
                        roomId
                )
        );

        playerSessionRegistry.remove(
                session.playerId()
        );

        System.out.println(
                "Expired reconnect session removed: "
                        + session.playerId()
        );
    }

    private void cleanupRoomMembership(
            ReconnectSession session,
            String roomId
    ) {
        GameRoom room =
                roomManager.findRoom(roomId)
                        .orElse(null);

        if (room == null
                || !room.containsPlayer(
                session.playerId()
        )) {

            return;
        }

        roomManager.removePlayerAfterDisconnect(
                session.playerId()
        );

        gameEventNotifier.playerLeft(
                roomId,
                session.playerId(),
                session.username()
        );
    }
}
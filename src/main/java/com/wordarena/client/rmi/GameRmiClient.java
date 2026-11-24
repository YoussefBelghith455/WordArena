package com.wordarena.client.rmi;

import com.wordarena.common.PlayerStats;
import com.wordarena.common.PlayerView;
import com.wordarena.common.RoomInfo;
import com.wordarena.common.ScoreEntry;
import com.wordarena.common.remote.WordArenaRemoteService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameRmiClient implements AutoCloseable {

    private static final String DEFAULT_HOST = "localhost";
    private static final int REFRESH_INTERVAL = 5;

    private final String host;

    private final ScheduledExecutorService executor;

    private final ReadOnlyStringWrapper status =
            new ReadOnlyStringWrapper("DISCONNECTED");

    private final ObservableList<RoomInfo> availableRooms =
            FXCollections.observableArrayList();

    private final ObservableList<RoomInfo> readOnlyRooms =
            FXCollections.unmodifiableObservableList(availableRooms);

    private final AtomicBoolean connected =
            new AtomicBoolean(false);

    private final AtomicBoolean started =
            new AtomicBoolean(false);

    private final AtomicBoolean closed =
            new AtomicBoolean(false);

    private WordArenaRemoteService remoteService;

    public GameRmiClient() {
        this(DEFAULT_HOST);
    }

    public GameRmiClient(String host) {
        this.host = host;

        this.executor =
                Executors.newSingleThreadScheduledExecutor(task -> {
                    Thread thread = new Thread(task, "wordarena-rmi");
                    thread.setDaemon(true);
                    return thread;
                });
    }

    public void start() {
        if (closed.get() || !started.compareAndSet(false, true)) {
            return;
        }

        executor.scheduleWithFixedDelay(
                this::maintainConnection,
                0,
                REFRESH_INTERVAL,
                TimeUnit.SECONDS
        );
    }

    private void maintainConnection() {
        if (closed.get()) {
            return;
        }

        try {
            if (remoteService == null) {
                connect();
            }

            if (!remoteService.ping()) {
                throw new RemoteException("RMI ping failed");
            }

            List<RoomInfo> rooms =
                    remoteService.getAvailableRooms();

            connected.set(true);
            updateStatus("CONNECTED");
            updateRooms(rooms);

        } catch (Exception exception) {
            handleConnectionFailure();
        }
    }

    private void connect() throws Exception {
        updateStatus("CONNECTING");

        Registry registry = LocateRegistry.getRegistry(
                host,
                WordArenaRemoteService.REGISTRY_PORT
        );

        remoteService =
                (WordArenaRemoteService) registry.lookup(
                        WordArenaRemoteService.SERVICE_NAME
                );
    }

    private void handleConnectionFailure() {
        remoteService = null;
        connected.set(false);
        updateStatus("DISCONNECTED");
        updateRooms(List.of());
    }

    public void refreshRooms() {
        if (!closed.get()) {
            executor.execute(this::maintainConnection);
        }
    }

    public CompletableFuture<RoomInfo> getRoomInfo(String roomId) {
        return executeRemote(
                service -> service.getRoomInfo(roomId)
        );
    }

    public CompletableFuture<List<PlayerView>> getRoomPlayers(
            String roomId
    ) {
        return executeRemote(
                service -> service.getRoomPlayers(roomId)
        );
    }

    public CompletableFuture<List<ScoreEntry>> getLeaderboard() {
        return executeRemote(
                WordArenaRemoteService::getLeaderboard
        );
    }

    public CompletableFuture<PlayerStats> getPlayerStats(
            String playerId
    ) {
        return executeRemote(
                service -> service.getPlayerStats(playerId)
        );
    }

    private <T> CompletableFuture<T> executeRemote(
            RemoteOperation<T> operation
    ) {
        if (closed.get()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("RMI client is closed")
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (remoteService == null) {
                    connect();
                }

                T result = operation.execute(remoteService);

                connected.set(true);
                updateStatus("CONNECTED");

                return result;

            } catch (Exception exception) {
                handleConnectionFailure();
                throw new CompletionException(exception);
            }
        }, executor);
    }

    private void updateStatus(String value) {
        Platform.runLater(() -> {
            if (!closed.get()) {
                status.set(value);
            }
        });
    }

    private void updateRooms(List<RoomInfo> rooms) {
        List<RoomInfo> snapshot = List.copyOf(rooms);

        Platform.runLater(() -> {
            if (!closed.get()) {
                availableRooms.setAll(snapshot);
            }
        });
    }

    public ReadOnlyStringProperty statusProperty() {
        return status.getReadOnlyProperty();
    }

    public String getStatus() {
        return status.get();
    }

    public boolean isConnected() {
        return connected.get();
    }

    public ObservableList<RoomInfo> getAvailableRooms() {
        return readOnlyRooms;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        connected.set(false);
        executor.shutdownNow();

        Platform.runLater(() -> {
            status.set("DISCONNECTED");
            availableRooms.clear();
        });
    }

    @FunctionalInterface
    private interface RemoteOperation<T> {

        T execute(WordArenaRemoteService service)
                throws RemoteException;
    }
}
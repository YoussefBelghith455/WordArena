package com.wordarena.server.rmi;

import com.wordarena.common.remote.WordArenaRemoteService;
import com.wordarena.server.game.RoomManager;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Objects;

public class RmiServiceManager {

    private final RoomManager roomManager;
    private final PlayerStatisticsRepository
            statisticsRepository;

    private Registry registry;
    private WordArenaRemoteServiceImpl remoteService;
    private boolean running;

    public RmiServiceManager(
            RoomManager roomManager,
            PlayerStatisticsRepository statisticsRepository
    ) {
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

    public synchronized void start()
            throws RemoteException {

        if (running) {
            throw new IllegalStateException(
                    "The RMI service is already running."
            );
        }

        registry = LocateRegistry.createRegistry(
                WordArenaRemoteService.REGISTRY_PORT
        );

        remoteService =
                new WordArenaRemoteServiceImpl(
                        roomManager,
                        statisticsRepository
                );

        registry.rebind(
                WordArenaRemoteService.SERVICE_NAME,
                remoteService
        );

        running = true;

        System.out.println(
                "WordArena RMI service is available on port "
                        + WordArenaRemoteService.REGISTRY_PORT
                        + " under the name "
                        + WordArenaRemoteService.SERVICE_NAME
                        + "."
        );
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }

        try {
            registry.unbind(
                    WordArenaRemoteService.SERVICE_NAME
            );
        } catch (Exception exception) {
            System.err.println(
                    "Unable to unbind the RMI service: "
                            + exception.getMessage()
            );
        }

        unexportRemoteObject(
                remoteService,
                "RMI service"
        );

        unexportRemoteObject(
                registry,
                "RMI Registry"
        );

        remoteService = null;
        registry = null;
        running = false;

        System.out.println(
                "WordArena RMI service stopped."
        );
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public PlayerStatisticsRepository
    getStatisticsRepository() {

        return statisticsRepository;
    }

    private void unexportRemoteObject(
            Object remoteObject,
            String objectName
    ) {
        if (!(remoteObject
                instanceof java.rmi.Remote remote)) {

            return;
        }

        try {
            UnicastRemoteObject.unexportObject(
                    remote,
                    true
            );
        } catch (NoSuchObjectException exception) {
            System.err.println(
                    objectName
                            + " was already unexported."
            );
        }
    }
}
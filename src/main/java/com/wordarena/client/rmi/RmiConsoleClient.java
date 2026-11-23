package com.wordarena.client.rmi;

import com.wordarena.common.PlayerStats;
import com.wordarena.common.PlayerView;
import com.wordarena.common.RoomInfo;
import com.wordarena.common.ScoreEntry;
import com.wordarena.common.remote.WordArenaRemoteService;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class RmiConsoleClient {

    private static final String DEFAULT_HOST =
            "localhost";

    private final Scanner scanner;

    private WordArenaRemoteService remoteService;
    private boolean running;

    public RmiConsoleClient() {
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        try {
            connect();

            running = true;
            printHelp();
            runCommandLoop();
        } catch (RemoteException exception) {
            System.err.println(
                    "Unable to connect to the WordArena RMI service: "
                            + exception.getMessage()
            );
        } catch (NotBoundException exception) {
            System.err.println(
                    "The WordArena RMI service is not registered: "
                            + exception.getMessage()
            );
        } finally {
            scanner.close();
        }
    }

    private void connect()
            throws RemoteException, NotBoundException {

        Registry registry =
                LocateRegistry.getRegistry(
                        DEFAULT_HOST,
                        WordArenaRemoteService.REGISTRY_PORT
                );

        remoteService =
                (WordArenaRemoteService) registry.lookup(
                        WordArenaRemoteService.SERVICE_NAME
                );

        boolean available =
                remoteService.ping();

        if (!available) {
            throw new RemoteException(
                    "The RMI service did not answer the ping request."
            );
        }

        System.out.println(
                "Connected to WordArena RMI service."
        );

        System.out.println(
                "Host: " + DEFAULT_HOST
        );

        System.out.println(
                "Registry port: "
                        + WordArenaRemoteService.REGISTRY_PORT
        );

        System.out.println(
                "Service name: "
                        + WordArenaRemoteService.SERVICE_NAME
        );
    }

    private void runCommandLoop() {
        while (running) {
            System.out.print("rmi> ");

            if (!scanner.hasNextLine()) {
                break;
            }

            String command =
                    scanner.nextLine()
                            .trim()
                            .toLowerCase();

            try {
                switch (command) {
                    case "ping" -> ping();
                    case "rooms" -> displayAvailableRooms();
                    case "room" -> displayRoomInformation();
                    case "players" -> displayRoomPlayers();
                    case "leaderboard" -> displayLeaderboard();
                    case "stats" -> displayPlayerStatistics();
                    case "help" -> printHelp();
                    case "quit", "exit" -> running = false;

                    default -> System.out.println(
                            "Unknown command. Enter help "
                                    + "to display the available commands."
                    );
                }
            } catch (RemoteException exception) {
                System.err.println(
                        "RMI operation failed: "
                                + exception.getMessage()
                );
            }
        }
    }

    private void ping() throws RemoteException {
        System.out.println(
                "RMI service available: "
                        + remoteService.ping()
        );
    }

    private void displayAvailableRooms()
            throws RemoteException {

        List<RoomInfo> rooms =
                remoteService.getAvailableRooms();

        if (rooms.isEmpty()) {
            System.out.println(
                    "No available rooms."
            );

            return;
        }

        System.out.println(
                "Available rooms"
        );

        System.out.println(
                "--------------------------------------------------"
        );

        for (RoomInfo room : rooms) {
            System.out.println(
                    "ID: " + room.getRoomId()
            );

            System.out.println(
                    "Name: " + room.getRoomName()
            );

            System.out.println(
                    "Mode: " + room.getGameMode()
            );

            System.out.println(
                    "Players: "
                            + room.getCurrentPlayers()
                            + "/"
                            + room.getMaxPlayers()
            );

            System.out.println(
                    "Started: " + room.isStarted()
            );

            System.out.println(
                    "--------------------------------------------------"
            );
        }
    }

    private void displayRoomInformation()
            throws RemoteException {

        String roomId =
                askRequiredValue("Room ID");

        RoomInfo room =
                remoteService.getRoomInfo(roomId);

        System.out.println(
                "Room ID: " + room.getRoomId()
        );

        System.out.println(
                "Room name: " + room.getRoomName()
        );

        System.out.println(
                "Mode: " + room.getGameMode()
        );

        System.out.println(
                "Players: "
                        + room.getCurrentPlayers()
                        + "/"
                        + room.getMaxPlayers()
        );

        System.out.println(
                "Started: " + room.isStarted()
        );
    }

    private void displayRoomPlayers()
            throws RemoteException {

        String roomId =
                askRequiredValue("Room ID");

        List<PlayerView> players =
                remoteService.getRoomPlayers(roomId);

        if (players.isEmpty()) {
            System.out.println(
                    "The room has no players."
            );

            return;
        }

        for (PlayerView player : players) {
            System.out.println(
                    player.getUsername()
                            + " | ready="
                            + player.isReady()
                            + " | attempts="
                            + player.getAttemptsUsed()
                            + " | score="
                            + player.getScore()
                            + " | solved="
                            + player.isSolved()
            );
        }
    }

    private void displayLeaderboard()
            throws RemoteException {

        List<ScoreEntry> leaderboard =
                remoteService.getLeaderboard();

        if (leaderboard.isEmpty()) {
            System.out.println(
                    "The leaderboard is empty."
            );

            return;
        }

        for (ScoreEntry entry : leaderboard) {
            System.out.println(
                    entry.getRank()
                            + ". "
                            + entry.getUsername()
                            + " - "
                            + entry.getScore()
                            + " points"
            );
        }
    }

    private void displayPlayerStatistics()
            throws RemoteException {

        String playerId =
                askRequiredValue("Player ID");

        PlayerStats statistics =
                remoteService.getPlayerStats(playerId);

        System.out.println(
                "Username: " + statistics.getUsername()
        );

        System.out.println(
                "Games played: "
                        + statistics.getGamesPlayed()
        );

        System.out.println(
                "Games won: "
                        + statistics.getGamesWon()
        );

        System.out.println(
                "Games lost: "
                        + statistics.getGamesLost()
        );

        System.out.println(
                "Total score: "
                        + statistics.getTotalScore()
        );
    }

    private String askRequiredValue(String fieldName) {
        while (true) {
            System.out.print(fieldName + ": ");

            String value =
                    scanner.nextLine().trim();

            if (!value.isBlank()) {
                return value;
            }

            System.out.println(
                    fieldName + " is required."
            );
        }
    }

    private void printHelp() {
        System.out.println();
        System.out.println(
                "Available RMI commands"
        );

        System.out.println(
                "--------------------------------------------------"
        );

        System.out.println(
                "ping        - Check the remote service"
        );

        System.out.println(
                "rooms       - List available rooms"
        );

        System.out.println(
                "room        - Display one room"
        );

        System.out.println(
                "players     - Display players in a room"
        );

        System.out.println(
                "leaderboard - Display global ranking"
        );

        System.out.println(
                "stats       - Display one player's statistics"
        );

        System.out.println(
                "help        - Display this menu"
        );

        System.out.println(
                "quit        - Close the RMI client"
        );

        System.out.println(
                "--------------------------------------------------"
        );
    }

    public static void main(String[] args) {
        new RmiConsoleClient().start();
    }
}
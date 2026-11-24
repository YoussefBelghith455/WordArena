package com.wordarena.client.tcp;

import com.wordarena.common.GameMessage;
import com.wordarena.server.tcp.GameServer;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ConsoleLoginClient {

    private static final String SERVER_HOST = "localhost";

    private final Scanner scanner;

    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    private String playerId;
    private String currentRoomId;
    private volatile boolean running;

    public ConsoleLoginClient() {
        scanner = new Scanner(System.in);
    }

    public void start() {
        try {
            connect();

            if (!login()) {
                return;
            }

            running = true;
            startServerMessageListener();
            runCommandLoop();
        } catch (IOException exception) {
            System.err.println(
                    "Unable to communicate with the WordArena server: "
                            + exception.getMessage()
            );
        } catch (ClassNotFoundException exception) {
            System.err.println(
                    "The server response type is not supported: "
                            + exception.getMessage()
            );
        } finally {
            closeConnection();
        }
    }

    private void connect() throws IOException {
        socket = new Socket(
                SERVER_HOST,
                GameServer.DEFAULT_PORT
        );

        output = new ObjectOutputStream(
                socket.getOutputStream()
        );

        output.flush();

        input = new ObjectInputStream(
                socket.getInputStream()
        );

        System.out.println(
                "Connected to WordArena server at "
                        + SERVER_HOST
                        + ":"
                        + GameServer.DEFAULT_PORT
        );
    }

    private boolean login()
            throws IOException, ClassNotFoundException {

        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        sendMessage(
                new GameMessage(
                        "LOGIN",
                        "",
                        "",
                        username
                )
        );

        Object receivedObject =
                input.readObject();

        if (!(receivedObject
                instanceof GameMessage response)) {

            System.err.println(
                    "The server returned an unsupported object."
            );

            return false;
        }

        printMessage(response);

        if (!"OK".equals(response.getType())) {
            return false;
        }

        playerId = response.getPlayerId();

        return true;
    }

    private void startServerMessageListener() {
        Thread listenerThread = new Thread(
                this::listenForServerMessages,
                "wordarena-console-listener"
        );

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listenForServerMessages() {
        while (running) {
            try {
                Object receivedObject =
                        input.readObject();

                if (receivedObject
                        instanceof GameMessage response) {

                    updateLocalState(response);

                    System.out.println();
                    printMessage(response);
                    printPrompt();
                } else {
                    System.err.println(
                            "Received an unsupported server object."
                    );
                }
            } catch (EOFException exception) {
                if (running) {
                    System.out.println();
                    System.out.println(
                            "The server closed the connection."
                    );
                }

                running = false;
            } catch (
                    IOException
                    | ClassNotFoundException exception
            ) {
                if (running) {
                    System.err.println();
                    System.err.println(
                            "Server listener stopped: "
                                    + exception.getMessage()
                    );
                }

                running = false;
            }
        }
    }

    private void updateLocalState(
            GameMessage response
    ) {
        if ("ROOM_STATE".equals(
                response.getType()
        )
                && response.getRoomId() != null
                && !response.getRoomId().isBlank()) {

            currentRoomId =
                    response.getRoomId();
        }

        if ("OK".equals(response.getType())
                && response.getContent() != null
                && response.getContent().contains(
                "Successfully left the room."
        )) {

            currentRoomId = null;
        }
    }

    private void runCommandLoop()
            throws IOException {

        printHelp();

        while (running) {
            printPrompt();

            if (!scanner.hasNextLine()) {
                break;
            }

            String command =
                    scanner.nextLine()
                            .trim()
                            .toLowerCase();

            switch (command) {
                case "create" -> createRoom();
                case "join" -> joinRoom();
                case "leave" -> leaveRoom();
                case "ready" -> setReady();
                case "lock" -> lockSecretWord();
                case "start" -> startRound();
                case "guess" -> submitGuess();
                case "room" -> showCurrentRoom();
                case "help" -> printHelp();
                case "quit", "exit" ->
                        running = false;

                default -> System.out.println(
                        "Unknown command. Enter help "
                                + "to see the available commands."
                );
            }
        }
    }

    private void createRoom()
            throws IOException {

        System.out.print("Room name: ");
        String roomName =
                scanner.nextLine();

        System.out.print(
                "Game mode "
                        + "(DUEL, COOPERATIVE, HOST, DOUBLE_HOST): "
        );

        String gameMode =
                scanner.nextLine()
                        .trim()
                        .toUpperCase();

        System.out.print("Maximum players: ");
        String maxPlayers =
                scanner.nextLine().trim();

        String content =
                roomName
                        + "|"
                        + gameMode
                        + "|"
                        + maxPlayers;

        sendMessage(
                new GameMessage(
                        "CREATE_ROOM",
                        playerId,
                        "",
                        content
                )
        );
    }

    private void joinRoom()
            throws IOException {

        System.out.print("Room ID: ");
        String roomId =
                scanner.nextLine().trim();

        sendMessage(
                new GameMessage(
                        "JOIN_ROOM",
                        playerId,
                        roomId,
                        ""
                )
        );
    }

    private void leaveRoom()
            throws IOException {

        String roomId =
                requireCurrentRoomId();

        if (roomId == null) {
            return;
        }

        sendMessage(
                new GameMessage(
                        "LEAVE_ROOM",
                        playerId,
                        roomId,
                        ""
                )
        );
    }

    private void setReady()
            throws IOException {

        String roomId =
                requireCurrentRoomId();

        if (roomId == null) {
            return;
        }

        System.out.print(
                "Ready state (true/false): "
        );

        String ready =
                scanner.nextLine().trim();

        sendMessage(
                new GameMessage(
                        "SET_READY",
                        playerId,
                        roomId,
                        ready
                )
        );
    }

    private void lockSecretWord()
            throws IOException {

        String roomId =
                requireCurrentRoomId();

        if (roomId == null) {
            return;
        }

        System.out.print(
                "Five-letter secret word: "
        );

        String secretWord =
                scanner.nextLine()
                        .trim()
                        .toUpperCase();

        sendMessage(
                new GameMessage(
                        "LOCK_WORD",
                        playerId,
                        roomId,
                        secretWord
                )
        );
    }

    private void startRound()
            throws IOException {

        String roomId =
                requireCurrentRoomId();

        if (roomId == null) {
            return;
        }

        sendMessage(
                new GameMessage(
                        "START_ROUND",
                        playerId,
                        roomId,
                        ""
                )
        );
    }

    private void submitGuess()
            throws IOException {

        String roomId =
                requireCurrentRoomId();

        if (roomId == null) {
            return;
        }

        System.out.print(
                "Five-letter guess: "
        );

        String guessedWord =
                scanner.nextLine()
                        .trim()
                        .toUpperCase();

        sendMessage(
                new GameMessage(
                        "GUESS",
                        playerId,
                        roomId,
                        guessedWord
                )
        );
    }

    private void showCurrentRoom() {
        if (currentRoomId == null
                || currentRoomId.isBlank()) {

            System.out.println(
                    "You are not currently in a room."
            );

            return;
        }

        System.out.println(
                "Current room ID: "
                        + currentRoomId
        );
    }

    private String requireCurrentRoomId() {
        if (currentRoomId == null
                || currentRoomId.isBlank()) {

            System.out.println(
                    "You are not currently in a room."
            );

            return null;
        }

        return currentRoomId;
    }

    private synchronized void sendMessage(
            GameMessage message
    ) throws IOException {

        output.writeObject(message);
        output.flush();
        output.reset();
    }

    private void printMessage(
            GameMessage message
    ) {
        System.out.println(
                "------------------------------"
        );

        System.out.println(
                "Type: " + message.getType()
        );

        System.out.println(
                "Player ID: "
                        + message.getPlayerId()
        );

        System.out.println(
                "Room ID: "
                        + message.getRoomId()
        );

        System.out.println(
                "Content: "
                        + message.getContent()
        );

        System.out.println(
                "------------------------------"
        );
    }

    private void printHelp() {
        System.out.println();
        System.out.println(
                "Available commands"
        );

        System.out.println(
                "------------------------------"
        );

        System.out.println(
                "create - Create a room"
        );

        System.out.println(
                "join   - Join a room"
        );

        System.out.println(
                "leave  - Leave the current room"
        );

        System.out.println(
                "ready  - Change ready state"
        );

        System.out.println(
                "lock   - Lock the Host-mode secret word"
        );

        System.out.println(
                "start  - Start the current round"
        );

        System.out.println(
                "guess  - Submit a five-letter guess"
        );

        System.out.println(
                "room   - Display the current room ID"
        );

        System.out.println(
                "help   - Display this menu"
        );

        System.out.println(
                "quit   - Disconnect"
        );

        System.out.println(
                "------------------------------"
        );
    }

    private void printPrompt() {
        System.out.print("> ");
    }

    private void closeConnection() {
        running = false;

        if (socket != null
                && !socket.isClosed()) {

            try {
                socket.close();
            } catch (IOException exception) {
                System.err.println(
                        "Unable to close the client socket: "
                                + exception.getMessage()
                );
            }
        }
    }

    public static void main(String[] args) {
        new ConsoleLoginClient().start();
    }
}
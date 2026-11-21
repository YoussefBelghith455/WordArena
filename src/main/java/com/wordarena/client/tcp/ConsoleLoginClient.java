package com.wordarena.client.tcp;

import com.wordarena.common.GameMessage;
import com.wordarena.server.tcp.GameServer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ConsoleLoginClient {

    private static final String SERVER_HOST = "localhost";

    public static void main(String[] args) {
        try (
                Scanner scanner = new Scanner(System.in);
                Socket socket = new Socket(
                        SERVER_HOST,
                        GameServer.DEFAULT_PORT
                );
                ObjectOutputStream output =
                        new ObjectOutputStream(
                                socket.getOutputStream()
                        )
        ) {
            output.flush();

            try (
                    ObjectInputStream input =
                            new ObjectInputStream(
                                    socket.getInputStream()
                            )
            ) {
                System.out.println(
                        "Connected to WordArena server at "
                                + SERVER_HOST
                                + ":"
                                + GameServer.DEFAULT_PORT
                );

                System.out.print("Enter username: ");
                String username = scanner.nextLine();

                GameMessage loginRequest = new GameMessage(
                        "LOGIN",
                        "",
                        "",
                        username
                );

                output.writeObject(loginRequest);
                output.flush();
                output.reset();

                Object receivedObject = input.readObject();

                if (!(receivedObject instanceof GameMessage response)) {
                    System.err.println(
                            "The server returned an unsupported object."
                    );
                    return;
                }

                printResponse(response);

                if ("OK".equals(response.getType())) {
                    System.out.println();
                    System.out.println(
                            "The connection will remain open."
                    );
                    System.out.println(
                            "Press ENTER to disconnect."
                    );

                    scanner.nextLine();
                }
            }
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
        }
    }

    private static void printResponse(GameMessage response) {
        System.out.println();
        System.out.println("Server response");
        System.out.println("------------------------------");
        System.out.println("Type: " + response.getType());
        System.out.println(
                "Player ID: " + response.getPlayerId()
        );
        System.out.println(
                "Room ID: " + response.getRoomId()
        );
        System.out.println(
                "Message: " + response.getContent()
        );
    }
}
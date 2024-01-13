package org.projektpo2;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Scanner;

public class ConsoleHandler implements Runnable {
    private final ServerSocket serverSocket;

    private Server sv;
    public ConsoleHandler(ServerSocket serverSocket, Server srv) {
        this.serverSocket = serverSocket;
        this.sv=srv;
    }

    @Override
    public void run() {
        System.out.println("Konsola jest aktywna.");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String command = scanner.nextLine();
            switch (command.toLowerCase()) {
                case "test":
                    handleTestCommand();
                    break;
                case "exit":
                case "stop":
                    handleExitCommand();
                    return;
                default:
                    handleUnknownCommand();
            }
        }
    }

    private void handleTestCommand() {
        System.out.println("TEST");
    }

    private void handleExitCommand() {
        System.out.println("Zatrzymywanie serwera...");
        try {
            sv.closing = true;
            sv.executor.shutdown();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleUnknownCommand() {
        System.out.println("Nieznana komenda.");
    }
}

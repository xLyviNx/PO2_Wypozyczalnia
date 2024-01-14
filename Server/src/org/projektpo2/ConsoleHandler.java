package org.projektpo2;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasa obsługująca konsolę serwera.
 */
public class ConsoleHandler implements Runnable {
    private static final Logger logger = Utilities.getLogger(ConsoleHandler.class);

    /** Gniazdo serwera. */
    private final ServerSocket serverSocket;

    /** Obiekt serwera. */
    private Server sv;

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     *
     * @param serverSocket Gniazdo serwera.
     * @param srv          Obiekt serwera.
     */
    public ConsoleHandler(ServerSocket serverSocket, Server srv) {
        this.serverSocket = serverSocket;
        this.sv = srv;
    }

    /**
     * Metoda uruchamiająca wątek obsługujący konsolę serwera.
     */
    @Override
    public void run() {
        logger.info("Konsola jest aktywna.");
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

    /**
     * Metoda obsługująca komendę test.
     */
    private void handleTestCommand() {
        logger.info("TEST");
    }

    /**
     * Metoda obsługująca komendę exit lub stop.
     */
    private void handleExitCommand() {
        logger.info("Zatrzymywanie serwera...");
        try {
            sv.closing = true;
            sv.executor.shutdown();
            serverSocket.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Błąd zatrzymywania serwera", e);
        }
    }

    /**
     * Metoda obsługująca nieznane komendy.
     */
    private void handleUnknownCommand() {
        logger.warning("Nieznana komenda.");
    }
}

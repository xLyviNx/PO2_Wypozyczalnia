package src;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class Server {
    final int port = 12345;
    private ServerSocket serverSocket;

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        ExecutorService executor = Executors.newCachedThreadPool();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            executor.submit(() -> handleClient(clientSocket));
        }
    }

    private void handleClient(Socket socket) {
        try (DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            System.out.println("CLIENT CONNECTED " + socket);
            output.writeUTF("CONNECTED");
            String receivedString;

            while (true) {
                receivedString = input.readUTF();
                if (receivedString.equals("Exit")) {
                    System.out.println("Client " + socket + " sends exit...");
                    System.out.println("Connection closing...");
                    socket.close();
                    System.out.println("Closed");
                    break;
                }
                switch (receivedString) {
                    default:
                        output.writeUTF("REQUEST TAKEN");
                        break;
                }
            }
        } catch (SocketException e) {
            System.out.println("Client " + socket + " unexpectedly closed the connection.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

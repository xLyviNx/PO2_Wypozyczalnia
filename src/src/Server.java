package src;
import com.google.gson.Gson;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.*;

public class Server {
    final int port = 12345;
    public ArrayList<User> connectedClients = new ArrayList<User>();
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
        User session = null;
        try (DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            System.out.println("CLIENT CONNECTED " + socket);
            session = new User();
            session.isSignedIn=false;
            session.clientSocket=socket;
            connectedClients.add(session);

            output.writeUTF("CONNECTED");
            String receivedString;

            while (true) {
                receivedString = input.readUTF();
                if (receivedString.equals("Exit")) {
                    System.out.println("Client " + socket + " sends exit...");
                    System.out.println("Connection closing...");
                    socket.close();
                    System.out.println("Closed");
                    if (session != null)
                    {
                        connectedClients.remove(session);
                        session = null;
                    }
                    break;
                }else
                {
                    NetData data = NetData.fromJSON(receivedString);
                    if (data != null)
                    {
                        switch (data.operation)
                        {
                            case Register -> {

                                break;
                            }
                            default ->
                            {
                                System.out.println(socket + " requested unknown operation.");
                                break;
                            }
                        }
                    }
                    else
                    {
                        System.out.println("ERROR TRANSLATING DATA FROM " + socket);
                    }
                }
            }
        } catch (SocketException e) {
            System.out.println("Client " + socket + " unexpectedly closed the connection.");
            if (session != null)
            {
                connectedClients.remove(session);
                session = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (session != null)
            {
                connectedClients.remove(session);
                session = null;
            }
        }
        finally
        {
            if (session != null)
            {
                connectedClients.remove(session);
                session = null;
            }
        }
    }
}
class User
{
    public boolean isSignedIn;
    public Socket clientSocket;
}

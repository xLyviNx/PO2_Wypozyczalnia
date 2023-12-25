package src;
import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    public static Client instance;
    public Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    public void start(String address, int port) {
        if (instance != null)
            return;
        NetData dat = new NetData("TEST");

        try {
            instance=this;
            socket = new Socket(address, port);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            Scanner scanner = new Scanner(System.in);
            String receivedString;

            while (socket.isConnected()) {

            }
        } catch (ConnectException e) {
            System.out.println("Nie można połączyć się z serwerem: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
                if (output != null) {
                    output.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (instance==this)
            instance=null;
    }
    String SendRequest(String request)
    {
        return "";
    }

}

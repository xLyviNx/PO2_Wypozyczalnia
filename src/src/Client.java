package src;
import java.net.*;
import java.io.*;
import java.nio.Buffer;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Client {
    public static Client instance;
    public Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private BufferedInputStream bufInput;
    private BufferedOutputStream bufOutput;
    public void start(String address, int port) throws Exception {
        if (instance != null && instance!=this)
            return;
        //NetData dat = new NetData("TEST");
        while(WypozyczalniaOkno.instance == null)
        {
            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("WINDOW FOUND");
        try {
            instance=this;
            socket = new Socket(address, port);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            bufInput = new BufferedInputStream(input);
            bufOutput = new BufferedOutputStream(output);
            Scanner scanner = new Scanner(System.in);
            String receivedString;

            while (socket.isConnected() && WypozyczalniaOkno.instance != null)
            {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException iex) {
                    throw new RuntimeException(iex);
                }
                try {
                    output.writeUTF("ping");
                    output.flush();
                }catch (SocketException socketexc)
                {
                    System.out.println("ERROR PINGING");
                    throw new DisconnectException();
                }
            }
            System.out.println("DISCONNECTED.");
        } catch (ConnectException e) {
            System.out.println("Nie można połączyć się z serwerem: " + e.getMessage());
            throw new DisconnectException();
        } catch (IOException e) {
            e.printStackTrace();
            throw new DisconnectException();
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
        System.out.println("DC");
        return;
    }
    String SendRequest(String request)
    {
        return "";
    }

}


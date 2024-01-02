package src;

import java.io.IOException;
import java.nio.file.attribute.AclEntryType;
import java.util.Set;

public class Main
{
    public static String ip;
    public static int port;
    public static void main(String[] args)
    {
        if (args.length==8)
        {
            if (args[0].trim().equals("server")) {
                System.out.println("Server is starting...");
                ip = args[1];
                port = Integer.parseInt(args[2].trim());
                DatabaseHandler.setCredentials(args[3], args[4], args[5], args[6], args[7]);
                Server sv = new Server();
                try {
                    sv.start(ip, port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else if (args.length == 2) {
            System.out.println("Client is starting...");
            try
            {
                ip = args[0];
                port = Integer.parseInt(args[1].trim());
                RunClient();
                WypozyczalniaOkno.main(args);
            }catch (Exception ex)
            {
                ex.printStackTrace();
            }

        }

    }
    public static void RunClient()
    {
        Client cl;
        if (Client.instance != null)
        {
            cl=Client.instance;
        }else {
            cl = new Client();
        }
        Thread clTh = new Thread(() -> {
            try {
                cl.start(ip, port);
            } catch (DisconnectException e) {
                System.out.println("NO CONNECTION");
                if (WypozyczalniaOkno.instance != null)
                    WypozyczalniaOkno.instance.NoConnection();
                throw e;
            }catch (Exception ex)
            {

            }
        });
        clTh.setName("CLIENT THREAD");
        clTh.start();
    }
}
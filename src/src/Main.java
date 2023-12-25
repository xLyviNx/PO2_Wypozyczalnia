package src;

import java.io.IOException;

public class Main
{
    public static void main(String[] args)
    {
        if (args.length>0)
        {
            if (args[0].trim().equals("server"))
            {
                System.out.println("Server is starting...");
                Server sv = new Server();
                try {
                    sv.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return;
            }else if (args[0].trim().equals("console-client"))
            {
                return;
            }

        }
        System.out.println("Client is starting...");
        Client cl = new Client();
        Thread clTh = new Thread(() -> cl.start("localhost", 12345));
        clTh.start();
        WypozyczalniaOkno.main(args);
    }
}
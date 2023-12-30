package src;

import java.io.IOException;
import java.nio.file.attribute.AclEntryType;
import java.util.Set;

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
        RunClient();
        WypozyczalniaOkno.main(args);
    }
    public static void RunClient()
    {
        /*Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread t: threadSet) {
            if (t.isAlive())
            System.out.println(t.getName());
        }*/
        Client cl;
        if (Client.instance != null)
        {
            cl=Client.instance;
        }else {
            cl = new Client();
        }
        Thread clTh = new Thread(() -> {
            try {
                cl.start("localhost", 12345);
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
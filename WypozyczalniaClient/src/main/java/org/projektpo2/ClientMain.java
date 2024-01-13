package org.projektpo2;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import java.io.File;

public class ClientMain {
    public static String pathToJar;
    public static String imagePath;
    public static String ip;
    public static int port;


    public static void setupClient(String ip, int port)
    {
        ClientMain.ip=ip;
        ClientMain.port=port;
        setupPaths();
    }
    public static void runClient() {
        Client cl;
        if (Client.instance != null) {
            cl = Client.instance;
        } else {
            cl = new Client();
        }
        WypozyczalniaOkno wypozyczalniaOkno = WypozyczalniaOkno.instance;
        Thread clTh = new Thread(() -> {
            try {
                cl.start(ip, port);
            } catch (DisconnectException e) {
                System.out.println("NO CONNECTION");
                DisplayNoConnection();
                throw e;
            } catch (Exception ex) {
                ex.printStackTrace();
                Client.MessageBox(ex.getLocalizedMessage(), Alert.AlertType.ERROR);
                DisplayNoConnection();
            }
        });

        clTh.setName("CLIENT THREAD");
        try
        {
            clTh.start();
        }
        catch(Exception ex)
        {
            Client.MessageBox(ex.getLocalizedMessage(), Alert.AlertType.ERROR);
            DisplayNoConnection();
        }
    }
    private static void DisplayNoConnection()
    {
        if (WypozyczalniaOkno.instance != null)
            WypozyczalniaOkno.instance.NoConnection();
        else
            Platform.exit();
    }

    private static void setupPaths() {
        pathToJar = ClientMain.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (pathToJar.endsWith(".jar")) {
            System.out.println("Program jest uruchamiany z pliku JAR.");
            setJarImagePath();
        } else {
            System.out.println("Program nie jest uruchamiany z pliku JAR.");
        }
    }

    private static void setJarImagePath() {
        File jarFile = new File(pathToJar);
        imagePath = jarFile.getParent() + File.separator + "img/";
    }
    public static void main(String[] args)
    {
        WypozyczalniaOkno.main(args);
    }
}

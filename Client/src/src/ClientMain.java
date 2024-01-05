package src;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class ClientMain {
    public static String pathToJar;
    public static String imagePath;
    public static String ip;
    public static int port;

    public static void main(String[] args) {
        setupPaths();
        if (args.length == 2) {
            startClient(args);
        } else {
            showArgumentError();
        }
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

    private static void startClient(String[] args) {
        try {
            ip = args[0];
            port = Integer.parseInt(args[1].trim());
            runClient();

            final WypozyczalniaOkno mainStage = new WypozyczalniaOkno();
            WypozyczalniaOkno.instance = mainStage;
            mainStage.init();

            Platform.startup(() -> {
                Stage stage = new Stage();
                try {
                    mainStage.start(stage);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void runClient() {
        Client cl;
        if (Client.instance != null) {
            cl = Client.instance;
        } else {
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
            } catch (Exception ex)
            {

            }
        });

        clTh.setName("CLIENT THREAD");
        clTh.start();
    }

    private static void showArgumentError() {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle("BŁĄD");
        errorAlert.setHeaderText("Brakuje argumentów!");
        errorAlert.setContentText("Aby uruchomić aplikację, musisz dodać argumenty (dla klienta: IP, PORT");
        errorAlert.show();
    }
}

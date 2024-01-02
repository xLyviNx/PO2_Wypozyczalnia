package src;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.AclEntryType;
import java.util.Set;

public class Main
{
    public static String pathToJar;
    public static String imagePath;
    public static boolean isJar;
    public static String ip;
    public static int port;
    public static void main(String[] args)
    {
        pathToJar = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (pathToJar.endsWith(".jar")) {
            System.out.println("Program jest uruchamiany z pliku JAR.");
            File jarFile = new File(pathToJar);
            imagePath = jarFile.getParent() + File.separator + "img/";
        } else {
            System.out.println("Program nie jest uruchamiany z pliku JAR.");
            ClassLoader classLoader = Server.class.getClassLoader();
            URL resourceUrl = classLoader.getResource("");
            if (resourceUrl != null) {
                String classpath = new File(resourceUrl.getFile()).getPath();
                imagePath = classpath + File.separator + "img/";
            }
            else{
                System.out.println("Nie mozna utworzyc sciezki img.");
                return;
            }
        }

        if (args.length==8)
        {
            if (args[0].trim().equals("server")) {
                System.out.println("Images path: " + imagePath);
                Path folder = Paths.get(imagePath);
                if (!Files.exists(folder)) {
                    try {
                        Files.createDirectories(folder);
                        System.out.println("Katalog img został utworzony pomyślnie.");
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("Błąd podczas tworzenia katalogu img.");
                        return;
                    }
                }
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

                final WypozyczalniaOkno mainStage = new WypozyczalniaOkno();
                WypozyczalniaOkno.instance=mainStage;
                mainStage.init();

                Platform.startup(() -> {
                    Stage stage = new Stage();
                    try {
                        mainStage.start(stage);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
        else
        {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("BŁĄD");
            errorAlert.setHeaderText("Brakuje argumentów!");
            errorAlert.setContentText("Aby uruchomić aplikację, musisz dodać argumenty (dla klienta: IP, PORT, dla serwera: server IP PORT IP_BAZY PORT_BAZY NAZWA_BAZY UZYTKOWNIK_MYSQL HASLO_MYSQL");
            errorAlert.show();
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
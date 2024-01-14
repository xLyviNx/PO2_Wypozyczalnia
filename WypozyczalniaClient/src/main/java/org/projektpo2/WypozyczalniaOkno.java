package org.projektpo2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.projektpo2.controllers.NoConnectionController;
import org.projektpo2.controllers.StartPageController;

import java.util.logging.*;

/**
 * Klasa reprezentująca główne okno aplikacji.
 */
public class WypozyczalniaOkno extends Application {
    /** Instancja głównego okna. */
    public static WypozyczalniaOkno instance;
    /** Główny stage JavaFX. */
    private static Stage primaryStage;
    /** Obiekt do obsługi logów aplikacji. */
    private static final Logger logger = Utilities.getLogger(WypozyczalniaOkno.class);

    /**
     * Metoda startująca główne okno aplikacji.
     *
     * @param primaryStage Główny stage JavaFX.
     */
    @Override
    public void start(Stage primaryStage) {
        String ip = null;
        int port = 0;
        if (getParameters().getRaw().size() == 2) {
            ip = getParameters().getRaw().get(0);
            port = Integer.parseInt(getParameters().getRaw().get(1));
        } else {
            IPAndPortInputPopup input = new IPAndPortInputPopup();
            input.showInputDialog();
            try {
                ip = input.ip;
                port = Integer.parseInt(input.port);
            } catch (Exception ex) {
                Platform.exit();
            }
        }
        WypozyczalniaOkno.primaryStage = primaryStage;
        instance = this;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/projektpo2/fxml/StartPage.fxml"));
            Parent root = loader.load();

            StartPageController controller = loader.getController();
            Scene scene = new Scene(root, 1280, 720);
            String css = this.getClass().getResource("/org/projektpo2/fxml/style1.css").toExternalForm();
            scene.getStylesheets().add(css);
            primaryStage.setScene(scene);
            primaryStage.show();

            primaryStage.setOnCloseRequest(t -> {
                if (Client.instance!=null)
                {
                    Client.instance.SendQuit();
                }
                Platform.exit();
                //System.exit(1);
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getClass().toString(), e);
        }
        startClient(ip, port);
    }

    /**
     * Metoda wyświetlająca główny ekran aplikacji.
     */
    public void MainScene() {
        Platform.runLater(() -> {
            try {
                StartPageController spage = new StartPageController();
                spage.load_scene();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.getClass().toString(), ex);
            }
        });
    }

    /**
     * Metoda wyświetlająca ekran informujący o braku połączenia.
     */
    public void NoConnection() {
        Platform.runLater(() -> {
            try {
                NoConnectionController ncon = new NoConnectionController();
                ncon.load_scene();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.getClass().toString(), ex);
            }
        });
    }

    /**
     * Metoda zwracająca główny stage JavaFX.
     *
     * @return Główny stage JavaFX.
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Metoda startująca klienta.
     *
     * @param ip   Adres IP serwera.
     * @param port Numer portu serwera.
     */
    private void startClient(String ip, int port) {
        ClientMain.setupClient(ip, port);
        ClientMain.runClient();
    }

    /**
     * Metoda uruchamiająca główne okno aplikacji.
     *
     * @param args Argumenty przekazywane przy uruchamianiu aplikacji.
     */
    public static void main(String[] args) {
        launch(args);
    }
}

package org.projektpo2.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.projektpo2.ClientMain;
import org.projektpo2.Utilities;
import org.projektpo2.WypozyczalniaOkno;

import java.io.IOException;
import java.util.logging.*;
/**
 * Kontroler obsługujący brak połączenia z serwerem.
 */
public class NoConnectionController {

    /**
     * Logger służący do logowania zdarzeń w klasie NoConnectionController.
     */
    private static final Logger logger = Utilities.getLogger(NoConnectionController.class);

    /**
     * Obsługuje przycisk restartu.
     *
     * Uruchamia ponownie klienta, zamyka główne okno w przypadku braku dostępu do instancji WypozyczalniaOkno,
     * a następnie wczytuje scenę główną.
     */
    @FXML
    public void RestartButton() {
        try {
            logger.info("RESTART button clicked");
            ClientMain.runClient();

            if (WypozyczalniaOkno.instance == null) {
                logger.info("Closing primary stage");
                WypozyczalniaOkno.getPrimaryStage().close();
            }

            logger.info("Loading main scene");
            WypozyczalniaOkno.instance.MainScene();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during restart: " + e.getMessage(), e);
        }
    }

    /**
     * Wczytuje scenę braku połączenia.
     */
    @FXML
    public void load_scene() {
        try {
            logger.info("Loading NoConnection scene");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/projektpo2/fxml/NoConnection.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 720);
            String css = this.getClass().getResource("/org/projektpo2/fxml/style1.css").toExternalForm();
            scene.getStylesheets().add(css);
            WypozyczalniaOkno.getPrimaryStage().setScene(scene);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading NoConnection scene: " + e.getMessage(), e);
        }
    }
}

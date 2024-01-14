package org.projektpo2.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import org.projektpo2.Utilities;
import org.projektpo2.WypozyczalniaOkno;

import java.io.IOException;
import java.util.logging.*;

/**
 * Kontroler obsługujący widok strony startowej.
 */
public class StartPageController {
    private static final Logger logger = Utilities.getLogger(StartPageController.class);

    /**
     * Metoda wczytująca scenę strony startowej.
     */
    @FXML
    public void load_scene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/projektpo2/fxml/StartPage.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 720);
            String css = this.getClass().getResource("/org/projektpo2/fxml/style1.css").toExternalForm();
            scene.getStylesheets().add(css);
            WypozyczalniaOkno.getPrimaryStage().setScene(scene);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading StartPage.fxml: " + e.getMessage(), e);
        }
    }

    /**
     * Metoda obsługująca akcję naciśnięcia przycisku logowania.
     *
     * @param event Zdarzenie akcji
     */
    @FXML
    public void handleButtonLogowanieAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/projektpo2/fxml/LogowanieNEW.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 720);
            String css = this.getClass().getResource("/org/projektpo2/fxml/style1.css").toExternalForm();
            scene.getStylesheets().add(css);
            WypozyczalniaOkno.getPrimaryStage().setScene(scene);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading LogowanieNEW.fxml: " + e.getMessage(), e);
        }
    }

    /**
     * Metoda obsługująca akcję naciśnięcia przycisku rejestracji.
     *
     * @param event Zdarzenie akcji
     */
    @FXML
    public void handleButtonRegisterAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/projektpo2/fxml/RegisterScene.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 720);
            String css = this.getClass().getResource("/org/projektpo2/fxml/style1.css").toExternalForm();
            scene.getStylesheets().add(css);
            WypozyczalniaOkno.getPrimaryStage().setScene(scene);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading RegisterScene.fxml: " + e.getMessage(), e);
        }
    }
}

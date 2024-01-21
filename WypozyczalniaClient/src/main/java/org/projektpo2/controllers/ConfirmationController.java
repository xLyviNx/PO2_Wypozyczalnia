package org.projektpo2.controllers;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.projektpo2.Client;
import org.projektpo2.Utilities;
import org.projektpo2.WypozyczalniaOkno;

import java.io.IOException;
import java.net.URL;
import java.util.logging.*;
/**
 * Kontroler potwierdzeń rezerwacji.
 */
public class ConfirmationController {
    private static final Logger logger = Utilities.getLogger(ConfirmationController.class);

    /** Instancja kontrolera. */
    public static ConfirmationController instance;

    /** Scena JavaFX powiązana z kontrolerem. */
    public Scene scene;

    /** Kontener typu VBox w interfejsie użytkownika. */
    @FXML
    private VBox container;

    /**
     * Otwiera scenę potwierdzeń rezerwacji.
     *
     * @return Instancja kontrolera sceny potwierdzeń rezerwacji.
     */
    public static ConfirmationController OpenScene() {
        try {
            URL path = OffersController.class.getResource("/org/projektpo2/fxml/confirmationPanel.fxml");
            if (path == null) {
                logger.log(Level.SEVERE, "FXML file not found.");
                return null;
            }
            FXMLLoader loader = new FXMLLoader(path);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 720);
            URL cssPath = OffersController.class.getResource("/org/projektpo2/fxml/style1.css");
            if (cssPath != null) {
                scene.getStylesheets().add(cssPath.toExternalForm());
            } else {
                logger.log(Level.WARNING, "CSS file not found (Confirmations).");
            }
            Stage primaryStage = WypozyczalniaOkno.getPrimaryStage();
            primaryStage.setScene(scene);
            primaryStage.show();
            instance = loader.getController();
            logger.info("Scene opened: " + instance);
            instance.scene = scene;
            instance.StartScene();
            return instance;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading FXML file: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Runtime error during FXML loading: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unknown error during FXML loading.", e);
        }
        return null;
    }

    /**
     * Inicjuje scenę potwierdzeń rezerwacji.
     */
    public void StartScene() {
        if (Client.instance != null) {
            Client.instance.RequestConfirmations();
        }
    }

    /**
     * Dodaje przycisk potwierdzenia rezerwacji do interfejsu.
     *
     * @param content Treść przycisku.
     * @param id      Identyfikator rezerwacji.
     */
    public void AddButton(String content, int id) {
        HBox entryBox = new HBox();
        entryBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        entryBox.setPrefHeight(65);
        entryBox.setPrefWidth(1278);
        entryBox.setSpacing(15);
        entryBox.getStyleClass().add("offerButtonBar");

        entryBox.setPrefHeight(Region.USE_COMPUTED_SIZE);
        entryBox.setPadding(new javafx.geometry.Insets(5.0, 10.0, 5.0, 10.0));

        VBox labelVBox = new VBox();
        labelVBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label label = new Label(content);
        label.setTextFill(javafx.scene.paint.Color.WHITE);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMaxHeight(Double.MAX_VALUE);
        label.setFont(new Font(26.0));

        labelVBox.getChildren().add(label);
        VBox.setVgrow(labelVBox, javafx.scene.layout.Priority.ALWAYS);

        Button deleteButton = new Button("Usuń");
        deleteButton.setStyle("-fx-font-size: 15.0;");
        deleteButton.getStyleClass().add("mainScreenbutton");
        deleteButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleDelete(id);
            }
        });

        Button confirmButton = new Button("Potwierdź i rozpocznij");
        confirmButton.setStyle("-fx-font-size: 15.0;");
        confirmButton.getStyleClass().add("mainScreenbutton");
        confirmButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleConfirm(id);
            }
        });

        entryBox.getChildren().addAll(labelVBox, deleteButton, confirmButton);
        HBox.setHgrow(labelVBox, Priority.ALWAYS);
        container.getChildren().add(entryBox);
    }

    /**
     * Obsługuje kliknięcie przycisku usuwania rezerwacji.
     *
     * @param id Identyfikator rezerwacji.
     */
    private void handleDelete(int id) {
        logger.info("Delete clicked for ID: " + id);
        if (Client.instance != null) {
            Client.instance.RequestCancelReservation(id);
        }
    }

    /**
     * Obsługuje kliknięcie przycisku potwierdzenia rezerwacji.
     *
     * @param id Identyfikator rezerwacji.
     */
    private void handleConfirm(int id) {
        logger.info("Confirm clicked for ID: " + id);
        if (Client.instance != null) {
            Client.instance.RequestConfirmReservation(id);
        }
    }

    /**
     * Odświeża widok potwierdzeń rezerwacji.
     */
    public void Refresh() {
        container.getChildren().clear();
        StartScene();
    }

    /**
     * Powraca do poprzedniej sceny.
     */
    public void GoBack() {
        OffersController.openScene();
        logger.info("Go back button pressed");
    }
}

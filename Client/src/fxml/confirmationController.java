package fxml;

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
import src.Client;
import src.WypozyczalniaOkno;

import java.io.IOException;
import java.net.URL;

public class ConfirmationController
{
    public static ConfirmationController instance;
    public Scene scene;
    @FXML
    private VBox container;  // Assuming you have an HBox with fx:id="container" in your FXML
    public static ConfirmationController OpenScene()
    {
        try {
            URL path = OffersController.class.getResource("/fxml/confirmationPanel.fxml");
            if (path == null) {
                System.err.println("FXML file not found.");
                return null;
            }
            FXMLLoader loader = new FXMLLoader(path);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 720);
            URL cssPath = OffersController.class.getResource("/fxml/style1.css");
            System.out.println("CSS Path: " + cssPath);
            if (cssPath != null) {
                scene.getStylesheets().add(cssPath.toExternalForm());
            } else {
                System.err.println("CSS file not found.");
            }
            Stage primaryStage = WypozyczalniaOkno.getPrimaryStage();
            primaryStage.setScene(scene);
            primaryStage.show();
            instance = loader.getController();
            System.out.println(instance);
            instance.scene = scene;
            instance.StartScene();
            return instance;
        } catch (IOException e) {
            System.err.println("Error loading FXML file: " + e.getMessage());
            e.printStackTrace();
        } catch (RuntimeException e) {
            System.err.println("Runtime error during FXML loading: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unknown error during FXML loading.");
            e.printStackTrace();
        }
        return null;
    }

    public void StartScene()
    {
        if (Client.instance != null)
        {
            Client.instance.RequestConfirmations();
        }
    }
    public void AddButton(String content, int id) {
        HBox entryBox = new HBox();
        entryBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        entryBox.setPrefHeight(65);
        entryBox.setPrefWidth(1278);
        entryBox.setSpacing(15);
        entryBox.getStyleClass().add("offerButtonBar");

        entryBox.setPrefHeight(Region.USE_COMPUTED_SIZE);
        // Ustawienie paddingu dla HBox
        entryBox.setPadding(new javafx.geometry.Insets(5.0, 10.0, 5.0, 10.0));

        // Creating VBox for Label
        VBox labelVBox = new VBox();
        labelVBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Creating Label
        Label label = new Label(content);
        label.setTextFill(javafx.scene.paint.Color.WHITE);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMaxHeight(Double.MAX_VALUE);
        label.setFont(new Font(26.0));

        labelVBox.getChildren().add(label);
        VBox.setVgrow(labelVBox, javafx.scene.layout.Priority.ALWAYS); // Dodaj vgrow dla VBox

        // Creating Buttons
        Button deleteButton = new Button("Usuń");
        deleteButton.setStyle("-fx-font-size: 15.0;");
        deleteButton.getStyleClass().add("mainScreenbutton");
        // Dodanie obsługi zdarzenia dla przycisku "Usuń"
        deleteButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleDelete(id);
            }
        });

        Button confirmButton = new Button("Potwierdź i rozpocznij");
        confirmButton.setStyle("-fx-font-size: 15.0;");
        confirmButton.getStyleClass().add("mainScreenbutton");
        // Dodanie obsługi zdarzenia dla przycisku "Potwierdź i rozpocznij"
        confirmButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleConfirm(id);
            }
        });

        // Adding components to the entryBox
        entryBox.getChildren().addAll(labelVBox, deleteButton, confirmButton);

        // Ustawienie HBox.hgrow dla VBox
        HBox.setHgrow(labelVBox, Priority.ALWAYS);
        container.getChildren().add(entryBox);
    }

    // Example handler methods
    private void handleDelete(int id) {
        System.out.println("Delete clicked for ID: " + id);
        if (Client.instance!=null)
        {
            Client.instance.RequestCancelReservation(id);
        }
    }

    private void handleConfirm(int id) {
        System.out.println("Confirm clicked for ID: " + id);
        if (Client.instance!=null)
        {
            Client.instance.RequestConfirmReservation(id);
        }
    }
    public void Refresh(){
        container.getChildren().clear();
        StartScene();
    }
    public void GoBack()
    {
        OffersController.openScene();
    }
}

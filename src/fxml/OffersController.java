package fxml;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import src.Client;
import javafx.scene.control.Label;
import src.WypozyczalniaOkno;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.Parent;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

public class OffersController {
    @FXML
    private FlowPane flow;
    public Scene scene;
    public static OffersController instance;
    @FXML
    public Label label_user;
    @FXML
    public Button addOfferButton;
    @FXML
    public Button confirmationsButton;
    public void StartScene()
    {
        if (Client.instance != null)
        {
            Client.instance.RequestUsername();
            Client.instance.RequestOffers();
            Client.instance.RequestConfButton();
        }
    }
    public static OffersController openScene() {
        try {
            // Load the FXML file
            URL path = OffersController.class.getResource("/fxml/cars.fxml");
            System.out.println("FXML Path: " + path);

            if (path == null) {
                System.err.println("FXML file not found.");
                return null;
            }

            FXMLLoader loader = new FXMLLoader(path);
            Parent root = loader.load();

            // Create the scene
            Scene scene = new Scene(root, 1280, 720);

            // Apply the CSS style
            URL cssPath = OffersController.class.getResource("/fxml/style1.css");
            System.out.println("CSS Path: " + cssPath);

            if (cssPath != null) {
                scene.getStylesheets().add(cssPath.toExternalForm());
            } else {
                System.err.println("CSS file not found.");
            }

            // Set the scene to the primary stage
            Stage primaryStage = WypozyczalniaOkno.getPrimaryStage();
            primaryStage.setScene(scene);

            // Show the stage
            primaryStage.show();
            // Return the controller instance if needed
            instance = loader.getController();
            System.out.println(instance);
            instance.scene=scene;
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
    public static void AddOfferNode(String vehicleName, float price, byte[] imageBytes, int dbid, boolean isRent, int daysLeft) {
            if (instance != null && WypozyczalniaOkno.instance != null && instance.scene == WypozyczalniaOkno.getPrimaryStage().getScene()) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        instance.flow.getChildren().add(instance.createOfferNode(vehicleName, price, imageBytes, dbid, isRent, daysLeft));
                    }
                });
            }
    }
    public static void setUsername(String username) {
        if (instance != null && instance.label_user != null) {
            Platform.runLater(() -> {
                instance.label_user.setText(username);
            });
        } else {
            System.out.println("instance or usernameText is null: " + instance + ", " + instance.label_user);
        }
    }

    private Node createOfferNode(String vehicleName, float price, byte[] imageBytes, int dbid, boolean isRent, int daysLeft) {
        VBox offerNode = new VBox();
        offerNode.getStyleClass().addAll( isRent? "offerButtonRent" : (daysLeft == -1? "offerButtonAwaiting" : "offerButton"));
        offerNode.setAlignment(Pos.CENTER); // Ustawienie środka dla całego VBox

        HBox imageBox = new HBox();
        ImageView imageView = new ImageView();
        if (imageBytes.length > 0) {
            try {
                Image image = new Image(new ByteArrayInputStream(imageBytes));
                imageView.setImage(image);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        imageView.setFitHeight(136.0);
        imageView.setFitWidth(279.0);
        imageBox.getChildren().add(imageView);

        VBox textInfo = new VBox();
        textInfo.getStyleClass().addAll("offerButtonBar");
        textInfo.setAlignment(Pos.CENTER); // Ustawienie środka dla VBox z tekstem

        Label vehicleLabel = new Label(vehicleName);
        vehicleLabel.getStyleClass().addAll("offerLabel");
        vehicleLabel.setFont(Font.font("Calibri Light", 20.0)); // Ustawienie rozmiaru tekstu

        Label priceLabel =null;
        if (isRent)
        {
            priceLabel = new Label("Pozostało " + daysLeft + " dni.");
        }
        else if (daysLeft==-1)
        {
            priceLabel = new Label("Oczekuje na rozpatrzenie");
        }
        else{
            priceLabel = new Label(String.format("%.2f zł/dzień", price));
        }
        priceLabel.getStyleClass().addAll("offerLabel");
        priceLabel.setFont(Font.font("Calibri Light", 16.0)); // Ustawienie rozmiaru tekstu

        textInfo.getChildren().addAll(vehicleLabel, priceLabel);

        offerNode.getChildren().addAll(imageBox, textInfo);
        if(!isRent && daysLeft!=-1) {
            offerNode.setOnMouseClicked(event -> {
                ClickOnItem(dbid);
            });
        }
        return offerNode;
    }
    @FXML
    public void LogoutButton()
    {
        if (Client.instance != null)
        {
            Client.instance.SendLogout();
        }
    }
    public void ClickOnItem(int id)
    {
        System.out.println("KLIKNIETO " + id);
        OfferDetailsController.openScene(id);
    }
    @FXML
    public void AddOfferButton()
    {
        addOfferController.openScene();
    }
    @FXML
    public void Refresh()
    {
        flow.getChildren().clear();
        StartScene();
    }
    @FXML
    public void GoToConfirmations()
    {
        confirmationController.OpenScene();
    }
}


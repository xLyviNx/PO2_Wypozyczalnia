package org.projektpo2.controllers;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.projektpo2.Client;
import org.projektpo2.WypozyczalniaOkno;

import java.io.IOException;
import java.net.URL;

public class ReservationController {
    public static ReservationController instance;
    public Scene scene;
    public int carid;
    @FXML
    private Text headertext;
    @FXML
    private Text dniText;
    @FXML
    private Slider slider_dni;
    @FXML
    public Button but_reserve;
    float price;

    public static ReservationController openScene(String header, int id, float price) {
        try {
            URL path = OffersController.class.getResource("/org/projektpo2/fxml/rezerwacja.fxml");
            if (path == null) {
                System.err.println("FXML file not found.");
                return null;
            }
            FXMLLoader loader = new FXMLLoader(path);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 720);
            URL cssPath = OffersController.class.getResource("/org/projektpo2/fxml/style1.css");
            if (cssPath != null) {
                scene.getStylesheets().add(cssPath.toExternalForm());
            } else {
                System.err.println("CSS file not found (Reservation Controller).");
            }
            Stage primaryStage = WypozyczalniaOkno.getPrimaryStage();
            primaryStage.setScene(scene);
            primaryStage.show();
            instance = loader.getController();
            instance.scene = scene;
            instance.StartScene(header, id, price);
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

    public void StartScene(String header, int id, float price)
    {
        carid = id;
        this.price=price;
        dniText.setText("Wybierz na ile dni chcesz wypozyczyc pojazd.");
        headertext.setText(header);
        slider_dni.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                dniText.setText("Pojazd zostanie wypozyczony na " + newValue.intValue() + " dni.\nSpodziewany koszt wynajmu: " + String.format("%.2f zł", price * newValue.intValue()));
            }
        });
    }
    @FXML
    public void Cancel()
    {
        OfferDetailsController.openScene(carid);
    }
    @FXML
    public void Confirm()
    {
        int days = (int)slider_dni.getValue();
        if (days<3)
        {
            Client.MessageBox("Okres wypozyczenia pojazdu to minimum 3 dni!", Alert.AlertType.ERROR);
            return;
        }
        but_reserve.setVisible(false);
        if(Client.instance!=null)
        {
            Client.instance.RequestReservation(carid, days);
        }
    }
}

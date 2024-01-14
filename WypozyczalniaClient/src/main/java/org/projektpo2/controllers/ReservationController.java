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
import org.projektpo2.Utilities;
import org.projektpo2.WypozyczalniaOkno;

import java.io.IOException;
import java.net.URL;
import java.util.logging.*;

/**
 * Kontroler obsługujący proces rezerwacji pojazdu.
 */
public class ReservationController {
    private static final Logger logger = Utilities.getLogger(ReservationController.class);

    /**
     * Statyczna instancja kontrolera rezerwacji.
     */
    public static ReservationController instance;

    /**
     * Scena rezerwacji.
     */
    public Scene scene;

    /**
     * Identyfikator pojazdu, który jest rezerwowany.
     */
    public int carid;

    /**
     * Tekst nagłówka rezerwacji.
     */
    @FXML
    private Text headertext;

    /**
     * Tekst informujący o wybranych dniach wypożyczenia.
     */
    @FXML
    private Text dniText;

    /**
     * Suwak do wyboru liczby dni wypożyczenia.
     */
    @FXML
    private Slider slider_dni;

    /**
     * Przycisk potwierdzający rezerwację.
     */
    @FXML
    public Button but_reserve;

    /**
     * Cena za dzień wypożyczenia pojazdu.
     */
    float price;

    /**
     * Metoda otwierająca scenę rezerwacji i ustawiająca jej początkowy stan.
     *
     * @param header Nagłówek rezerwacji
     * @param id     Identyfikator pojazdu
     * @param price  Cena za dzień wypożyczenia
     * @return Instancja kontrolera rezerwacji
     */
    public static ReservationController openScene(String header, int id, float price) {
        try {
            URL path = OffersController.class.getResource("/org/projektpo2/fxml/rezerwacja.fxml");
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
                logger.log(Level.WARNING, "CSS file not found (Reservation Controller).");
            }
            Stage primaryStage = WypozyczalniaOkno.getPrimaryStage();
            primaryStage.setScene(scene);
            primaryStage.show();
            instance = loader.getController();
            instance.scene = scene;
            instance.StartScene(header, id, price);
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
     * Inicjalizuje stan sceny rezerwacji.
     *
     * @param header Nagłówek rezerwacji
     * @param id     Identyfikator pojazdu
     * @param price  Cena za dzień wypożyczenia
     */
    public void StartScene(String header, int id, float price) {
        carid = id;
        this.price = price;
        dniText.setText("Wybierz na ile dni chcesz wypożyczyć pojazd.");
        headertext.setText(header);
        slider_dni.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                dniText.setText("Pojazd zostanie wypożyczony na " + newValue.intValue() + " dni.\nSpodziewany koszt wynajmu: " + String.format("%.2f zł", price * newValue.intValue()));
            }
        });
    }

    /**
     * Metoda obsługująca anulowanie procesu rezerwacji.
     */
    @FXML
    public void Cancel() {
        OfferDetailsController.openScene(carid);
    }

    /**
     * Metoda obsługująca potwierdzenie rezerwacji.
     */
    @FXML
    public void Confirm() {
        int days = (int) slider_dni.getValue();
        if (days < 3) {
            Client.MessageBox("Okres wypożyczenia pojazdu to minimum 3 dni!", Alert.AlertType.ERROR);
            return;
        }
        but_reserve.setVisible(false);
        if (Client.instance != null) {
            Client.instance.RequestReservation(carid, days);
        }
    }
}

package fxml;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

import src.WypozyczalniaOkno;
public class StronaStartowaController {
    @FXML
    private Button buttonLogowanie;

    @FXML
    private Button buttonRejestracja;

    private WypozyczalniaOkno wypozyczalniaOkno;

    // Metoda ustawiająca referencję do głównego okna
    public void setWypozyczalniaOkno(WypozyczalniaOkno wypozyczalniaOkno)
    {
        this.wypozyczalniaOkno = wypozyczalniaOkno;
    }

    @FXML
    private void handleButtonLogowanieAction(ActionEvent event) throws IOException
    {
        System.out.println("Button logowanie click!");
        // Ładowanie nowego widoku (FXML) z pliku Logowanie.fxml
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Logowanie.fxml"));
        Parent root = loader.load();

        // Tworzenie nowej sceny
        Scene scene = new Scene(root);

        // Uzyskanie dostępu do głównego okna (Stage) i ustawienie nowej sceny
        WypozyczalniaOkno.getPrimaryStage().setScene(scene);
    }
}

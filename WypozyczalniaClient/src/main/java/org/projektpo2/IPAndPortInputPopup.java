package org.projektpo2;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Klasa reprezentująca okno dialogowe do wprowadzania adresu IP i numeru portu.
 */
public class IPAndPortInputPopup extends Application {

    /** Adres IP. */
    public String ip;

    /** Numer portu. */
    public String port;

    /**
     * Metoda uruchamiająca aplikację.
     *
     * @param args Argumenty wiersza poleceń.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Metoda startująca interfejs użytkownika.
     *
     * @param primaryStage Główny etap interfejsu użytkownika.
     */
    @Override
    public void start(Stage primaryStage) {
        Button openInputDialogButton = new Button("Open Input Dialog");
        openInputDialogButton.setOnAction(event -> showInputDialog());

        VBox vbox = new VBox(10);
        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(openInputDialogButton);

        Scene scene = new Scene(vbox, 250, 150);
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    /**
     * Metoda wyświetlająca okno dialogowe z polami do wprowadzenia adresu IP i numeru portu.
     */
    public void showInputDialog() {
        Stage inputStage = new Stage();
        inputStage.initModality(Modality.APPLICATION_MODAL);
        inputStage.setTitle("Enter IP and Port");

        Label ipLabel = new Label("IP:");
        Label portLabel = new Label("Port:");
        TextField ipTextField = new TextField();
        TextField portTextField = new TextField();

        Button submitButton = new Button("Połącz");
        submitButton.setOnAction(event -> {
            ip = ipTextField.getText();
            port = portTextField.getText();

            if (validatePort(port)) {
                inputStage.close();
            } else {
                showAlert("Invalid Port", "Please enter a valid port number.");
            }
        });

        VBox vbox = new VBox(10);
        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(ipLabel, ipTextField, portLabel, portTextField, submitButton);

        Scene scene = new Scene(vbox, 250, 150);
        inputStage.setScene(scene);

        inputStage.showAndWait();
    }

    /**
     * Metoda sprawdzająca, czy podany numer portu jest prawidłowy.
     *
     * @param port Numer portu do sprawdzenia.
     * @return true, jeśli numer portu jest prawidłowy, w przeciwnym razie false.
     */
    private boolean validatePort(String port) {
        try {
            int portNumber = Integer.parseInt(port);
            return portNumber > 0 && portNumber <= 65535; // Port musi być liczbą z zakresu 1-65535
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Metoda wyświetlająca komunikat o błędzie.
     *
     * @param title   Tytuł komunikatu.
     * @param content Treść komunikatu.
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

package fxml;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

import fxml.Cars_list_controller;

public class LogowanieController {
    @FXML
    private Button buttonLogIn;

    @FXML
    private Button buttonRejestracja;

    @FXML
    private TextField userLogin;

    @FXML
    private PasswordField userPassword;

    @FXML
    private void GoToRegister(ActionEvent event) throws IOException
    {
        StronaStartowaController obj = new StronaStartowaController();
        obj.handleButtonRegisterAction(event);
    }
    @FXML
    private void handleLogInButton(ActionEvent event) throws IOException
    {
        String login = userLogin.getText();
        String receivedPassword = userPassword.getText();


        String realPassword = "admin";


        //Dodac weryfikacji loginu i hasła z bazy danych

        // Sprawdź, czy login i hasło to "admin"
        if (login.equals("admin") && receivedPassword.equals("admin"))
        {
            showAlert("Zalogowano pomyślnie!");
            Cars_list_controller cars_list_controller = new Cars_list_controller();
            cars_list_controller.load_scene();

        }
        else
        {
            showAlert("Błędny login lub hasło!");
        }
        //logowanie dla zwyklego uzytkownika
        if (realPassword.equals(receivedPassword))
        {
            showAlert("zalogowano");
        }
        else showAlert("bledne haslo");
    }
    private void showAlert(String message)
    {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Komunikat");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

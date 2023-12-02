package fxml;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import fxml.StronaStartowaController;
import javafx.scene.control.Alert;

import java.io.IOException;

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
        String password = userPassword.getText();

        //Dodac weryfikacji loginu i hasła z bazy danych

        // Sprawdź, czy login i hasło to "admin"
        if (login.equals("admin") && password.equals("admin"))
        {
            showAlert("Zalogowano pomyślnie!");
        }
        else
        {
            showAlert("Błędny login lub hasło!");
        }
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

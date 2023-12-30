package fxml;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import src.Client;
import src.MD5Encryptor;

import java.io.IOException;

public class LoginController {
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
        StartPageController obj = new StartPageController();
        obj.handleButtonRegisterAction(event);
    }
    @FXML
    private void handleLogInButton(ActionEvent event) throws IOException
    {
        String login = userLogin.getText();
        String receivedPassword = userPassword.getText();
        if (login.isEmpty() || receivedPassword.isEmpty())
        {
            Client.MessageBox("Nie podano wszystkich danych!", Alert.AlertType.ERROR);
            return;
        }

        String password = MD5Encryptor.encryptPassword(receivedPassword);
        if (Client.instance != null)
        {
            Client.instance.RequestLogin(login, password);
        }
    }
}

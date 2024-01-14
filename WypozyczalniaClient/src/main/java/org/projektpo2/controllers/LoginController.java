package org.projektpo2.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.projektpo2.Client;
import org.projektpo2.Utilities;

import java.io.IOException;
import java.util.logging.*;

/**
 * Kontroler obsługujący proces logowania użytkownika.
 */
public class LoginController {

    /**
     * Logger służący do logowania zdarzeń w klasie LoginController.
     */
    private static final Logger logger = Utilities.getLogger(LoginController.class);

    /**
     * Pole tekstowe do wprowadzania loginu użytkownika.
     */
    @FXML
    private TextField userLogin;

    /**
     * Pole tekstowe do wprowadzania hasła użytkownika.
     */
    @FXML
    private PasswordField userPassword;
    /**
     * Obsługuje przycisk przechodzenia do strony rejestracji.
     *
     * @param event Zdarzenie przycisku.
     * @throws IOException Wyjątek wejścia/wyjścia.
     */
    @FXML
    private void GoToRegister(ActionEvent event) throws IOException {
        StartPageController obj = new StartPageController();
        obj.handleButtonRegisterAction(event);
        logger.info("Navigated to registration page.");
    }

    /**
     * Obsługuje przycisk logowania.
     *
     * Pobiera wprowadzone dane użytkownika, sprawdza ich poprawność i wysyła żądanie logowania do serwera.
     *
     * @param event Zdarzenie przycisku.
     */
    @FXML
    private void handleLogInButton(ActionEvent event) {
        String login = userLogin.getText().trim();
        String receivedPassword = userPassword.getText().trim();

        // Sprawdza, czy podano wszystkie dane logowania
        if (login.isEmpty() || receivedPassword.isEmpty()) {
            Client.MessageBox("Nie podano wszystkich danych!", Alert.AlertType.ERROR);
            return;
        }

        // Wysyła żądanie logowania do serwera
        if (Client.instance != null) {
            Client.instance.RequestLogin(login, receivedPassword);
            logger.info("Login request sent for user: " + login);
        }
    }
}

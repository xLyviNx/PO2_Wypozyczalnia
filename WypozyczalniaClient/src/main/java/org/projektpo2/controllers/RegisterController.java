package org.projektpo2.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import org.projektpo2.Client;
import org.projektpo2.Utilities;

import java.io.IOException;
import java.util.logging.*;

/**
 * Kontroler obsługujący rejestrację nowego użytkownika.
 */
public class RegisterController {

    /**
     * Logger.
     */
    private static final Logger logger = Utilities.getLogger(RegisterController.class);

    /**
     * Pole tekstowe dla numeru telefonu.
     */
    @FXML
    private TextField numer_field;

    /**
     * Pole tekstowe dla imienia użytkownika.
     */
    @FXML
    private TextField imie_field;

    /**
     * Pole tekstowe dla nazwiska użytkownika.
     */
    @FXML
    private TextField nazwisko_field;

    /**
     * Pole tekstowe dla loginu użytkownika.
     */
    @FXML
    private TextField login_field;

    /**
     * Pole hasła dla użytkownika.
     */
    @FXML
    private PasswordField pass_field;

    /**
     * Pole powtórzonego hasła dla użytkownika.
     */
    @FXML
    private PasswordField rep_pass_field;

    /**
     * Inicjalizuje kontroler, dodając filtry dla pól tekstowych, aby ograniczyć długość wprowadzanych danych.
     */
    @FXML
    public void initialize() {
        numer_field.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            String input = event.getCharacter();
            if (!input.matches("\\d*") || numer_field.getText().length() >= 9) {
                event.consume();
            }
        });
        login_field.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (login_field.getText().length() >= 32) {
                event.consume();
            }
        });
        pass_field.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (pass_field.getText().length() >= 20) {
                event.consume();
            }
        });
        rep_pass_field.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (rep_pass_field.getText().length() >= 20) {
                event.consume();
            }
        });
        imie_field.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (imie_field.getText().length() >= 15) {
                event.consume();
            }
        });
        nazwisko_field.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (nazwisko_field.getText().length() >= 32) {
                event.consume();
            }
        });
    }

    /**
     * Obsługuje powrót do strony logowania po naciśnięciu przycisku.
     *
     * @param event Zdarzenie przycisku
     */
    @FXML
    private void BackToLogin(ActionEvent event) {
        StartPageController obj = new StartPageController();
        obj.handleButtonLogowanieAction(event);
    }

    /**
     * Obsługuje przycisk rejestracji.
     * Sprawdza wprowadzone dane i wysyła żądanie rejestracji do klienta.
     * @param event event
     */
    @FXML
    private void RegisterButton(ActionEvent event) {
        try {
            String numerText = numer_field.getText().trim();
            if (numerText.length() != 9) {
                Client.MessageBox("Numer telefonu musi mieć 9 znaków.", Alert.AlertType.ERROR);
                return;
            }
            int numer = Integer.parseInt(numerText);

            String login = login_field.getText().trim();
            String pwd = pass_field.getText().trim();
            String pwdR = rep_pass_field.getText().trim();
            String imie = imie_field.getText().trim();
            String nazwisko = nazwisko_field.getText().trim();

            if (login.isEmpty() || pwd.isEmpty() || pwdR.isEmpty() || imie.isEmpty() || nazwisko.isEmpty()) {
                Client.MessageBox("Wszystkie pola muszą być wypełnione.", Alert.AlertType.ERROR);
                logger.log(Level.WARNING, "KTÓREŚ Z PÓL JEST PUSTE");
                return;
            }
            if (login.length() < 5) {
                Client.MessageBox("Login musi mieć minimum 5 znaków.", Alert.AlertType.ERROR);
                return;
            }
            if (pwd.length() < 5) {
                Client.MessageBox("Hasło musi mieć minimum 5 znaków.", Alert.AlertType.ERROR);
                return;
            }
            if (!pwdR.equals(pwd)) {
                Client.MessageBox("Hasła nie są takie same!", Alert.AlertType.ERROR);
                return;
            }
            if (imie.length() < 3) {
                Client.MessageBox("Imię musi mieć minimum 3 znaki.", Alert.AlertType.ERROR);
                return;
            }
            if (nazwisko.length() < 3) {
                Client.MessageBox("Nazwisko musi mieć minimum 3 znaki.", Alert.AlertType.ERROR);
                return;
            }
            if (Client.instance != null) {
                Client.instance.RequestRegister(login, pwd, pwdR, numer, imie, nazwisko);
            }
        } catch (NumberFormatException ex) {
            Client.MessageBox("Wprowadzono nieprawidłową wartość liczbową.", Alert.AlertType.ERROR);
            logger.log(Level.WARNING, "NumberFormatException: " + ex.getMessage(), ex);
        }
    }
}

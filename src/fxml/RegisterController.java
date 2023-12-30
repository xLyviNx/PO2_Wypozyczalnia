package fxml;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import src.Client;


import java.io.IOException;

public class RegisterController {

    @FXML
    private Button backbutton;

    @FXML
    private TextField numer_field;
    @FXML
    private TextField imie_field;
    @FXML
    private TextField nazwisko_field;
    @FXML
    private TextField login_field;

    @FXML
    private PasswordField pass_field;

    @FXML
    private Button register_button;

    @FXML
    private PasswordField rep_pass_field;

    @FXML
    public void initialize()
    {

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

    @FXML
    private void BackToLogin(ActionEvent event) throws IOException
    {
        StartPageController obj= new StartPageController();
        obj.handleButtonLogowanieAction(event);
    }
    @FXML
    private void RegisterButton(ActionEvent event)
    {
        System.out.println("REGISTER BUTTON");
        String numerText = numer_field.getText().trim();
        if (numerText.length() != 9)
        {
            Client.MessageBox("Numer telefonu musi mieć 9 znaków.", Alert.AlertType.ERROR);
            return;
        }
        int numer = 0;
        try {
            numer = Integer.parseInt(numerText);
        } catch (NumberFormatException e) {
            Client.MessageBox("Nieprawidłowy format numeru telefonu.", Alert.AlertType.ERROR);
            System.err.println("Błąd: Nieprawidłowy format numeru telefonu");
            return;
        }
        String login = login_field.getText().trim();
        String pwd = pass_field.getText().trim();
        String pwdR = rep_pass_field.getText().trim();
        String imie = imie_field.getText().trim();
        String nazwisko = nazwisko_field.getText().trim();
        if (login.isEmpty() || pwd.isEmpty() || pwdR.isEmpty() || imie.isEmpty()|| nazwisko.isEmpty())
        {
            Client.MessageBox("Wszystkie pola muszą być wypełnione.", Alert.AlertType.ERROR);
            System.err.println("KTÓREŚ Z PÓL JEST PUSTE");
            return;
        }
        if (login.length()<5)
        {
            Client.MessageBox("Login musi mieć minimum 5 znaków.", Alert.AlertType.ERROR);
            return;
        }
        if (pwd.length()<5)
        {
            Client.MessageBox("Hasło musi mieć minimum 5 znaków.", Alert.AlertType.ERROR);
            return;
        }
        if (!pwdR.equals(pwd))
        {
            Client.MessageBox("Hasła nie są takie same!", Alert.AlertType.ERROR);
            return;
        }
        if (imie.length()< 3)
        {
            Client.MessageBox("Imie musi mieć minimum 3 znaki.", Alert.AlertType.ERROR);
            return;
        }
        if (nazwisko.length()< 3)
        {
            Client.MessageBox("Nazwisko musi mieć minimum 3 znaki.", Alert.AlertType.ERROR);
            return;
        }
        if (Client.instance != null)
        {
            Client.instance.RequestRegister(login, pwd, pwdR, numer,imie, nazwisko);
        }
    }
}

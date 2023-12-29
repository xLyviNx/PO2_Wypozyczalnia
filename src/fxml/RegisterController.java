package fxml;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
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
    private TextField pass_field;

    @FXML
    private Button register_button;

    @FXML
    private TextField rep_pass_field;
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
        String numerText = numer_field.getText();
        int numer = 0;
        try {
            numer = Integer.parseInt(numerText);
        } catch (NumberFormatException e) {
            System.err.println("Błąd: Nieprawidłowy format numeru telefonu");
        }
        String login = login_field.getText();
        String pwd = pass_field.getText();
        String pwdR = rep_pass_field.getText();
        String imie = imie_field.getText();
        String nazwisko = nazwisko_field.getText();
        if (login.isEmpty() || pwd.isEmpty() || pwdR.isEmpty() || imie.isEmpty()|| nazwisko.isEmpty() || numerText.length() != 9)
        {
            System.err.println("KTÓREŚ Z PÓL JEST PUSTE");
            return;
        }
        if (Client.instance != null)
        {
            Client.instance.RequestRegister(login, pwd, pwdR, numer,imie, nazwisko);
        }
    }
}

package fxml;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import fxml.StronaStartowaController;
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
}

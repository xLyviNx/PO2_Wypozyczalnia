package fxml;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;



import java.io.IOException;

public class controller_register {

    @FXML
    private Button backbutton;

    @FXML
    private TextField emailfield;

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
        StronaStartowaController obj= new StronaStartowaController();
        obj.handleButtonLogowanieAction(event);
    }

}

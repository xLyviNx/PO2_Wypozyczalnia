package fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import src.WypozyczalniaOkno;

import java.io.IOException;

public class NoConnectionController {
    @FXML
    public Button restartbutton;

    @FXML
    public void initialize() {
        /*
        restartbutton.setOnAction(event -> {
            // Code to execute when the button is clicked
            System.out.println("Restart button clicked!");
        });*/
    }

    @FXML
    public void load_scene() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NoConnection.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1280, 720);
        WypozyczalniaOkno.getPrimaryStage().setScene(scene);
    }
}

package fxml;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import src.Client;
import src.Main;
import src.WypozyczalniaOkno;

import java.io.IOException;

public class NoConnectionController {
    @FXML
    public void RestartButton() {
        System.out.println("RESTART");
        Main.RunClient();
        if (WypozyczalniaOkno.instance == null){
            WypozyczalniaOkno.getPrimaryStage().close();
        }
        WypozyczalniaOkno.instance.MainScene();
    }

    @FXML
    public void load_scene() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NoConnection.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1280, 720);
        String css = this.getClass().getResource("/fxml/style1.css").toExternalForm();
        scene.getStylesheets().add(css);
        WypozyczalniaOkno.getPrimaryStage().setScene(scene);
    }
}

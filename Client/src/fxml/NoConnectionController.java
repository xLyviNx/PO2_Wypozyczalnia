package fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import src.ClientMain;
import src.WypozyczalniaOkno;

import java.io.IOException;

public class NoConnectionController {
    @FXML
    public void RestartButton() {
        System.out.println("RESTART");
        ClientMain.runClient();
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

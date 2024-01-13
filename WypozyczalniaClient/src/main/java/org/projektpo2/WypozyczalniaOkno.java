package org.projektpo2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.projektpo2.controllers.NoConnectionController;
import org.projektpo2.controllers.StartPageController;

public class WypozyczalniaOkno extends Application {
    public static WypozyczalniaOkno instance;
    private static Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        String ip = null;
        int port=0;
        if (getParameters().getRaw().size()==2) {
             ip = getParameters().getRaw().get(0);
             port = Integer.parseInt(getParameters().getRaw().get(1));
        }
        else
        {
            IPAndPortInputPopup input = new IPAndPortInputPopup();
            input.showInputDialog();
            try {
                ip=input.ip;
                port = Integer.parseInt(input.port);
            }catch (Exception ex)
            {
                Platform.exit();
            }
        }
        WypozyczalniaOkno.primaryStage = primaryStage;
        instance=this;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/projektpo2/fxml/StartPage.fxml"));
            Parent root = loader.load();

            StartPageController controller = loader.getController();
            controller.setWypozyczalniaOkno(this);

            Scene scene = new Scene(root, 1280, 720);
            String css = this.getClass().getResource("/org/projektpo2/fxml/style1.css").toExternalForm();
            scene.getStylesheets().add(css);
            primaryStage.setScene(scene);
            primaryStage.show();

            primaryStage.setOnCloseRequest(t -> {
                Platform.exit();
                System.exit(0);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        startClient(ip, port);
    }

    public void MainScene() {
        Platform.runLater(() -> {
            try {
                StartPageController spage = new StartPageController();
                spage.load_scene();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public void NoConnection() {
        Platform.runLater(() -> {
            try {
                NoConnectionController ncon = new NoConnectionController();
                ncon.load_scene();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void startClient(String ip, int port) {
        ClientMain.setupClient(ip,port);
        ClientMain.runClient();
    }
}

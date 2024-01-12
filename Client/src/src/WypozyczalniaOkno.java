package src;

import fxml.NoConnectionController;
import fxml.StartPageController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class WypozyczalniaOkno extends Application {
    public static WypozyczalniaOkno instance;
    private static Stage primaryStage;

    @Override
    public void init() throws Exception {
        super.init();
        Platform.startup(() -> {
            Stage stage = new Stage();
            try {
                start(stage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    @Override
    public void start(Stage primaryStage) {
        WypozyczalniaOkno.primaryStage = primaryStage;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/StartPage.fxml"));
            Parent root = loader.load();

            StartPageController controller = loader.getController();
            controller.setWypozyczalniaOkno(this);

            Scene scene = new Scene(root, 1280, 720);
            String css = this.getClass().getResource("/fxml/style1.css").toExternalForm();
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
}

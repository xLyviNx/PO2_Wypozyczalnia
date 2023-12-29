package src;

import fxml.NoConnectionController;
import fxml.StartPageController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class WypozyczalniaOkno extends Application {

    public static WypozyczalniaOkno instance;
    private static Stage primaryStage; //Pole do przechowywania referencji do głównego okna

    // Metoda do uzyskiwania dostępu do głównego okna
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        WypozyczalniaOkno.primaryStage = primaryStage;
        primaryStage.setTitle("Wypożyczalnia");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/StartPage.fxml"));
        Parent root = loader.load();

        // Pobieranie kontrolera z załadowanego pliku FXML
        StartPageController controller = loader.getController();

        // Ustawianie funkcji obsługi przycisku w kontrolerze
        controller.setWypozyczalniaOkno(this);

        Scene scene = new Scene(root, 1280, 720);
        String css = this.getClass().getResource("/fxml/style1.css").toExternalForm();
        scene.getStylesheets().add(css);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                Platform.exit();
                System.exit(0);
            }
        });
        instance = this;
    }

    public void MainScene()
    {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    StartPageController spage = new StartPageController();
                    spage.load_scene();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
    public void NoConnection() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    NoConnectionController ncon = new NoConnectionController();
                    ncon.load_scene();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}

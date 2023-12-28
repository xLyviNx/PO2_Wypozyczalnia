package src;

import fxml.StartPageController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class WypozyczalniaOkno extends Application {

    private static Stage primaryStage; //Pole do przechowywania referencji do głównego okna

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



        /*while (true)
            if (Client.instance == null || !Client.instance.socket.isConnected())
                primaryStage.close();

        PRZEZ TO JEST BRAK ODPOWIEDZI NWM JAK ZROBIC BO TO MIALO OKNO ZAMYKAC JAK NIE MA POLACZENIA
                */
    }

    // Metoda do uzyskiwania dostępu do głównego okna
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

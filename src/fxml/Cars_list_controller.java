package fxml;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import src.WypozyczalniaOkno;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;




import java.io.IOException;
import java.awt.*;

public class Cars_list_controller  {

    @FXML
    private ListView<ImageItem> list;
    @FXML
    public void initialize() {
    list.getItems().add(new ImageItem("tank",new Image("./src/Tank jednostka.png")));
        System.out.println("lista");

        list.setCellFactory(param -> new ListCell<ImageItem>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(100);
                imageView.setFitHeight(100);
            }

            @Override
            protected void updateItem(ImageItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    imageView.setImage(item.getImage());
                    setGraphic(imageView);
                    setText(item.getDescription());
                }
            }
        });

    }
    @FXML
    public void load_scene() throws IOException
    {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Cars_list.fxml"));

        Parent root = loader.load();
        Scene scene = new Scene(root, 1280, 720);
        WypozyczalniaOkno.getPrimaryStage().setScene(scene);

    }


    public static class ImageItem {
        private final String description;
        private final Image image;

        public ImageItem(String description, Image image) {
            this.description = description;
            this.image = image;
        }

        public String getDescription() {
            return description;
        }

        public Image getImage() {
            return image;
        }
    }
}


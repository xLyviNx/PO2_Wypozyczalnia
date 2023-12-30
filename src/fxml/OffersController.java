package fxml;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import src.WypozyczalniaOkno;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.Parent;




import java.io.IOException;

public class OffersController {

    @FXML
    private ListView<ImageItem> list;
    @FXML
    private FlowPane flow;
    @FXML
    public void initialize() {
    /*list.getItems().add(new ImageItem("tank",new Image("./src/Tank jednostka.png")));
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
     */
    }
    @FXML
    public void load_scene() throws IOException
    {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CarsList.fxml"));

        Parent root = loader.load();
        Scene scene = new Scene(root, 1280, 720);
        String css = this.getClass().getResource("/fxml/style1.css").toExternalForm();
        scene.getStylesheets().add(css);
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


package fxml;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import src.Client;
import src.WypozyczalniaOkno;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import javafx.scene.image.ImageView;
import javafx.beans.value.ChangeListener;

public class OfferDetailsController
{
    public static OfferDetailsController instance;
    public static Scene scene;
    @FXML
    private Label carname;
    @FXML
    private ImageView carphoto;
    @FXML
    private Button photoprev;
    @FXML
    private Button photonext;
    @FXML
    private Text infotext;
    @FXML
    private Button deletebtn;
    @FXML
    private HBox buttonsbar;
    @FXML
    private AnchorPane anchor;
    private int currentImage;
    private double ImageWidth;
    private double ImageHeight;
    private double ImageRatio;
    private HBox imageParent;
    ArrayList<Image> images = new ArrayList<>();
    int carid;
    public static OfferDetailsController openScene(int id) {
        try {
            URL path = OffersController.class.getResource("/fxml/offerDetails.fxml");
            System.out.println("FXML Path: " + path);

            if (path == null) {
                System.err.println("FXML file not found.");
                return null;
            }

            FXMLLoader loader = new FXMLLoader(path);
            Parent root = loader.load();

            // Create the scene
            Scene scene = new Scene(root, 1280, 720);

            // Apply the CSS style
            URL cssPath = OffersController.class.getResource("/fxml/style1.css");
            System.out.println("CSS Path: " + cssPath);

            if (cssPath != null) {
                scene.getStylesheets().add(cssPath.toExternalForm());
            } else {
                System.err.println("CSS file not found.");
            }

            // Set the scene to the primary stage
            Stage primaryStage = WypozyczalniaOkno.getPrimaryStage();
            primaryStage.setScene(scene);

            // Show the stage
            primaryStage.show();
            // Return the controller instance if needed
            instance = loader.getController();
            System.out.println(instance);
            instance.scene=scene;
            instance.StartScene(id);
            return instance;
        } catch (IOException e) {
            System.err.println("Error loading FXML file: " + e.getMessage());
            e.printStackTrace();
        } catch (RuntimeException e) {
            System.err.println("Runtime error during FXML loading: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unknown error during FXML loading.");
            e.printStackTrace();
        }
        return null;
    }
    public void StartScene(int id)
    {
        if (Client.instance != null)
        {
            Client.instance.RequestOffer(id);
        }
        carid = id;
        photoprev.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                System.out.println("Button clicked!");
                currentImage--;
                checkImage();
            }
        });
        photonext.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                System.out.println("Button 2 clicked!");
                currentImage++;
                checkImage();
            }
        });
        imageParent = (HBox)carphoto.getParent();
        ChangeListener<Number> listener = (obs, ov, nv) -> {
            adjustSize();
        };

        imageParent.widthProperty().addListener(listener);
        imageParent.heightProperty().addListener(listener);
    }

    void adjustSize() {
        if (ImageHeight == 0 || ImageWidth == 0)
            return;
        double parentWidth = imageParent.getWidth();
        double parentHeight = imageParent.getHeight();
        if (parentWidth / ImageWidth < parentHeight / ImageHeight) {
            carphoto.setFitWidth(parentWidth);
        } else {
            carphoto.setFitHeight(parentHeight);
        }
        carphoto.setPreserveRatio(true);
    }


    public void SetHeader(String header)
    {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (carname != null)
                {
                    carname.setText(header);
                }
            }
        });
    }
    public void SetDetails(String details)
    {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (carname != null)
                {
                    infotext.setText(details);
                }
            }
        });
    }
    public void AddImage(byte[] imageBytes)
    {
        System.out.println("C0");
        Platform.runLater(new Runnable() {
            @Override
            public void run()
            {
                System.out.println("C1");
                if (imageBytes.length > 0) {
                    System.out.println("C2");
                    try {
                        Image image = new Image(new ByteArrayInputStream(imageBytes));
                        if (image != null) {
                            images.add(image);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                System.out.println("C3");
                checkImage();
            }
        });
    }
    public void checkImage() {

        System.out.println("C4");
        photoprev.setVisible(!images.isEmpty() && currentImage >= 1);
        photonext.setVisible(!images.isEmpty() && currentImage < images.size() - 1);
        if (images.size() > currentImage && carphoto != null) {
            Image image = images.get(currentImage);
            setImageRatio(image);
        }
        else if (currentImage > images.size())
        {
            currentImage=images.size()-1;
        }else if (currentImage < images.size()-1)
        {
            currentImage=0;
        }
    }
    void setImageRatio(Image image)
    {
        ImageWidth = image.getWidth();
        ImageHeight = image.getHeight();            //saving the original image size and ratio
         ImageRatio = ImageWidth / ImageHeight;
        carphoto.setImage(image);
        adjustSize();
    }
    public void Powrot(MouseEvent mouseEvent)
    {
        OffersController.openScene();
    }

    public void Usun(MouseEvent mouseEvent)
    {

    }

    public void rezerwuj(MouseEvent mouseEvent)
    {
        ReservationController.openScene(carname.getText(), carid);
    }
}
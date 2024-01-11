package fxml;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import src.Client;
import src.WypozyczalniaOkno;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.Parent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

public class OffersController {
    @FXML
    private FlowPane flow;
    @FXML
    private VBox offerButtonTemplate;
    @FXML
    private HBox filtersbox;
    @FXML
    private Label label_user;
    @FXML
    public Button addOfferButton;
    @FXML
    public Button confirmationsButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Button filterButton;
    @FXML
    private Button sortButton;
    @FXML
    private TextField priceMin;
    @FXML
    private TextField priceMax;
    @FXML
    private TextField yearMin;
    @FXML
    private TextField yearMax;
    @FXML
    private TextField capMin;
    @FXML
    private TextField capMax;
    @FXML
    private ToggleGroup brand;
    @FXML
    private Button SortChangeButton;
    @FXML
    public VBox filterBrandsParent;
    public static OffersController instance;
    public Scene scene;
    private float priceMinValue = -1;
    private float priceMaxValue = -1;
    private int yearMinValue = -1;
    private int yearMaxValue = -1;
    private int engineCapMinValue = -1;
    private int engineCapMaxValue = -1;
    private String brandnameValue = null;
    boolean priceDESC;
    public void StartScene()
    {
        if (Client.instance != null)
        {
            Client.instance.RequestUsername();
            SortChangeButton.setText(priceDESC? "Aktualnie: Ceną w dół" : "Aktualnie: Ceną w górę");
            Refresh();
            Client.instance.RequestConfButton();
        }
    }
    public static OffersController openScene() {
        try {
            // Load the FXML file
            URL path = OffersController.class.getResource("/fxml/cars.fxml");
            if (path == null) {
                System.err.println("FXML file not found.");
                return null;
            }
            FXMLLoader loader = new FXMLLoader(path);
            Parent root = loader.load();

            Scene scene = new Scene(root, 1280, 720);

            URL cssPath = OffersController.class.getResource("/fxml/style1.css");
            if (cssPath != null) {
                scene.getStylesheets().add(cssPath.toExternalForm());
            } else {
                System.err.println("CSS file not found.");
            }

            Stage primaryStage = WypozyczalniaOkno.getPrimaryStage();
            primaryStage.setScene(scene);

            primaryStage.show();
            instance = loader.getController();
            instance.scene=scene;
            instance.StartScene();
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
    public static void AddOfferNode(String vehicleName, float price, byte[] imageBytes, int dbid, boolean isRent, int daysLeft) {
            if (instance != null && WypozyczalniaOkno.instance != null && instance.scene == WypozyczalniaOkno.getPrimaryStage().getScene()) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        instance.flow.getChildren().add(instance.createOfferNode(vehicleName, price, imageBytes, dbid, isRent, daysLeft));
                    }
                });
            }
    }
    public static void setUsername(String username) {
        if (instance != null && instance.label_user != null) {
            Platform.runLater(() -> {
                instance.label_user.setText(username);
            });
        } else {
            System.out.println("instance or usernameText is null: " + instance + ", " + instance.label_user);
        }
    }

    public void AddFilterBrand(String brandname)
    {
        RadioButton rb = new RadioButton(brandname);
        rb.setStyle("-fx-text-fill: white;");
        rb.setToggleGroup(brand);
        rb.setFont(new Font(15.0));
        filterBrandsParent.getChildren().add(rb);
    }
    private Node createOfferNode(String vehicleName, float price, byte[] imageBytes, int dbid, boolean isRent, int daysLeft) {
        VBox offerNode = new VBox();
        offerNode.getStyleClass().addAll( isRent? "offerButtonRent" : (daysLeft == -1? "offerButtonAwaiting" : "offerButton"));
        offerNode.setAlignment(Pos.CENTER); // Ustawienie środka dla całego VBox

        HBox imageBox = new HBox();
        ImageView imageView = new ImageView();
        if (imageBytes.length > 0) {
            try {
                Image image = new Image(new ByteArrayInputStream(imageBytes));
                imageView.setImage(image);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        imageView.setFitHeight(136.0);
        imageView.setFitWidth(279.0);
        imageBox.getChildren().add(imageView);

        VBox textInfo = new VBox();
        textInfo.getStyleClass().addAll("offerButtonBar");
        textInfo.setAlignment(Pos.CENTER); // Ustawienie środka dla VBox z tekstem

        Label vehicleLabel = new Label(vehicleName);
        vehicleLabel.getStyleClass().addAll("offerLabel");
        vehicleLabel.setFont(Font.font("Calibri Light", 20.0)); // Ustawienie rozmiaru tekstu

        Label priceLabel =null;
        if (isRent)
        {
            priceLabel = new Label("Pozostało " + daysLeft + " dni.");
        }
        else if (daysLeft==-1)
        {
            priceLabel = new Label("Oczekuje na rozpatrzenie");
        }
        else{
            priceLabel = new Label(String.format("%.2f zł/dzień", price));
        }
        priceLabel.getStyleClass().addAll("offerLabel");
        priceLabel.setFont(Font.font("Calibri Light", 16.0)); // Ustawienie rozmiaru tekstu

        textInfo.getChildren().addAll(vehicleLabel, priceLabel);

        offerNode.getChildren().addAll(imageBox, textInfo);
        if(!isRent && daysLeft!=-1) {
            offerNode.setOnMouseClicked(event -> {
                ClickOnItem(dbid);
            });
        }
        return offerNode;
    }
    @FXML
    public void LogoutButton()
    {
        if (Client.instance != null)
        {
            Client.instance.SendLogout();
        }
    }
    public void ClickOnItem(int id)
    {
        System.out.println("KLIKNIETO " + id);
        OfferDetailsController.openScene(id);
    }
    @FXML
    public void AddOfferButton()
    {
        AddOfferController.openScene();
    }
    @FXML
    public void Refresh()
    {
        flow.getChildren().clear();
        Client.instance.RequestOffers(brandnameValue, yearMinValue, yearMaxValue, engineCapMinValue, engineCapMaxValue, priceMinValue, priceMaxValue, priceDESC);
    }
    @FXML
    public void GoToConfirmations()
    {
        ConfirmationController.OpenScene();
    }
    @FXML
    public void FilterButton()
    {
        filtersbox.setVisible(true);
    }
    @FXML
    public void SortButton()
    {

        priceDESC = !priceDESC;
        SortChangeButton.setText(priceDESC? "Aktualnie: Ceną w dół" : "Aktualnie: Ceną w górę");
        Refresh();
    }
    @FXML
    public void CancelFilter()
    {
        filtersbox.setVisible(false);
    }
    @FXML
    public void ConfirmFilter()
    {
        priceMinValue = -1;
        priceMaxValue = -1;
        yearMinValue = -1;
        yearMaxValue = -1;
        engineCapMinValue = -1;
        engineCapMaxValue = -1;
        brandnameValue = null;
        try {
            if (brand.getSelectedToggle() != null)
                brandnameValue = ((RadioButton) brand.getSelectedToggle()).getText();

            if (!priceMin.getText().isEmpty()) {
                float parsedPriceMin = Float.parseFloat(priceMin.getText());
                if (parsedPriceMin >= 0) {
                    priceMinValue = parsedPriceMin;
                } else {
                    Client.MessageBox("Cena minimalna nie może być poniżej 0.", Alert.AlertType.ERROR);
                }
            }

            if (!priceMax.getText().isEmpty()) {
                float parsedPriceMax = Float.parseFloat(priceMax.getText());
                if (parsedPriceMax >= 0) {
                    priceMaxValue = parsedPriceMax;
                } else {
                    Client.MessageBox("Cena maksymalna nie może być poniżej 0.", Alert.AlertType.ERROR);
                }
            }

            if (!yearMin.getText().isEmpty()) {
                int parsedYearMin = Integer.parseInt(yearMin.getText());
                if (parsedYearMin >= 0) {
                    yearMinValue = parsedYearMin;
                } else {
                    Client.MessageBox("Rok minimalny nie może być poniżej 0.", Alert.AlertType.ERROR);
                }
            }

            if (!yearMax.getText().isEmpty()) {
                int parsedYearMax = Integer.parseInt(yearMax.getText());
                if (parsedYearMax >= 0) {
                    yearMaxValue = parsedYearMax;
                } else {
                    Client.MessageBox("Rok maksymalny nie może być poniżej 0.", Alert.AlertType.ERROR);
                }
            }

            if (!capMin.getText().isEmpty()) {
                int parsedEngineCapMin = Integer.parseInt(capMin.getText());
                if (parsedEngineCapMin >= 0) {
                    engineCapMinValue = parsedEngineCapMin;
                } else {
                    Client.MessageBox("Pojemność silnika minimalna nie może być poniżej 0.", Alert.AlertType.ERROR);
                }
            }

            if (!capMax.getText().isEmpty()) {
                int parsedEngineCapMax = Integer.parseInt(capMax.getText());
                if (parsedEngineCapMax >= 0) {
                    engineCapMaxValue = parsedEngineCapMax;
                } else {
                    Client.MessageBox("Pojemność silnika maksymalna nie może być poniżej 0.", Alert.AlertType.ERROR);
                }
            }
        } catch (NumberFormatException ex) {
            Client.MessageBox("Wprowadzono nieprawidłową wartość liczbową.", Alert.AlertType.ERROR);
        }
        Refresh();
        filtersbox.setVisible(false);
    }
}


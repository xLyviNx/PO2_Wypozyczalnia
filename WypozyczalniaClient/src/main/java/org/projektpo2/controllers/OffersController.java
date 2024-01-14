package org.projektpo2.controllers;

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
import org.projektpo2.Client;
import org.projektpo2.Utilities;
import org.projektpo2.WypozyczalniaOkno;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.Parent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.logging.*;

/**
 * Kontroler obsługujący widok ofert.
 */
public class OffersController {

    /**
     * Panel, który zawiera oferty.
     */
    @FXML
    private FlowPane flow;

    /**
     * Pasek z przyciskami filtrów.
     */
    @FXML
    private HBox filtersbox;

    /**
     * Etykieta wyświetlająca nazwę użytkownika.
     */
    @FXML
    private Label label_user;

    /**
     * Przycisk dodawania nowej oferty.
     */
    @FXML
    public Button addOfferButton;

    /**
     * Przycisk otwierający potwierdzenia rezerwacji.
     */
    @FXML
    public Button confirmationsButton;

    /**
     * Pole tekstowe do wprowadzania minimalnej ceny.
     */
    @FXML
    private TextField priceMin;

    /**
     * Pole tekstowe do wprowadzania maksymalnej ceny.
     */
    @FXML
    private TextField priceMax;

    /**
     * Pole tekstowe do wprowadzania minimalnego roku.
     */
    @FXML
    private TextField yearMin;

    /**
     * Pole tekstowe do wprowadzania maksymalnego roku.
     */
    @FXML
    private TextField yearMax;

    /**
     * Pole tekstowe do wprowadzania minimalnej pojemności silnika.
     */
    @FXML
    private TextField capMin;

    /**
     * Pole tekstowe do wprowadzania maksymalnej pojemności silnika.
     */
    @FXML
    private TextField capMax;

    /**
     * Grupa przycisków wyboru marki.
     */
    @FXML
    private ToggleGroup brand;

    /**
     * Przycisk zmiany sortowania.
     */
    @FXML
    private Button SortChangeButton;

    /**
     * Kontener na przyciski wyboru marek.
     */
    @FXML
    public VBox filterBrandsParent;

    /**
     * Instancja kontrolera.
     */
    public static OffersController instance;

    /**
     * Scena kontrolera.
     */
    public Scene scene;

    /**
     * Cena minimum.
     */
    private float priceMinValue = -1;

    /**
     * Cena maksimum.
     */
    private float priceMaxValue = -1;

    /**
     * Minimalny rok.
     */
    private int yearMinValue = -1;

    /**
     * Maksymalny rok.
     */
    private int yearMaxValue = -1;

    /**
     * Minimalna pojemność silnika.
     */
    private int engineCapMinValue = -1;

    /**
     * Maksymalna pojemność silnika.
     */
    private int engineCapMaxValue = -1;

    /**
     * Wybrana marka.
     */
    private String brandnameValue = null;

    /**
     * Kierunek sortowania cen (malejąco/rosnąco).
     */
    boolean priceDESC;

    /**
     * Logger dla kontrolera ofert.
     */
    private static final Logger logger = Utilities.getLogger(OffersController.class);

    /**
     * Inicjuje scenę, wysyła zapytania do serwera.
     */
    public void StartScene() {
        if (Client.instance != null) {
            Client.instance.RequestUsername();
            SortChangeButton.setText(priceDESC ? "Aktualnie: Ceną w dół" : "Aktualnie: Ceną w górę");
            Refresh();
            Client.instance.RequestConfButton();
        }
    }

    /**
     * Otwiera scenę z widokiem ofert.
     *
     * @return Instancja kontrolera ofert.
     */
    public static OffersController openScene() {
        try {
            URL path = OffersController.class.getResource("/org/projektpo2/fxml/cars.fxml");
            if (path == null) {
                logger.log(Level.SEVERE, "FXML file not found.");
                return null;
            }
            FXMLLoader loader = new FXMLLoader(path);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 720);
            URL cssPath = OffersController.class.getResource("/org/projektpo2/fxml/style1.css");
            if (cssPath != null) {
                scene.getStylesheets().add(cssPath.toExternalForm());
            } else {
                logger.log(Level.WARNING, "CSS file not found (OffersController).");
            }
            Stage primaryStage = WypozyczalniaOkno.getPrimaryStage();
            primaryStage.setScene(scene);
            primaryStage.show();
            instance = loader.getController();
            instance.scene = scene;
            instance.StartScene();
            return instance;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading FXML file: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Runtime error during FXML loading: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unknown error during FXML loading.", e);
        }
        return null;
    }

    /**
     * Dodaje węzeł oferty do interfejsu użytkownika.
     *
     * @param vehicleName Nazwa pojazdu.
     * @param price Cena wynajmu pojazdu na dzień.
     * @param imageBytes Dane obrazu reprezentującego pojazd.
     * @param dbid Identyfikator pojazdu w bazie danych.
     * @param isRent Czy pojazd jest wynajęty.
     * @param daysLeft Liczba dni pozostałych do końca wynajmu.
     */
    public static void AddOfferNode(String vehicleName, float price, byte[] imageBytes, int dbid, boolean isRent, int daysLeft) {
        if (instance != null && WypozyczalniaOkno.instance != null && instance.scene == WypozyczalniaOkno.getPrimaryStage().getScene()) {
            Platform.runLater(() -> {
                instance.flow.getChildren().add(instance.createOfferNode(vehicleName, price, imageBytes, dbid, isRent, daysLeft));
            });
        }
    }

    /**
     * Ustawia nazwę użytkownika w interfejsie.
     *
     * @param username Nazwa użytkownika.
     */
    public static void setUsername(String username) {
        if (instance != null && instance.label_user != null) {
            Platform.runLater(() -> {
                instance.label_user.setText(username);
            });
        } else {
            logger.log(Level.WARNING, "instance or usernameText is null: " + instance + ", " + instance.label_user);
        }
    }

    /**
     * Dodaje filtr marki do interfejsu.
     *
     * @param brandname Nazwa marki.
     */
    public void AddFilterBrand(String brandname) {
        RadioButton rb = new RadioButton(brandname);
        rb.setStyle("-fx-text-fill: white;");
        rb.setToggleGroup(brand);
        rb.setFont(new Font(15.0));
        filterBrandsParent.getChildren().add(rb);
    }

    /**
     * Tworzy węzeł oferty.
     *
     * @param vehicleName Nazwa pojazdu.
     * @param price Cena pojazdu.
     * @param imageBytes Dane obrazu reprezentującego pojazd.
     * @param dbid Identyfikator pojazdu w bazie danych.
     * @param isRent Czy pojazd jest wynajęty.
     * @param daysLeft Liczba dni pozostałych do końca wynajmu.
     * @return Węzeł oferty.
     */
    private Node createOfferNode(String vehicleName, float price, byte[] imageBytes, int dbid, boolean isRent, int daysLeft) {
        VBox offerNode = new VBox();
        offerNode.getStyleClass().addAll(isRent ? "offerButtonRent" : (daysLeft == -1 ? "offerButtonAwaiting" : "offerButton"));
        offerNode.setAlignment(Pos.CENTER);
        HBox imageBox = new HBox();
        ImageView imageView = new ImageView();
        if (imageBytes.length > 0) {
            try {
                Image image = new Image(new ByteArrayInputStream(imageBytes));
                imageView.setImage(image);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Error loading image: " + ex.getMessage(), ex);
            }
        }
        imageView.setFitHeight(136.0);
        imageView.setFitWidth(279.0);
        imageBox.getChildren().add(imageView);
        VBox textInfo = new VBox();
        textInfo.getStyleClass().addAll("offerButtonBar");
        textInfo.setAlignment(Pos.CENTER);
        Label vehicleLabel = new Label(vehicleName);
        vehicleLabel.getStyleClass().addAll("offerLabel");
        vehicleLabel.setFont(Font.font("Calibri Light", 20.0));
        Label priceLabel = null;
        if (isRent) {
            priceLabel = new Label("Pozostało " + daysLeft + " dni.");
        } else if (daysLeft == -1) {
            priceLabel = new Label("Oczekuje na rozpatrzenie");
        } else {
            priceLabel = new Label(String.format("%.2f zł/dzień", price));
        }
        priceLabel.getStyleClass().addAll("offerLabel");
        priceLabel.setFont(Font.font("Calibri Light", 16.0));
        textInfo.getChildren().addAll(vehicleLabel, priceLabel);
        offerNode.getChildren().addAll(imageBox, textInfo);
        if (!isRent && daysLeft != -1) {
            offerNode.setOnMouseClicked(event -> {
                ClickOnItem(dbid);
            });
        }
        return offerNode;
    }

    /**
     * Obsługuje kliknięcie na element oferty.
     *
     * @param id Identyfikator pojazdu.
     */
    public void ClickOnItem(int id) {
        logger.log(Level.INFO, "Clicked item ID: " + id);
        OfferDetailsController.openScene(id);
    }

    /**
     * Obsługuje naciśnięcie przycisku wylogowania.
     */
    @FXML
    public void LogoutButton() {
        if (Client.instance != null) {
            Client.instance.SendLogout();
        }
    }

    /**
     * Otwiera scenę z dodawaniem nowej oferty.
     */
    @FXML
    public void AddOfferButton() {
        AddOfferController.openScene();
    }

    /**
     * Odświeża widok ofert.
     */
    @FXML
    public void Refresh() {
        flow.getChildren().clear();
        Client.instance.RequestOffers(brandnameValue, yearMinValue, yearMaxValue, engineCapMinValue, engineCapMaxValue, priceMinValue, priceMaxValue, priceDESC);
    }

    /**
     * Przechodzi do widoku potwierdzeń.
     */
    @FXML
    public void GoToConfirmations() {
        ConfirmationController.OpenScene();
    }

    /**
     * Wyświetla panel z filtrami.
     */
    @FXML
    public void FilterButton() {
        filtersbox.setVisible(true);
    }

    /**
     * Zmienia kierunek sortowania.
     */
    @FXML
    public void SortButton() {
        priceDESC = !priceDESC;
        SortChangeButton.setText(priceDESC ? "Aktualnie: Ceną w dół" : "Aktualnie: Ceną w górę");
        Refresh();
    }

    /**
     * Anuluje i zamyka widok filtrów.
     */
    @FXML
    public void CancelFilter() {
        filtersbox.setVisible(false);
    }

    /**
     * Potwierdza zastosowanie filtrów na ofertach.
     * Zmienia wartości filtrów na podstawie wprowadzonych danych, a następnie odświeża widok ofert.
     * W przypadku błędnych danych liczbowych, wyświetla komunikat błędu.
     */
    @FXML
    public void ConfirmFilter() {
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

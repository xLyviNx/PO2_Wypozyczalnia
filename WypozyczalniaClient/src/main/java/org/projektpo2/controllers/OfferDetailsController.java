package org.projektpo2.controllers;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.projektpo2.Client;
import org.projektpo2.Utilities;
import org.projektpo2.WypozyczalniaOkno;
import org.projektpo2.packets.VehiclePacket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.*;
/**
 * Kontroler obsługujący szczegóły oferty pojazdu.
 */
public class OfferDetailsController {
    /**
     * Logger służący do logowania zdarzeń w klasie OfferDetailsController.
     */
    private static final Logger logger = Utilities.getLogger(OfferDetailsController.class);

    /**
     * Instancja kontrolera.
     */
    public static OfferDetailsController instance;

    /**
     * Scena dla kontrolera.
     */
    public static Scene scene;

    /**
     * Etykieta wyświetlająca nazwę pojazdu.
     */
    @FXML
    private Label carname;

    /**
     * Obrazek wyświetlający zdjęcie pojazdu.
     */
    @FXML
    private ImageView carphoto;

    /**
     * Przycisk do przeglądania poprzedniego zdjęcia.
     */
    @FXML
    private Button photoprev;

    /**
     * Przycisk do przeglądania następnego zdjęcia.
     */
    @FXML
    private Button photonext;

    /**
     * Pole tekstowe wyświetlające informacje o pojeździe.
     */
    @FXML
    private Text infotext;

    /**
     * Przycisk do usuwania oferty.
     */
    @FXML
    public Button deletebtn;

    /**
     * Indeks aktualnie wyświetlanego zdjęcia.
     */
    private int currentImage;

    /**
     * Szerokość obrazu.
     */
    private double ImageWidth;

    /**
     * Wysokość obrazu.
     */
    private double ImageHeight;

    /**
     * Rodzic obrazu.
     */
    private HBox imageParent;

    /**
     * Lista obrazów.
     */
    private ArrayList<Image> images = new ArrayList<>();

    /**
     * Cena oferty.
     */
    public float price;

    /**
     * Identyfikator pojazdu.
     */
    int carid;

    /**
     * Otwiera scenę szczegółów oferty dla danego identyfikatora pojazdu.
     *
     * @param id Identyfikator pojazdu.
     * @return Instancja kontrolera szczegółów oferty.
     */
    public static OfferDetailsController openScene(int id) {
        try {
            URL path = OffersController.class.getResource("/org/projektpo2/fxml/offerDetails.fxml");
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
                logger.log(Level.WARNING, "CSS file not found (Offer Details).");
            }
            Stage primaryStage = WypozyczalniaOkno.getPrimaryStage();
            primaryStage.setScene(scene);
            primaryStage.show();
            instance = loader.getController();
            instance.scene = scene;
            instance.StartScene(id);
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
     * Inicjalizuje scenę szczegółów oferty.
     *
     * @param id Identyfikator pojazdu.
     */
    public void StartScene(int id) {
        if (Client.instance != null) {
            Client.instance.RequestOffer(id);
        }
        carid = id;
        photoprev.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            logger.info("Photoprev button clicked!");
            currentImage--;
            checkImage();
        });
        photonext.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            logger.info("Photonext button clicked!");
            currentImage++;
            checkImage();
        });
        imageParent = (HBox) carphoto.getParent();
        ChangeListener<Number> listener = (obs, ov, nv) -> adjustSize();

        imageParent.widthProperty().addListener(listener);
        imageParent.heightProperty().addListener(listener);
    }

    /**
     * Dostosowuje rozmiar obrazu do wymiarów widoku.
     */
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

    /**
     * Ustawia nagłówek dla oferty.
     *
     * @param header Nagłówek oferty.
     */
    public void SetHeader(String header) {
        Platform.runLater(() -> {
            if (carname != null) {
                carname.setText(header);
            }
        });
    }

    /**
     * Ustawia szczegóły dla oferty.
     *
     * @param details Szczegóły oferty.
     */
    public void SetDetails(String details) {
        Platform.runLater(() -> {
            if (carname != null) {
                infotext.setText(details);
            }
        });
    }

    /**
     * Dodaje obraz do listy.
     *
     * @param imageBytes Dane obrazu jako tablica bajtów.
     */
    public void AddImage(byte[] imageBytes) {
        Platform.runLater(() -> {
            if (imageBytes.length > 0) {
                try {
                    Image image = new Image(new ByteArrayInputStream(imageBytes));
                    if (image != null) {
                        images.add(image);
                    }
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Error adding image: " + ex.getMessage(), ex);
                }
            }
            checkImage();
        });
    }

    /**
     * Sprawdza i aktualizuje widoczność przycisków przewijania zdjęć.
     */
    public void checkImage() {
        photoprev.setVisible(!images.isEmpty() && currentImage >= 1);
        photonext.setVisible(!images.isEmpty() && currentImage < images.size() - 1);
        if (images.size() > currentImage && carphoto != null) {
            Image image = images.get(currentImage);
            setImageSizes(image);
        } else if (currentImage > images.size()) {
            currentImage = images.size() - 1;
        } else if (currentImage < images.size() - 1) {
            currentImage = 0;
        }
    }

    /**
     * Ustawia współczynniki proporcji obrazu.
     *
     * @param image Obiekt obrazu.
     */
    void setImageSizes(Image image) {
        ImageWidth = image.getWidth();
        ImageHeight = image.getHeight();
        carphoto.setImage(image);
        adjustSize();
    }

    /**
     * Obsługuje przycisk powrotu do poprzedniej sceny.
     *
     * @param mouseEvent Zdarzenie myszy.
     */
    public void Powrot(MouseEvent mouseEvent) {
        OffersController.openScene();
    }

    /**
     * Obsługuje przycisk usuwania oferty.
     *
     * @param mouseEvent Zdarzenie myszy.
     */
    public void Usun(MouseEvent mouseEvent) {
        if (Client.instance != null) {
            Client.instance.RequestDelete(carid);
        }
    }

    /**
     * Obsługuje przycisk rezerwacji oferty.
     *
     * @param mouseEvent Zdarzenie myszy.
     */
    public void rezerwuj(MouseEvent mouseEvent) {
        ReservationController.openScene(carname.getText(), carid, price);
    }

    /**
     * Obsługuje odpowiedź dotyczącą szczegółów oferty.
     *
     * @param vp Obiekt pakietu zawierającego szczegóły pojazdu.
     */
    public void handleOfferDetailsResponse(VehiclePacket vp) {
        updateHeaderAndDetails(vp);
        updateDeleteButtonVisibility(vp);
        addImages(vp.images);
        checkImage();
    }

    /**
     * Aktualizuje nagłówek i szczegóły oferty.
     *
     * @param vp Obiekt pakietu zawierającego szczegóły pojazdu.
     */
    public void updateHeaderAndDetails(VehiclePacket vp) {
        String header = vp.brand + " " + vp.model;
        String details = String.format("%s\nRok produkcji: %s\nSilnik: %s, (%s ccm)\nCena za dzień: %s\n%s",
                header, vp.year, vp.engine, vp.engineCap, String.format("%.2f", vp.price), vp.description);

        Platform.runLater(() -> {
            SetHeader(header);
            SetDetails(details);
            price = vp.price;
        });
    }

    /**
     * Aktualizuje widoczność przycisku usuwania w zależności od możliwości usunięcia oferty.
     *
     * @param vp Obiekt pakietu zawierającego szczegóły pojazdu.
     */
    public void updateDeleteButtonVisibility(VehiclePacket vp) {
        Platform.runLater(() -> {
            if (!vp.canBeDeleted) {
                try {
                    HBox btnpar = (HBox) deletebtn.getParent();
                    btnpar.getChildren().remove(deletebtn);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Error updating delete button visibility: " + ex.getMessage(), ex);
                }
            } else {
                deletebtn.setVisible(true);
            }
        });
    }

    /**
     * Dodaje obrazy do listy.
     *
     * @param images Lista obrazów w postaci tablic bajtów.
     */
    public void addImages(ArrayList<byte[]> images) {
        for (byte[] img : images) {
            AddImage(img);
        }
    }
}

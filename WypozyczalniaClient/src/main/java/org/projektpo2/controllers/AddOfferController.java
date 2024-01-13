package org.projektpo2.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.projektpo2.Client;
import org.projektpo2.Utilities;
import org.projektpo2.WypozyczalniaOkno;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AddOfferController {
    public static AddOfferController instance;
    public Scene scene;
    private static final Logger logger = Logger.getLogger(AddOfferController.class.getName());
    @FXML
    private TextField marka;
    @FXML
    private TextField model;
    @FXML
    private TextField rokprod;
    @FXML
    private TextField silnik;
    @FXML
    private TextField cena;
    @FXML
    private TextArea opis;
    @FXML
    public Button but_confirm;
    @FXML
    private Label thumbText;
    @FXML
    private Label imagesText;
    @FXML
    private Button imagesButton;
    @FXML
    private Button thumbButton;
    private byte[] selectedThumbnail;
    private ArrayList<byte[]> selectedImages;
    private ArrayList<String> selectedImagesNames = new ArrayList<>();
    private String selectedImageName;
    @FXML
    private TextField enginecap;

    public static AddOfferController openScene() {
        try {
            URL path = OffersController.class.getResource("/org/projektpo2/fxml/addOffer.fxml");
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
                logger.log(Level.WARNING, "CSS file not found (Add Offer).");
            }
            Stage primaryStage = WypozyczalniaOkno.getPrimaryStage();
            primaryStage.setScene(scene);
            primaryStage.show();
            instance = loader.getController();
            logger.info("Scene opened: " + instance);
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

    private void StartScene() {
        logger.info("Scene started");
    }

    @FXML
    public void imagesPress() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPG Files", "*.jpg"));

        int MAX_CHARACTERS_PER_IMAGE = 15;
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            if (selectedFiles.size() > 5) {
                Client.MessageBox("Możesz wybrać maksymalnie 5 plików.", Alert.AlertType.WARNING);
                return;
            }

            selectedImages = new ArrayList<>();
            long totalSize = 0;
            StringBuilder fileList = new StringBuilder();

            for (File selectedFile : selectedFiles) {
                logger.info("Selected file: " + selectedFile.getAbsolutePath());
                byte[] img = Utilities.loadImageAsBytes(selectedFile.getAbsolutePath());

                if (img != null && img.length > 0 && img.length <= 2000000) {
                    selectedImages.add(img);

                    if (fileList.length() > 0) {
                        fileList.append(";");
                    }
                    fileList.append(selectedFile.getName());
                    selectedImagesNames.add(selectedFile.getName());
                    totalSize += img.length;
                } else {
                    Client.MessageBox("Nie udało się odczytać pliku: " + selectedFile.getName(), Alert.AlertType.ERROR);
                }
            }

            imagesButton.setText(fileList.toString() + " " + Utilities.bytesFormatter(totalSize));
        } else {
            logger.info("No files selected");
        }
    }

    @FXML
    public void thumbnailPress() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPG Files", "*.jpg"));

        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            logger.info("Selected file: " + selectedFile.getAbsolutePath());
            byte[] img = Utilities.loadImageAsBytes(selectedFile.getAbsolutePath());

            if (img != null && img.length > 0) {
                if (img.length <= 2000000) {
                    selectedThumbnail = img;
                    thumbButton.setText(selectedFile.getName() + " (" + Utilities.bytesFormatter(img.length) + ")");
                    selectedImageName = selectedFile.getName();
                } else {
                    Client.MessageBox("Plik przekracza 2 MB!", Alert.AlertType.ERROR);
                }
            } else {
                Client.MessageBox("Nie udało się odczytać pliku.", Alert.AlertType.ERROR);
            }
        } else {
            logger.info("No file selected");
        }
    }

    @FXML
    public void Cancel() {
        OffersController.openScene();
        logger.info("Cancel button pressed");
    }

    @FXML
    public void Confirm() {
        if (Client.instance != null) {
            try {
                int currentYear = Year.now().getValue();
                int year = Integer.parseInt(rokprod.getText());
                float price = Float.parseFloat(cena.getText());
                int engineCapacity = Integer.parseInt(enginecap.getText());
                if (year < 1900 || year > currentYear) {
                    throw new IllegalArgumentException("Nieprawidłowy rok. Proszę wprowadzić rok między 1900 a " + currentYear + ".");
                }

                if (engineCapacity <= 0) {
                    throw new IllegalArgumentException("Nieprawidłowa pojemność silnika. Proszę podać poprawną pojemność silnika.");
                }

                if (price <= 0) {
                    throw new IllegalArgumentException("Nieprawidłowa cena. Proszę podać poprawną cenę.");
                }

                but_confirm.setVisible(false);

                if (selectedImages == null) {
                    selectedImages = new ArrayList<>();
                }

                if (selectedThumbnail == null)
                    selectedThumbnail = new byte[0];

                Client.instance.RequestAddOffer(marka.getText(), model.getText(), year, silnik.getText(), price, opis.getText(), selectedThumbnail, selectedImageName, selectedImages, selectedImagesNames, engineCapacity);
                logger.info("Offer confirmation requested");
            } catch (NumberFormatException ex) {
                Client.MessageBox("Proszę wprowadzić poprawne wartości liczbowe dla roku, ceny i pojemności silnika.", Alert.AlertType.ERROR);
                logger.log(Level.SEVERE, "NumberFormatException: " + ex.getMessage(), ex);
            } catch (IllegalArgumentException ex) {
                Client.MessageBox(ex.getMessage(), Alert.AlertType.ERROR);
                logger.log(Level.SEVERE, "IllegalArgumentException: " + ex.getMessage(), ex);
            } catch (Exception ex) {
                Client.MessageBox("Wystąpił nieoczekiwany błąd: " + ex.getMessage(), Alert.AlertType.ERROR);
                logger.log(Level.SEVERE, "Unexpected exception: " + ex.getMessage(), ex);
            }
        }
    }
}

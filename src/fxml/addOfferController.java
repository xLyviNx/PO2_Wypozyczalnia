package fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import src.Client;
import src.Utilities;
import src.WypozyczalniaOkno;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class addOfferController
{
    public static addOfferController instance;
    public Scene scene;

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
    private String selectedImagesNames;
    private String selectedImageName;

    public static addOfferController openScene() {
        try {
            URL path = OffersController.class.getResource("/fxml/addOffer.fxml");
            if (path == null) {
                System.err.println("FXML file not found.");
                return null;
            }
            FXMLLoader loader = new FXMLLoader(path);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 720);
            URL cssPath = OffersController.class.getResource("/fxml/style1.css");
            System.out.println("CSS Path: " + cssPath);
            if (cssPath != null) {
                scene.getStylesheets().add(cssPath.toExternalForm());
            } else {
                System.err.println("CSS file not found.");
            }
            Stage primaryStage = WypozyczalniaOkno.getPrimaryStage();
            primaryStage.setScene(scene);
            primaryStage.show();
            instance = loader.getController();
            System.out.println(instance);
            instance.scene = scene;
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
    private void StartScene()
    {
        
    }
    @FXML
    public void imagesPress()
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPG Files", "*.jpg"));

        // Show open file dialog
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            // Sprawdź, czy nie wybrano więcej niż 5 plików
            if (selectedFiles.size() > 5) {
                Client.MessageBox("Możesz wybrać maksymalnie 5 plików.", Alert.AlertType.WARNING);
                return;
            }

            // Process the selected image files
            selectedImages = new ArrayList<>();

            long totalSize=0;
            for (File selectedFile : selectedFiles) {
                System.out.println("Selected file: " + selectedFile.getAbsolutePath());
                byte[] img = Utilities.loadImageAsBytes(selectedFile.getAbsolutePath(), true);

                if (img != null && img.length > 0 && img.length <= 2000000) {
                    selectedImages.add(img);
                    totalSize+=img.length;
                } else {
                    Client.MessageBox("Nie udało się odczytać pliku: " + selectedFile.getName(), Alert.AlertType.ERROR);
                }
            }

            // Update your UI or perform additional processing as needed
            if (!selectedImages.isEmpty()) {
                // For example, display the names of the selected files
                StringBuilder fileList = new StringBuilder();
                for (File selectedFile : selectedFiles) {
                    fileList.append(selectedFile.getName()).append(";");
                }
                fileList.deleteCharAt(fileList.length() - 1); // Remove the last comma

                selectedImagesNames = fileList.toString();

                String size = (" (" + Utilities.bytesFormatter(totalSize)+")");
                imagesButton.setText(selectedImagesNames + size);
            }
        } else {
            System.out.println("No files selected");
        }
    }
    @FXML
    public void thumbnailPress() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPG Files", "*.jpg"));

        // Show open file dialog
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            // Process the selected image file
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
            byte[] img = Utilities.loadImageAsBytes(selectedFile.getAbsolutePath(), true);
            if (img != null && img.length>0)
            {
                if (img.length<=2000000) {
                    selectedThumbnail = img;
                    thumbButton.setText(selectedFile.getName() + " (" + Utilities.bytesFormatter(img.length) +")");
                    selectedImageName=selectedFile.getName();
                }
                else{
                    Client.MessageBox("Plik przekracza 2 MB!", Alert.AlertType.ERROR);
                }
            }
            else{
                Client.MessageBox("Nie udalo sie odczytac pliku.", Alert.AlertType.ERROR);
            }
        } else {
            System.out.println("No file selected");
        }
    }
    @FXML
    public void Cancel()
    {
        OffersController.openScene();
    }
    @FXML
    public void Confirm()
    {
        if (Client.instance != null)
        {

            try {
                int year = Integer.parseInt(rokprod.getText());
                float price = Float.parseFloat(cena.getText());
                but_confirm.setVisible(false);
                if (selectedImages== null)
                {
                    selectedImages = new ArrayList<>();
                }
                if (selectedThumbnail==null)
                    selectedThumbnail=new byte[0];
                Client.instance.RequestAddOffer(marka.getText(), model.getText(), year, silnik.getText(), price, opis.getText(), selectedThumbnail, selectedImageName, selectedImages, selectedImagesNames);
            }
            catch (Exception ex)
            {
                Client.MessageBox(ex.getLocalizedMessage(), Alert.AlertType.ERROR);
            }
        }
    }
}

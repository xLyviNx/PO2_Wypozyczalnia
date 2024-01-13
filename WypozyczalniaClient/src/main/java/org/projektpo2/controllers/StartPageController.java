package org.projektpo2.controllers;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;

import javafx.scene.Scene;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

import org.projektpo2.WypozyczalniaOkno;
public class StartPageController {
    @FXML
    private Button buttonLogowanie;

    @FXML
    private Button buttonRejestracja;

    private WypozyczalniaOkno wypozyczalniaOkno;

    public void setWypozyczalniaOkno(WypozyczalniaOkno wypozyczalniaOkno)
    {
        this.wypozyczalniaOkno = wypozyczalniaOkno;
    }
    @FXML
    public void load_scene() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/projektpo2/fxml/StartPage.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1280, 720);
        String css = this.getClass().getResource("/org/projektpo2/fxml/style1.css").toExternalForm();
        scene.getStylesheets().add(css);
        WypozyczalniaOkno.getPrimaryStage().setScene(scene);
    }
    @FXML
    public void handleButtonLogowanieAction(ActionEvent event) throws IOException
    {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/projektpo2/fxml/LogowanieNEW.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1280, 720);
        String css = this.getClass().getResource("/org/projektpo2/fxml/style1.css").toExternalForm();
        scene.getStylesheets().add(css);
        WypozyczalniaOkno.getPrimaryStage().setScene(scene);
    }
    @FXML
    public void handleButtonRegisterAction(ActionEvent event) throws IOException
    {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/projektpo2/fxml/RegisterScene.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1280, 720);
        String css = this.getClass().getResource("/org/projektpo2/fxml/style1.css").toExternalForm();
        scene.getStylesheets().add(css);
        WypozyczalniaOkno.getPrimaryStage().setScene(scene);
    }

}

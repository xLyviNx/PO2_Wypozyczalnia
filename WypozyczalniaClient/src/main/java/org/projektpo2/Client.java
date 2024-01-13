package org.projektpo2;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import org.projektpo2.controllers.*;
import org.projektpo2.packets.*;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Client {
    public static Client instance;
    public Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;

    public void start(String address, int port) throws Exception {
        if (isInstanceAlreadyRunning()) {
            return;
        }

        waitForWindowInstance();

        try {
            initializeSocket(address, port);
            startPingThread();

            while (socket.isConnected() && isWindowInstanceAvailable()) {
                NetData data = receiveData();
                if (data != null) {
                    handleReceivedData(data);
                }
            }

            System.out.println("DISCONNECTED.");
        } catch (ConnectException e) {
            handleConnectionException(e);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new DisconnectException();
        } finally {
            closeResources();
            resetInstance();
        }

        System.out.println("DC");
    }

    private boolean isInstanceAlreadyRunning() {
        return instance != null && instance != this;
    }

    private void waitForWindowInstance() {
        while (WypozyczalniaOkno.instance == null) {
            sleep(300);
        }
    }

    private void initializeSocket(String address, int port) throws IOException {
        instance = this;
        socket = new Socket(address, port);
        output = new ObjectOutputStream(socket.getOutputStream());
        input = new ObjectInputStream(socket.getInputStream());
    }

    private void startPingThread() {
        Thread pings = new Thread(() -> {
            while (socket.isConnected() && isWindowInstanceAvailable()) {
                sleep(1000);
                try {
                    sendPing();
                } catch (SocketException socketExc) {
                    handlePingError(socketExc);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        pings.start();
    }

    private void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isWindowInstanceAvailable() {
        return WypozyczalniaOkno.instance != null;
    }

    private void sendPing() throws IOException {
        output.writeObject(new NetData(NetData.Operation.Ping));
        output.flush();
    }

    private void handlePingError(SocketException socketExc) {
        System.out.println("ERROR PINGING");
        throw new DisconnectException();
    }

    private NetData receiveData() throws IOException, ClassNotFoundException {
        return (NetData) input.readObject();
    }

    private void handleConnectionException(ConnectException e) {
        System.out.println("Nie można połączyć się z serwerem: " + e.getMessage());
        throw new DisconnectException();
    }

    private void closeResources() {
        closeQuietly(input);
        closeQuietly(output);
        closeQuietly(socket);
    }

    private void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetInstance() {
        if (instance == this) {
            instance = null;
        }
    }


    public void RequestAddOffer(String brand, String model, int year, String engine, float price, String desc, byte[] thumbnail, String thumbnailname, ArrayList<byte[]> images, ArrayList<String> imagesnames, int ecap)
    {
        if (brand.length()>32)
        {
            MessageBox("Marka nie może być dłuższa niż 32 znaki.", Alert.AlertType.ERROR);
            return;
        }
        if (model.length()>32)
        {
            MessageBox("Model nie może być dłuższy niż 32 znaki.", Alert.AlertType.ERROR);
            return;
        }
        if (engine.length()>32)
        {
            MessageBox("Silnik nie może być dłuższy niż 32 znaki.", Alert.AlertType.ERROR);
            return;
        }

        VehiclePacket data = new VehiclePacket();

        data.brand = brand;
        data.model = model;
        data.year = year;
        data.engine = engine;
        data.price = price;
        data.description = desc;
        data.thumbnailPath = thumbnailname;
        data.imagePaths = imagesnames;
        data.thumbnail = thumbnail;
        data.images.addAll(images);
        data.databaseId=-1;
        data.operation = NetData.Operation.AddOffer;
        data.engineCap = ecap;
        SendRequest(data);

    }
    private void handleReceivedData(NetData data) throws IOException {
        //System.out.println("RECEIVED DATA");
        try {
            if (data.operationType == NetData.OperationType.Error) {
                handleErrorResponse(data);
            } else {
                handleOtherResponses(data);
            }
        }catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void handleErrorResponse(NetData data) {
        ErrorPacket ep = (ErrorPacket)data;
        if (ep==null)return;
        if (data.operation == NetData.Operation.ReservationRequest && ReservationController.instance != null) {
            Platform.runLater(() -> ReservationController.instance.but_reserve.setVisible(true));
        }
        MessageBox(ep.ErrorMessage, Alert.AlertType.ERROR);
    }
    private void handleOtherResponses(NetData data) {
        switch (data.operation) {
            case Register:
                handleRegisterResponse(data);
                break;
            case Login:
                handleLoginResponse(data);
                break;
            case OfferUsername:
                handleOfferUsernameResponse(data);
                break;
            case OfferElement:
                handleOfferElementResponse(data);
                break;
            case OfferDetails:
                handleOfferDetailsResponse(data);
                break;
            case Logout:
                handleLogoutResponse(data);
                break;
            case ReservationRequest:
                handleReservationRequestResponse(data);
                break;
            case addButton:
                handleAddButtonResponse(data);
                break;
            case AddOffer:
                handleAddOfferResponse(data);
                break;
            case DeleteOffer:
                handleDeleteOfferResponse(data);
                break;
            case ReservationElement:
                handleReservationElementResponse(data);
                break;
            case ManageReservation:
                handleReservationManagementResponse(data);
                break;
            case ConfirmationsButton:
                handleConfirmButton(data);
                break;
            case BrandsList:
                handleBrandsList(data);
                break;
            default:
                break;
        }
    }
    private void handleBrandsList(NetData data)
    {
        BrandsList blist = (BrandsList) data;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (OffersController.instance != null) {
                    OffersController.instance.filterBrandsParent.getChildren().clear();
                    OffersController.instance.AddFilterBrand("KAŻDA");
                    for (String br : blist.brands) {
                        if(br.isEmpty()) continue;
                        OffersController.instance.AddFilterBrand(br);
                    }
                }
            }
        });

    }
    private void handleConfirmButton(NetData data)
    {
        ConfirmButtonVisibility cbv = (ConfirmButtonVisibility) data;
        if (cbv==null)return;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (OffersController.instance!=null) {
                    HBox hbx = (HBox) OffersController.instance.confirmationsButton.getParent();
                    OffersController.instance.confirmationsButton.setVisible(cbv.isVisible);
                }
            }
        });
    }

    public void RequestConfButton()
    {
        NetData req = new NetData(NetData.Operation.ConfirmationsButton);
        SendRequest(req);
    }

    private void handleReservationManagementResponse(NetData data) {
        ManageReservationRequest mreq= (ManageReservationRequest)data;
        if (mreq==null)return;
        Platform.runLater(() -> {
            if (ConfirmationController.instance != null) {
                ConfirmationController.instance.Refresh();
                if (mreq.confirm)
                    MessageBox("Pomyślnie potwierdzono rezerwację.", Alert.AlertType.INFORMATION);
                else
                    MessageBox("Pomyślnie anulowano rezerwację.", Alert.AlertType.INFORMATION);
            }
        });
    }
    private void handleRegisterResponse(NetData data) {
        if (data.operationType == NetData.OperationType.Success) {
            MessageBox("Zarejestrowano.", Alert.AlertType.INFORMATION);
            Platform.runLater(OffersController::openScene);
        }
    }

    private void handleLoginResponse(NetData data) {
        if (data.operationType == NetData.OperationType.Success) {
            MessageBox("Zalogowano.", Alert.AlertType.INFORMATION);
            Platform.runLater(OffersController::openScene);
        }
    }

    private void handleOfferUsernameResponse(NetData data) {
        UsernamePacket up = (UsernamePacket) data;
        if (up != null) {
            OffersController.setUsername(up.username);
        }
    }

    private void handleOfferElementResponse(NetData data)
    {
        VehiclePacket vp = (VehiclePacket) data;
        if (vp!=null) {
            try {
                byte[] imgs = vp.thumbnail!=null ? vp.thumbnail : new byte[0];
                OffersController.AddOfferNode(vp.brand + " " + vp.model + " (" + vp.year + ") " + vp.engine, vp.price, imgs,
                        vp.databaseId, vp.isRented, vp.daysLeft);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.err.println("ERROR ADDING CAR");
            }
        }
    }

    private void handleOfferDetailsResponse(NetData data) {
        if (OfferDetailsController.instance == null) {
            return;
        }

        VehiclePacket vp = (VehiclePacket) data;
        OfferDetailsController offerDetailsController = OfferDetailsController.instance;
        offerDetailsController.updateHeaderAndDetails(vp);
        offerDetailsController.updateDeleteButtonVisibility(vp);
        offerDetailsController.addImages(vp.images);
        offerDetailsController.checkImage();
    }


    private void handleLogoutResponse(NetData data) {
        Platform.runLater(() -> {
            try {
                StartPageController spc = new StartPageController();
                spc.load_scene();
                MessageBox("Wylogowano.", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void handleReservationRequestResponse(NetData data) {
        if (data.operationType == NetData.OperationType.Success) {
            Platform.runLater(() -> {
                MessageBox("Zarezerwowano pojazd. Oczekuj próby kontaktu naszego agenta na twój numer telefonu.", Alert.AlertType.INFORMATION);
                OffersController.openScene();
            });
        }
    }

    private void handleAddButtonResponse(NetData data) {
        AddOfferButtonVisibility vis = (AddOfferButtonVisibility) data;
        Platform.runLater(() -> {
            if (OffersController.instance != null) {
                OffersController.instance.addOfferButton.setVisible(vis.isVisible);
            }
        });
    }

    private void handleAddOfferResponse(NetData data) {
        Platform.runLater(() -> {
            if (data.operationType == NetData.OperationType.Success) {
                MessageBox("Dodano ofertę.", Alert.AlertType.INFORMATION);
                OffersController.openScene();
            } else if (data.operationType == NetData.OperationType.Error) {
                if (AddOfferController.instance != null) {
                    AddOfferController.instance.but_confirm.setVisible(true);
                }
            }
        });
    }

    private void handleDeleteOfferResponse(NetData data) {
        Platform.runLater(() -> {
            if (data.operationType == NetData.OperationType.Success) {
                MessageBox("Usunięto ofertę.", Alert.AlertType.INFORMATION);
                OffersController.openScene();
            }
        });
    }

    private void handleReservationElementResponse(NetData data) {
        ReservationElement element = (ReservationElement) data;
        Platform.runLater(() -> {
            if (element!=null && element.carId!=0 && element.reserveId!=0) {
                if (ConfirmationController.instance != null) {
                    String text = "(" + element.reserveId + ") " + element.firstName + " " + element.lastName + " (" + element.login + ", " + element.phoneNumber + ")\n";
                    text += element.brand + " " + element.model + " (" + element.productionYear + ", ID: " + element.carId + "), CZAS: " + element.reserveDays + " dni, KOSZT: " + String.format("%.2f zł", element.reserveDays * element.dailyPrice) + ".";
                    ConfirmationController.instance.AddButton(text, element.reserveId);
                }
            }
        });
    }
    public void RequestCancelReservation(int id)
    {
        ManageReservationRequest req = new ManageReservationRequest(id,false);
        SendRequest(req);
    }
    public void RequestConfirmReservation(int id)
    {
        ManageReservationRequest req = new ManageReservationRequest(id,true);
        SendRequest(req);
    }
    public void RequestConfirmations()
    {
        NetData req = new NetData(NetData.Operation.RequestConfirmtations);
        SendRequest(req);
    }
    public void RequestDelete(int id)
    {
        DeleteOfferRequestPacket req = new DeleteOfferRequestPacket(id);
        SendRequest(req);
    }
    public void RequestReservation(int id, int days)
    {
        ReservationRequestPacket req = new ReservationRequestPacket(id,days);
        SendRequest(req);
    }
    public void RequestOffer(int id)
    {
        OfferDetailsRequestPacket request = new OfferDetailsRequestPacket(id);
        SendRequest(request);
    }
    public void RequestRegister(String username, String pwd, String pwdR, int phone, String imie, String nazwisko)
            throws DisconnectException {
        RegisterPacket request = new RegisterPacket();
        request.login=username;
        request.password = (MD5Encryptor.encryptPassword(pwd));
        request.repeat_password = (MD5Encryptor.encryptPassword(pwdR));
        request.phonenumber=phone;
        request.imie=imie;
        request.nazwisko=nazwisko;
        SendRequest(request);
    }
    public void SendLogout()
    {
        NetData request = new NetData(NetData.Operation.Logout);
        SendRequest(request);

    }
    public void RequestLogin(String username, String pwd) {
        LoginPacket request = new LoginPacket(username, MD5Encryptor.encryptPassword(pwd));
        SendRequest(request);
    }

    public void RequestUsername() {
        NetData req = new NetData(NetData.Operation.OfferUsername);
        SendRequest(req);
    }

    public void RequestOffers(String brand, int yearMin, int yearMax, int engineCapMin, int engineCapMax, float priceMin, float priceMax, boolean priceDESC) {
        FilteredOffersRequestPacket request = new FilteredOffersRequestPacket(brand, yearMin, yearMax, engineCapMin, engineCapMax, priceMin, priceMax, priceDESC);
        SendRequest(request);
    }

    void SendRequest(Object request) throws DisconnectException {
        if (!(request instanceof NetData))
            return;
        if (output != null && socket != null) {
            try {
                output.writeObject(request);
                output.flush();
            } catch (Exception e) {
                System.out.println("ERROR SENDING REQUEST");
                if (WypozyczalniaOkno.instance != null)
                    WypozyczalniaOkno.instance.NoConnection();
                throw new DisconnectException();
            }
        }
    }

    public static void MessageBox(String content, Alert.AlertType mtype) {
        Platform.runLater(() -> {
            Alert errorAlert = new Alert(mtype);
            errorAlert.setTitle(mtype == Alert.AlertType.ERROR ? "BŁĄD" : "Informacja");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText(content);
            errorAlert.show();
        });
    }
}

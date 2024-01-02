package src;

import fxml.*;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;

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
        if (instance != null && instance != this)
            return;

        while (WypozyczalniaOkno.instance == null) {
            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("WINDOW FOUND");

        try {
            instance = this;
            socket = new Socket(address, port);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            String receivedString;

            Thread pings = new Thread(() -> {
                while (socket.isConnected() && WypozyczalniaOkno.instance != null) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException iex) {
                        throw new RuntimeException(iex);
                    }
                    try {
                        output.writeObject(new NetData(NetData.Operation.Ping));
                        output.flush();
                    } catch (SocketException socketexc) {
                        System.out.println("ERROR PINGING");
                        socketexc.printStackTrace();
                        throw new DisconnectException();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            pings.start();

            while (socket.isConnected() && WypozyczalniaOkno.instance != null) {
                NetData data = (NetData) input.readObject();

                if (data != null) {
                    handleReceivedData(data);
                }
            }

            System.out.println("DISCONNECTED.");
        } catch (ConnectException e) {
            System.out.println("Nie można połączyć się z serwerem: " + e.getMessage());
            throw new DisconnectException();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new DisconnectException();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
                if (output != null) {
                    output.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (instance == this)
            instance = null;
        System.out.println("DC");
    }

    public void RequestAddOffer(String brand, String model, int year, String engine, float price, String desc, byte[] thumbnail, String thumbnailname, ArrayList<byte[]> images, String imagesnames)
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
        if (engine.length()>32)
        {
            MessageBox("Silnik nie może być dłuższy niż 32 znaki.", Alert.AlertType.ERROR);
            return;
        }
        NetData data = new NetData(NetData.Operation.AddOffer);
        data.Strings.add(brand);
        data.Strings.add(model);
        data.Integers.add(year);
        data.Strings.add(engine);
        data.Floats.add(price);
        data.Strings.add(desc);
        data.Strings.add(thumbnailname);
        data.Strings.add(imagesnames);
        data.Images.add(thumbnail);
        data.Images.addAll(images);
        SendRequest(data);
    }
    private void handleReceivedData(NetData data) throws IOException {
        System.out.println("RECEIVED DATA");
        if (data.operationType == NetData.OperationType.Error) {
            if (data.operation == NetData.Operation.ReservationRequest && ReservationController.instance != null)
            {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        ReservationController.instance.but_reserve.setVisible(true);
                    }
                });
            }
            MessageBox(data.Strings.get(0), Alert.AlertType.ERROR);
        }
        if (data.operationType == NetData.OperationType.MessageBox) {
            MessageBox(data.Strings.get(0), Alert.AlertType.INFORMATION);
        } else if (data.operation == NetData.Operation.Register) {
            if (data.operationType == NetData.OperationType.Success) {
                MessageBox("Zarejestrowano.", Alert.AlertType.INFORMATION);
                Platform.runLater(() -> {
                    OffersController offers = OffersController.openScene();
                });
            }
        } else if (data.operation == NetData.Operation.Login) {
            if (data.operationType == NetData.OperationType.Success) {
                MessageBox("Zalogowano.", Alert.AlertType.INFORMATION);
                Platform.runLater(() -> {
                    OffersController offers = OffersController.openScene();
                });
            }
        } else if (data.operation == NetData.Operation.OfferUsername) {
            if (data.Strings.size() == 1) {
                OffersController.setUsername(data.Strings.get(0).trim());
            }
        } else if (data.operation == NetData.Operation.OfferElement) {
            if (data.Strings.size() == 1 && data.Floats.size() == 1 && data.Integers.size() == 2
                    && (data.Images.size() == 1 || data.Images.isEmpty()) && data.Booleans.size() == 1) {
                //System.out.println("ADDING CAR");
                try {
                    byte[] imgs = data.Images.size() >0? data.Images.get(0) : new byte[0];
                    boolean isRent = data.Booleans.get(0);
                    int daysLeft = data.Integers.get(1);
                    OffersController.AddOfferNode(data.Strings.get(0).trim(), data.Floats.get(0), imgs,
                            data.Integers.get(0), isRent, daysLeft);
                }catch(Exception ex)
                {
                    ex.printStackTrace();
                    System.err.println("ERROR ADDING CAR");
                }
            }
        } else if (data.operation == NetData.Operation.OfferDetails) {
            if (data.Strings.size() == 2 && data.Floats.size() == 1 && data.Integers.size() == 1){
                //System.out.println("ADDING DETAILS");
                if (OfferDetailsController.instance != null)
                {
                    OfferDetailsController.instance.SetHeader(data.Strings.get(0).trim());
                    OfferDetailsController.instance.SetDetails(data.Strings.get(1).trim());
                    OfferDetailsController.instance.price = data.Floats.get(0);
                    //OfferDetailsController.instance.deletebtn.setVisible(!data.Booleans.isEmpty() && data.Booleans.get(0));
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if (data.Booleans.isEmpty() || !data.Booleans.get(0))
                            {
                                try {
                                    HBox btnpar = (HBox) OfferDetailsController.instance.deletebtn.getParent();
                                    btnpar.getChildren().remove(OfferDetailsController.instance.deletebtn);
                                }catch(Exception ex)
                                {
                                    ex.printStackTrace();
                                }
                            }else{
                                OfferDetailsController.instance.deletebtn.setVisible(true);
                            }
                        }
                    });

                    for (byte[] img : data.Images)
                    {
                        OfferDetailsController.instance.AddImage(img);
                    }
                    OfferDetailsController.instance.checkImage();
                }
            }
        } else if (data.operation == NetData.Operation.Logout) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        StartPageController spc = new StartPageController();
                        spc.load_scene();
                        MessageBox("Wylogowano.", Alert.AlertType.INFORMATION);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } else if (data.operation == NetData.Operation.ReservationRequest)
        {
            if (data.operationType == NetData.OperationType.Success)
            {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        MessageBox("Zarezerwowano pojazd. Oczekuj próby kontaktu naszego agenta na twój numer telefonu.", Alert.AlertType.INFORMATION);
                        OffersController.openScene();
                    }
                });
            }
        }
        else if (data.operation == NetData.Operation.addButton)
        {
            if (data.Booleans.size()==1)
            {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (OffersController.instance != null)
                        {
                            OffersController.instance.addOfferButton.setVisible(data.Booleans.get(0));
                        }
                    }
                });
            }
        }
        else if (data.operation == NetData.Operation.AddOffer)
        {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    if (data.operationType == NetData.OperationType.Success)
                    {
                        MessageBox("Dodano oferte.", Alert.AlertType.INFORMATION);
                        OffersController.openScene();
                    }
                    else if (data.operationType== NetData.OperationType.Error)
                    {
                        if (addOfferController.instance!=null)
                        {
                            addOfferController.instance.but_confirm.setVisible(true);
                        }
                    }
                }
            });

        }
        else if (data.operation == NetData.Operation.DeleteOffer)
        {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    if (data.operationType == NetData.OperationType.Success)
                    {
                        MessageBox("Usunięto ofertę.", Alert.AlertType.INFORMATION);
                        OffersController.openScene();
                    }
                }
            });

        }
        else if (data.operation == NetData.Operation.ReservationElement)
        {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    if (data.Strings.size()==5 && data.Floats.size()==1 && data.Integers.size()==5)
                    {
                        if (confirmationController.instance != null)
                        {
                            String text = "(" + data.Integers.get(0) + ") " + data.Strings.get(3) + " " + data.Strings.get(4) + " (" + data.Strings.get(2) + ", " + data.Integers.get(4) + ")\n";
                            text += data.Strings.get(0) + " " + data.Strings.get(1) + " (" + data.Integers.get(2) + ", ID: " + data.Integers.get(3) + "), CZAS: " + data.Integers.get(1) + " dni, KOSZT: " + String.format("%.2f zł", data.Integers.get(1) * data.Floats.get(0)) + ".";
                            confirmationController.instance.AddButton(text, data.Integers.get(0));
                        }
                    }
                }
            });
        }
        else if ((data.operation == NetData.Operation.CancelReservation || data.operation == NetData.Operation.ConfirmReservation) && data.operationType == NetData.OperationType.Success)
        {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    if (confirmationController.instance!=null)
                    {
                        confirmationController.instance.Refresh();
                        String oper = data.operation == NetData.Operation.CancelReservation ? "anulowano" : "potwierdzono";
                        MessageBox("Pomyślnie " + oper + " rezerwację.", Alert.AlertType.INFORMATION);
                    }
                }
            });
        }

    }
    public void RequestCancelReservation(int id)
    {
        NetData req = new NetData(NetData.Operation.CancelReservation);
        req.Integers.add(id);
        SendRequest(req);
    }
    public void RequestConfirmReservation(int id)
    {
        NetData req = new NetData(NetData.Operation.ConfirmReservation);
        req.Integers.add(id);
        SendRequest(req);
    }
    public void RequestConfirmations()
    {
        NetData req = new NetData(NetData.Operation.RequestConfirmtations);
        SendRequest(req);
    }
    public void RequestDelete(int id)
    {
        NetData req = new NetData(NetData.Operation.DeleteOffer);
        req.Integers.add(id);
        SendRequest(req);
    }
    public void RequestReservation(int id, int days)
    {
        NetData req = new NetData(NetData.Operation.ReservationRequest);
        req.Integers.add(id);
        req.Integers.add(days);
        SendRequest(req);
    }
    public void RequestOffer(int id)
    {
        System.out.println("Requesting OFFER");
        NetData request = new NetData(NetData.Operation.OfferDetails);
        request.Integers.add(id);
        SendRequest(request);
    }
    public void RequestRegister(String username, String pwd, String pwdR, int phone, String imie, String nazwisko)
            throws DisconnectException {
        NetData request = new NetData(NetData.Operation.Register);
        request.Strings.add(username);// 0
        request.Strings.add(MD5Encryptor.encryptPassword(pwd));// 1
        request.Strings.add(MD5Encryptor.encryptPassword(pwdR));// 2
        request.Integers.add(phone);// 0
        request.Strings.add(imie);// 3
        request.Strings.add(nazwisko);// 4
        SendRequest(request);
    }
    public void SendLogout()
    {
        NetData request = new NetData(NetData.Operation.Logout);
        SendRequest(request);

    }
    public void RequestLogin(String username, String pwd) {
        NetData request = new NetData(NetData.Operation.Login);
        request.Strings.add(username);// 0
        request.Strings.add(MD5Encryptor.encryptPassword(pwd));// 1
        SendRequest(request);
    }

    public void RequestUsername() {
        NetData req = new NetData(NetData.Operation.OfferUsername);
        SendRequest(req);
    }

    public void RequestOffers() {
        NetData req = new NetData(NetData.Operation.OfferElement);
        SendRequest(req);
    }

    void SendRequest(NetData request) throws DisconnectException {
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

package src;

import fxml.OfferDetailsController;
import fxml.OffersController;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
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

    private void handleReceivedData(NetData data) throws IOException {
        if (data.operationType == NetData.OperationType.Error) {
            MessageBox(data.Strings.get(0), Alert.AlertType.ERROR);
        } else if (data.operationType == NetData.OperationType.MessageBox) {
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
            if (data.Strings.size() == 1 && data.Floats.size() == 1 && data.Integers.size() == 1
                    && (data.Images.size() == 1 || data.Images.size() == 0)) {
                System.out.println("ADDING CAR");
                OffersController.AddOfferNode(data.Strings.get(0).trim(), data.Floats.get(0), data.Images.get(0),
                        data.Integers.get(0));
            } else {
                System.out.println(data.Strings.size());
                System.out.println(data.Floats.size());
                System.out.println(data.Integers.size());
                System.out.println(data.Images.size());
            }
        } else if (data.operation == NetData.Operation.OfferDetails) {
            if (data.Strings.size() == 2 && data.Floats.size() == 1 && data.Integers.size() == 1){
                System.out.println("ADDING DETAILS");
                if (OfferDetailsController.instance != null)
                {
                    OfferDetailsController.instance.SetHeader(data.Strings.get(0).trim());
                    OfferDetailsController.instance.SetDetails(data.Strings.get(1).trim());
                    for (byte[] img : data.Images)
                    {
                        OfferDetailsController.instance.AddImage(img);
                    }
                    OfferDetailsController.instance.checkImage();
                }
            }else{
                System.out.println("Received Wrong Car Details");
                System.out.println("Strings: " + data.Strings.size());
                System.out.println("Floats: " + data.Floats.size());
                System.out.println("Integers: " + data.Integers.size());
                System.out.println("Images: " + data.Images.size());
            }
        }
    }
    public void RequestOffer(int id)
    {
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

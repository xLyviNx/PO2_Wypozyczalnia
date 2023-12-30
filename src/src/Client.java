package src;
import fxml.OffersController;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.net.*;
import java.io.*;
import java.nio.Buffer;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Client {
    public static Client instance;
    public Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private BufferedInputStream bufInput;
    private BufferedOutputStream bufOutput;
    public void start(String address, int port) throws Exception {
        if (instance != null && instance!=this)
            return;
        //NetData dat = new NetData("TEST");
        while(WypozyczalniaOkno.instance == null)
        {
            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("WINDOW FOUND");
        try {
            instance=this;
            socket = new Socket(address, port);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            bufInput = new BufferedInputStream(input);
            bufOutput = new BufferedOutputStream(output);
            String receivedString;

            Thread pings = new Thread(()->
            {
                while (socket.isConnected() && WypozyczalniaOkno.instance != null)
                {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException iex) {
                        throw new RuntimeException(iex);
                    }
                    try {
                        output.writeUTF("ping");
                        output.flush();
                    }
                    catch (SocketException socketexc)
                    {
                        System.out.println("ERROR PINGING");
                        socketexc.printStackTrace();
                        throw new DisconnectException();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            pings.start();
            while (socket.isConnected() && WypozyczalniaOkno.instance != null)
            {
                receivedString = input.readUTF();
                if (receivedString != null && !receivedString.isEmpty())
                {
                    System.out.println(receivedString);
                    try {
                        NetData data = NetData.fromJSON(receivedString);
                        if (data!=null)
                        {
                            if (data.operationType== NetData.OperationType.Error)
                            {
                                MessageBox(data.Strings.get(0), Alert.AlertType.ERROR);
                            }
                            else if (data.operationType == NetData.OperationType.MessageBox)
                            {
                                MessageBox(data.Strings.get(0), Alert.AlertType.INFORMATION);
                            }
                            else if (data.operation== NetData.Operation.Register)
                            {
                               if (data.operationType == NetData.OperationType.Success)
                               {
                                    MessageBox("Zarejestrowano.", Alert.AlertType.INFORMATION);
                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            OffersController offers = new OffersController();
                                            try {
                                                offers.load_scene();
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }
                                    });
                               }
                            }
                            else if (data.operation== NetData.Operation.Login)
                            {
                                if (data.operationType == NetData.OperationType.Success)
                                {
                                    MessageBox("Zalogowano.", Alert.AlertType.INFORMATION);
                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            OffersController offers = new OffersController();
                                            try {
                                                offers.load_scene();
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }catch (Exception ex)
                    {
                        //NOT JSON
                        System.out.println("JSON FAILED. DATA IS PROBABLY NOT JSON NETDATA." + receivedString);
                    }

                }
            }

            System.out.println("DISCONNECTED.");
        } catch (ConnectException e) {
            System.out.println("Nie można połączyć się z serwerem: " + e.getMessage());
            throw new DisconnectException();
        } catch (IOException e) {
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

        if (instance==this)
            instance=null;
        System.out.println("DC");
        return;
    }
    public void RequestRegister(String username, String pwd, String pwdR, int phone, String imie, String nazwisko) throws DisconnectException
    {
        NetData request = new NetData(NetData.Operation.Register);
        request.Strings.add(username);//0
        request.Strings.add(MD5Encryptor.encryptPassword(pwd));//1
        request.Strings.add(MD5Encryptor.encryptPassword(pwdR));//2
        request.Integers.add(phone);//0
        request.Strings.add(imie);//3
        request.Strings.add(nazwisko);//4
        SendRequest(request);
    }
    public void RequestLogin(String username, String pwd)
    {
        NetData request = new NetData(NetData.Operation.Login);
        request.Strings.add(username);//0
        request.Strings.add(MD5Encryptor.encryptPassword(pwd));//1
        SendRequest(request);

    }
    public static void MessageBox(String content, Alert.AlertType mtype)
    {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Alert errorAlert = new Alert(mtype);
                errorAlert.setTitle(mtype == Alert.AlertType.ERROR? "BŁĄD" : "Informacja");
                errorAlert.setHeaderText(null);
                errorAlert.setContentText(content);
                errorAlert.show();
            }
        });
    }
    void SendRequest(NetData request) throws DisconnectException
    {
        if (output != null && socket != null)
        {
            try {
                output.writeUTF(request.toJSON());
                output.flush();
            }
            catch (Exception e)
            {
                System.out.println("ERROR SENDING REQUEST");
                if (WypozyczalniaOkno.instance != null)
                    WypozyczalniaOkno.instance.NoConnection();
                throw new DisconnectException();
            }
        }
    }
}


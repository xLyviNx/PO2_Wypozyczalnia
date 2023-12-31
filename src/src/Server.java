package src;

import com.google.gson.Gson;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    final int port = 12345;
    public ArrayList<User> connectedClients = new ArrayList<>();
    private ServerSocket serverSocket;

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        ExecutorService executor = Executors.newCachedThreadPool();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            executor.submit(() -> handleClient(clientSocket));
        }
    }

    private void handleClient(Socket socket) {
        User session = null;
        try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
            System.out.println("CLIENT CONNECTED " + socket);
            session = new User();
            session.isSignedIn = false;
            session.clientSocket = socket;
            connectedClients.add(session);
            output.writeObject(new NetData(NetData.Operation.Unspecified, "CONNECTED"));
            output.flush();

            while (true) {
                NetData data = (NetData) input.readObject();

                if (data.operation.equals(NetData.Operation.Exit)) {
                    System.out.println("Client " + socket + " sends exit...");
                    System.out.println("Connection closing...");
                    socket.close();
                    System.out.println("Closed");
                    if (session != null) {
                        connectedClients.remove(session);
                        session = null;
                    }
                    break;
                } else if (!data.operation.equals(NetData.Operation.Ping)) {
                    try {
                        switch (data.operation) {
                            case Register:
                                handleRegister(data, output, session);
                                break;
                            case Login:
                                handleLogin(data, output, session);
                                break;
                            case OfferUsername:
                                handleOfferUsername(output, session);
                                break;
                            case OfferElement:
                                handleOfferElement(output);
                                break;
                            default:
                                System.out.println(socket + " requested unknown operation.");
                                break;
                        }
                    } catch (SQLException e) {
                        NetData response = new NetData(NetData.Operation.Unspecified);
                        SendError(output, "Blad polaczenia z baza danych.", response);
                    }
                }
            }
        } catch (SocketException e) {
            System.out.println("Client " + socket + " unexpectedly closed the connection.");
            if (session != null) {
                connectedClients.remove(session);
                session = null;
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            if (session != null) {
                connectedClients.remove(session);
                session = null;
            }
        }
    }

    private void handleRegister(NetData data, ObjectOutputStream output, User session) throws IOException, SQLException {
        NetData response = new NetData(NetData.Operation.Register);

        if (session.isSignedIn) {
            SendError(output, "Jestes juz zalogowany!", response);
            return;
        }

        if (data.Strings.size() == 5 && data.Integers.size() == 1) {
            int numtel = data.Integers.get(0);

            // Sprawdzenia poprawności danych rejestracyjnych
            if (String.valueOf(numtel).length() == 9) {
                for (String s : data.Strings) {
                    if (s.isEmpty()) {
                        SendError(output, "Zadne pole nie moze byc puste!", response);
                        return;
                    }
                }

                if (data.Strings.get(1).equals(data.Strings.get(2))) {
                    String existsquery = "SELECT `id_uzytkownika` FROM `uzytkownicy` WHERE `login` = \""
                            + data.Strings.get(0) + "\" OR numer_telefonu = " + numtel + ";";
                    DatabaseHandler dbh = new DatabaseHandler();
                    if (dbh.conn == null || dbh.conn.isClosed()) {
                        SendError(output, "Blad polaczenia z baza danych.", response);
                        dbh.close();
                        return;
                    }
                    ResultSet existing = dbh.executeQuery(existsquery);
                    try {
                        if (existing.isClosed() || existing.next()) {
                            SendError(output,
                                    "Istnieje juz uzytkownik o takim loginie lub numerze telefonu, lub wystapil nieznany blad polaczenia z baza danych!",
                                    response);
                            return;
                        } else {
                            String registerQuery = "INSERT INTO uzytkownicy(`login`,`password`,`imie`,`nazwisko`,`data_utworzenia`,`numer_telefonu`,`typy_uzytkownikow_id_typu`) VALUES (\""
                                    + data.Strings.get(0).trim() + "\", \"" + data.Strings.get(1).trim() + "\", \""
                                    + data.Strings.get(3).trim() + "\", \"" + data.Strings.get(4).trim()
                                    + "\", NOW(), " + numtel + ", 1);";
                            int registerResult = dbh.executeUpdate(registerQuery);
                            if (registerResult > 0) {
                                response.operationType = NetData.OperationType.Success;
                                output.writeObject(response);
                                output.flush();
                                session.isSignedIn = true;
                                session.username = data.Strings.get(0);
                                dbh.close();
                                return;
                            } else {
                                SendError(output, "Nie udalo sie zarejestrowac.", response);
                                dbh.close();
                                return;
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            existing.close();
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        dbh.close();
                    }
                } else {
                    SendError(output, "Hasla nie sa identyczne!", response);
                    return;
                }
            } else {
                SendError(output, "Numer telefonu jest nieprawidlowy!", response);
                return;
            }
        } else {
            SendError(output, "Niepoprawne dane rejestracyjne.", response);
            return;
        }
    }

    private void handleLogin(NetData data, ObjectOutputStream output, User session) throws IOException, SQLException {
        NetData response = new NetData(NetData.Operation.Login);

        if (session.isSignedIn) {
            SendError(output, "Jestes juz zalogowany!", response);
            return;
        } else {
            if (data.Strings.size() == 2) {
                for (String s : data.Strings) {
                    if (s.isEmpty()) {
                        SendError(output, "Zadne pole nie moze byc puste!", response);
                        return;
                    }
                }

                String existsquery = "SELECT `id_uzytkownika` FROM `uzytkownicy` WHERE `login` = \""
                        + data.Strings.get(0) + "\" AND password = \"" + data.Strings.get(1) + "\";";
                DatabaseHandler dbh = new DatabaseHandler();
                if (dbh.conn == null || dbh.conn.isClosed()) {
                    SendError(output, "Blad polaczenia z baza danych.", response);
                    dbh.close();
                    return;
                }
                ResultSet existing = dbh.executeQuery(existsquery);
                try {
                    if (!existing.isClosed() && existing.next()) {
                        session.isSignedIn = true;
                        session.username = data.Strings.get(0);
                        response.operationType = NetData.OperationType.Success;
                        output.writeObject(response);
                        output.flush();
                        dbh.close();
                        return;
                    } else {
                        SendError(output, "Nie udalo sie zalogowac! Upewnij sie ze dane sa poprawne!", response);
                        dbh.close();
                        return;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        existing.close();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    dbh.close();
                }
            }
        }
    }

    private void handleOfferUsername(ObjectOutputStream output, User session) throws IOException {
        NetData response = new NetData(NetData.Operation.OfferUsername);
        response.Booleans.add(session.isSignedIn);
        if (session.isSignedIn) {
            response.Strings.add(session.username);
        } else {
            response.Strings.add("NIEZALOGOWANY");
        }
        output.writeObject(response);
        output.flush();
    }

    private void handleOfferElement(ObjectOutputStream output) throws IOException, SQLException {
        String query = "SELECT * FROM `auta` ORDER BY `cenaZaDzien` ASC;";
        DatabaseHandler dbh = new DatabaseHandler();
        if (dbh.conn == null || dbh.conn.isClosed()) {
            NetData response = new NetData(NetData.Operation.OfferElement);
            SendError(output, "Błąd połączenia z bazą danych.", response);
            dbh.close();
            return;
        }
        ResultSet result = dbh.executeQuery(query);
        while (result.next()) {
            System.out.println("SENDING AN OFFER.");
            NetData response = new NetData(NetData.Operation.OfferElement);
            int id = result.getInt("id_auta");
            String marka = result.getString("marka");
            String model = result.getString("model");
            String silnik = result.getString("silnik");
            int prod = result.getInt("rok_prod");
            float cena = result.getFloat("cenaZaDzien");
            String topText = marka + " " + model + " (" + prod + ") " + silnik;
            response.Strings.add(topText);
            response.Floats.add(cena);
            response.Integers.add(id);
            String zdjecie = result.getString("zdjecie");
            if (!zdjecie.isEmpty()) {
                byte[] img = loadImageAsBytes(zdjecie);
                System.out.println("IMG SIZE: " + img.length);
                response.Images.add(img);
            }
            output.writeObject(response);
            output.flush();
        }
        dbh.close();
    }


    private static byte[] loadImageAsBytes(String imagePath) {
        try {
            URL resourceUrl = Server.class.getResource(imagePath);
            if (resourceUrl != null) {
                try (InputStream stream = resourceUrl.openStream()) {
                    return stream.readAllBytes();
                }
            } else {
                // Handle the case where the resource is not found
                System.out.println("Resource not found: " + resourceUrl);
                return new byte[0];
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception (e.g., log it or return a default image)
            return new byte[0];
        }
    }

    private void SendError(ObjectOutputStream output, String error, NetData data) throws IOException {
        if (data == null || output == null)
            return;
        data.operationType = NetData.OperationType.Error;
        data.Strings.add(error);
        output.writeObject(data);
        output.flush();
    }

    void SendMessage(ObjectOutputStream output, String mes, NetData data) throws IOException {
        if (data == null || output == null)
            return;
        data.operationType = NetData.OperationType.MessageBox;
        data.Strings.add(mes);
        output.writeObject(data);
        output.flush();
    }

    public static void main(String[] args) {
        try {
            new Server().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class User implements Serializable {
    public boolean isSignedIn;
    public String username;
    public transient Socket clientSocket;
}
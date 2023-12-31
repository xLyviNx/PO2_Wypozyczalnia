package src;

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
            try {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleClient(clientSocket));
            }catch(Exception ex)
            {
                ex.printStackTrace();
            }
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
                            case OfferDetails:
                                handleOfferDetails(data, output);
                                break;
                            case Logout:
                            {
                                if (session.isSignedIn)
                                {
                                    try {
                                        session.isSignedIn = false;
                                        session.username = "";
                                        NetData response = new NetData(NetData.Operation.Logout);
                                        output.writeObject(response);
                                        output.flush();
                                    }catch(Exception ex)
                                    {
                                        ex.printStackTrace();
                                    }
                                }
                                else{
                                    NetData response = new NetData(NetData.Operation.Unspecified);
                                    SendError(output, "Nie jestes zalogowany!", response);
                                }
                                break;
                            }
                            case ReservationRequest:
                            {
                                handleReservation(data, session, output);
                                break;
                            }
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
    private boolean reservationExists (int id, ObjectOutputStream output, DatabaseHandler dbh) {
        String query = "SELECT * FROM wypozyczenie " +
                "WHERE id_wypozyczenia = " + id +
                " AND (data_wypozyczenia IS NULL OR (data_wypozyczenia IS NOT NULL AND CURRENT_DATE() BETWEEN data_wypozyczenia AND DATE_ADD(data_wypozyczenia, INTERVAL days DAY)))";
        try{
            ResultSet resultSet = dbh.executeQuery(query);
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (output!=null)
                    SendError(output, "Blad bazy danych!", new NetData(NetData.Operation.Unspecified));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            return true;
        }
    }
    private void handleReservation(NetData data, User session, ObjectOutputStream output) {
        if (session != null && session.isSignedIn) {
            DatabaseHandler dbh = new DatabaseHandler();
            if (!checkDBConnection(dbh, output)) {
                return;
            }
            if (!reservationExists(data.Integers.get(0), output, dbh)) {
                System.out.println("Rezerwacja nie istnieje");
                String query = "INSERT INTO wypozyczenie (`uzytkownicy_id_uzytkownika`, `auta_id_auta`, `days`)\n" +
                        "SELECT id_uzytkownika, " + data.Integers.get(0) + ", " + data.Integers.get(1) + " FROM uzytkownicy\n" +
                        "WHERE login = '" + session.username + "';";
                int res = dbh.executeUpdate(query);
                if (res<=0)
                {
                    try {
                        SendError(output, "Nie udalo sie zarezerwowac pojazdu. Sprobuj ponownie pozniej.", new NetData(NetData.Operation.ReservationRequest));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    dbh.close();
                }
                else {
                    NetData response = new NetData(NetData.Operation.ReservationRequest);
                    response.operationType = NetData.OperationType.Success;
                    dbh.close();
                    try {
                        output.writeObject(response);
                        output.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            } else {
                try {
                    SendError(output, "Rezerwacja istnieje, albo wystapil blad polaczenia z baza danych.", new NetData(NetData.Operation.ReservationRequest));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            NetData err = new NetData(NetData.Operation.Unspecified);
            err.operationType = NetData.OperationType.Error;
            err.Strings.add("Nie jestes zalogowany/a!");
        }
    }
    private void handleOfferDetails(NetData data, ObjectOutputStream output)
    {
        System.out.println(data.Integers.size());
        System.out.println(data.Integers.get(0));
        if (data.Integers.size() == 1 && data.Integers.get(0)>0)
        {
            DatabaseHandler dbh = new DatabaseHandler();
            int id = data.Integers.get(0);
            if (!checkDBConnection(dbh, output)) {
                return;
            }
            String query = "SELECT * FROM `auta` WHERE `id_auta` = " + id +";";
            ResultSet result = dbh.executeQuery(query);
            while (true) {
                try {
                    if (!result.next()) break;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("SENDING AN OFFER.");
                NetData response = new NetData(NetData.Operation.OfferDetails);
                try {
                    String marka = result.getString("marka");
                    String model = result.getString("model");
                    String silnik = result.getString("silnik");
                    int prod = result.getInt("rok_prod");
                    float cena = result.getFloat("cenaZaDzien");
                    String opis = result.getString("opis");
                    String header = marka + " " + model;
                    response.Strings.add(header);
                    String details = "Silnik: " + silnik + "\n" + "Rok produkcji: " + prod + "\nCena za dzień: " + String.format("%.2f zł", cena)+"\n\n"+opis;
                    response.Strings.add(details);
                    response.Floats.add(cena);
                    response.Integers.add(id);
                    String imagesString = result.getString("wiekszeZdjecia");
                    if (imagesString != null && !imagesString.isEmpty()) {
                        String[] images = imagesString.split(";");
                        for (String image : images) {
                            byte[] img = loadImageAsBytes(image);
                            System.out.println("IMG SIZE: " + img.length);
                            if (img.length > 0)
                            {
                                response.Images.add(img);
                            }
                        }
                    }
                    output.writeObject(response);
                    output.flush();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
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
                    if (!checkDBConnection(dbh, output)) {
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
                if (!checkDBConnection(dbh, output)) {
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

    private void handleOfferElement(ObjectOutputStream output)  {
        String query = "SELECT a.`id_auta`, a.`marka`, a.`model`, a.`rok_prod`, a.`silnik`, a.`zdjecie`, a.`opis`, a.`cenaZaDzien` " +
                "FROM `auta` a " +
                "LEFT JOIN `wypozyczenie` w ON a.`id_auta` = w.`auta_id_auta` " +
                "WHERE w.`id_wypozyczenia` IS NULL " +
                "   OR (w.`data_wypozyczenia` IS NOT NULL " +
                "       AND NOT (CURRENT_DATE() BETWEEN w.`data_wypozyczenia` AND DATE_ADD(w.`data_wypozyczenia`, INTERVAL w.`days` DAY))) " +
                "ORDER BY a.`cenaZaDzien` ASC;";
        DatabaseHandler dbh = new DatabaseHandler();
        if (!checkDBConnection(dbh, output)) {
            return;
        }
        ResultSet result = dbh.executeQuery(query);
        try {
            while (result.next()) {
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
                try {
                    String zdjecie = result.getString("zdjecie");
                    if (zdjecie != null && !zdjecie.isEmpty()) {
                        byte[] img = loadImageAsBytes(zdjecie);
                        System.out.println("IMG SIZE: " + img.length);
                        response.Images.add(img);
                    }
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
                output.writeObject(response);
                output.flush();
                System.out.println("SENDING AN OFFER.");
                /*try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }*/
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        dbh.close();
    }


    private static byte[] loadImageAsBytes(String imagePath) {
        try {
            URL resourceUrl = Server.class.getResource("/img/"+imagePath);
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
    boolean checkDBConnection(DatabaseHandler dbh, ObjectOutputStream output)
    {
        NetData response = new NetData(NetData.Operation.Unspecified);
        try {
            if (dbh.conn == null || dbh.conn.isClosed()) {
                if (output!=null)
                    SendError(output, "Błąd połączenia z bazą danych.", response);
                dbh.close();
                return false;
            }
        } catch (SQLException | IOException e) {
            if (output!=null) {
                try {
                    SendError(output, "Błąd połączenia z bazą danych.", response);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            return false;
        }
        return true;
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
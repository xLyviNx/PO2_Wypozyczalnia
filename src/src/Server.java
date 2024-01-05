package src;

import javafx.scene.Scene;
import jdk.jshell.execution.Util;

import javax.imageio.ImageIO;
import javax.xml.transform.Result;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private volatile boolean closing = false;
    private ExecutorService executor;
    public ArrayList<User> connectedClients = new ArrayList<>();
    private ServerSocket serverSocket;

    public void start(String ip, int port) throws IOException {
        serverSocket = new ServerSocket(port);
        executor = Executors.newCachedThreadPool();
        Thread console = new Thread(()->handleConsoleCommands(serverSocket));
        console.setDaemon(true);
        console.start();
        while (true) {
            try {
                if (!serverSocket.isClosed() && !closing) {
                    Socket clientSocket = serverSocket.accept();
                    executor.submit(() -> handleClient(clientSocket));
                }
            } catch (Exception ex) {
                if (!closing) {
                    ex.printStackTrace();
                }
            }
            if (closing)
                break;
        }
        return;
    }

    private void handleConsoleCommands(ServerSocket serverSocket) {
        System.out.println("Konsola jest aktywna.");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String command = scanner.nextLine();
            switch (command.toLowerCase()) {
                case "test":
                    System.out.println("TEST");
                    break;
                case "exit":
                case "stop":
                    System.out.println("Zatrzymywanie serwera...");
                    try {
                        closing = true;
                        executor.shutdown();
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                default:
                    System.out.println("Nieznana komenda.");
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
                                handleOfferElement(output,session);
                                break;
                            case OfferDetails:
                                handleOfferDetails(data, session, output);
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
                            case AddOffer:
                            {
                                try {
                                    handleAddOffer(data, session, output);
                                }catch(Exception ex)
                                {
                                    ex.printStackTrace();
                                }
                                break;
                            }
                            case DeleteOffer:
                            {
                                handleDeleteOffer(data,session,output);
                                break;
                            }
                            case RequestConfirmtations:
                            {
                                try {
                                    handleSendConfirmations(data, session, output);
                                } catch (IOException e) {
                                    SendError(output, "Błąd IO.\n" + e.getLocalizedMessage(), new NetData(NetData.Operation.Unspecified));
                                    throw new RuntimeException(e);
                                } catch (SQLException e) {
                                    SendError(output, "Błąd Bazy Danych.", new NetData(NetData.Operation.Unspecified));
                                    throw new RuntimeException(e);
                                }
                                break;
                            }
                            case CancelReservation:
                            {
                                handleCancelReservation(data,session,output);
                                break;
                            }
                            case ConfirmReservation:
                            {
                                try {
                                    handleConfirmReservation(data,session,output);
                                } catch (IOException e) {
                                    SendError(output, "Błąd IO.\n" + e.getLocalizedMessage(), new NetData(NetData.Operation.Unspecified));
                                    throw new RuntimeException(e);
                                } catch (SQLException e) {
                                    SendError(output, "Błąd Bazy Danych.", new NetData(NetData.Operation.Unspecified));
                                    throw new RuntimeException(e);
                                }
                                break;
                            }
                            case ConfirmationsButton:
                            {
                                handleConfirmButton(data,session,output);
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

    private void handleConfirmButton(NetData data, User session, ObjectOutputStream output) {
        NetData res = new NetData(NetData.Operation.ConfirmationsButton);
        if (session.isSignedIn && !session.username.isEmpty() && session.canManageReservations) {
            res.Booleans.add(true);
        } else {
            res.Booleans.add(false);
        }
        try {
            output.writeObject(res);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleConfirmReservation(NetData data, User session, ObjectOutputStream output) throws IOException, SQLException {
        NetData err = new NetData(NetData.Operation.Unspecified);
        if (!session.isSignedIn || session.username.isEmpty()) {
            SendError(output, "Nie jesteś zalogowany/a!", err);
            return;
        }
        if (!session.canManageReservations) {
            SendError(output, "Nie masz uprawnień do usuwania ofert!", err);
            return;
        }
        if (data.Integers.size()==1) {
            DatabaseHandler dbh = new DatabaseHandler();
            if (!checkDBConnection(dbh, output)) {
                return;
            }
            int id = data.Integers.get(0);
            String query = "UPDATE wypozyczenie SET data_wypozyczenia = NOW() WHERE id_wypozyczenia = ?";
            PreparedStatement preparedStatement = dbh.conn.prepareStatement(query);
            preparedStatement.setInt(1, id);
            int updated = preparedStatement.executeUpdate();
            if (updated > 0) {
                NetData response = new NetData(NetData.Operation.ConfirmReservation);
                response.operationType = NetData.OperationType.Success;
                output.writeObject(response);
                output.flush();
            } else {
                SendError(output, "Błąd potwierdzania rezerwacji!", err);
            }
        }
        else{
            SendError(output, "Błąd przetwarzania żądania!", err);
        }
    }

    private void handleCancelReservation(NetData data, User session, ObjectOutputStream output) throws IOException {
        NetData err = new NetData(NetData.Operation.Unspecified);
        if (!session.isSignedIn || session.username.isEmpty()) {
            SendError(output, "Nie jesteś zalogowany/a!", err);
            return;
        }
        if (!session.canManageReservations) {
            SendError(output, "Nie masz uprawnień do usuwania ofert!", err);
            return;
        }
        if (data.Integers.size() == 1) {
            DatabaseHandler dbh = new DatabaseHandler();
            if (!checkDBConnection(dbh, output)) {
                return;
            }
            int id = data.Integers.get(0);
            String query = "DELETE FROM wypozyczenie WHERE id_wypozyczenia = ?";
            try (PreparedStatement deleteStatement = dbh.conn.prepareStatement(query)) {
                deleteStatement.setInt(1, id);
                int res = deleteStatement.executeUpdate();
                if (res > 0) {
                    NetData response = new NetData(NetData.Operation.CancelReservation);
                    response.operationType = NetData.OperationType.Success;
                    output.writeObject(response);
                    output.flush();
                } else {
                    SendError(output, "Błąd usuwania rezerwacji!", err);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        }else {
            SendError(output, "Błąd przetwarzania żądania!", err);
        }
    }

    private void handleSendConfirmations(NetData data, User session, ObjectOutputStream output) throws IOException, SQLException {
        NetData err = new NetData(NetData.Operation.Unspecified);
        if (!session.isSignedIn || session.username.isEmpty()) {
            SendError(output, "Nie jesteś zalogowany/a!", err);
            return;
        }
        if (!session.canManageReservations) {
            SendError(output, "Nie masz uprawnień do usuwania ofert!", err);
            return;
        }
        DatabaseHandler dbh = new DatabaseHandler();
        if (!checkDBConnection(dbh, output))
        {
            return;
        }
        String query = "SELECT\n" +
                "    wyp.id_wypozyczenia,\n" +
                "    wyp.days,\n" +
                "    car.marka,\n" +
                "    car.model,\n" +
                "    car.rok_prod,\n" +
                "    car.id_auta,\n" +
                "    car.cenaZaDzien,\n" +
                "    uzy.login,\n" +
                "    uzy.imie,\n" +
                "    uzy.nazwisko,\n" +
                "    uzy.numer_telefonu\n" +
                "FROM\n" +
                "    wypozyczenie wyp\n" +
                "INNER JOIN\n" +
                "    auta car ON wyp.auta_id_auta = car.id_auta\n" +
                "INNER JOIN\n" +
                "    uzytkownicy uzy ON wyp.uzytkownicy_id_uzytkownika = uzy.id_uzytkownika\n" +
                "WHERE\n" +
                "    wyp.data_wypozyczenia IS NULL;";

        ResultSet results = dbh.executeQuery(query);
        while (results != null && results.next())
        {
            NetData reservation = new NetData(NetData.Operation.ReservationElement);
            int reserveId = results.getInt("id_wypozyczenia");
            int reserveDays = results.getInt("days");
            String marka = results.getString("marka");
            String model = results.getString("model");
            int rokProd = results.getInt("rok_prod");
            int idAuta = results.getInt("id_auta");
            float cenaZaDzien = results.getFloat("cenaZaDzien");
            String login = results.getString("login");
            String imie = results.getString("imie");
            String nazwisko = results.getString("nazwisko");
            int numerTelefonu = results.getInt("numer_telefonu");

            reservation.Integers.add(reserveId);
            reservation.Integers.add(reserveDays);
            reservation.Strings.add(marka);
            reservation.Strings.add(model);
            reservation.Integers.add(rokProd);
            reservation.Integers.add(idAuta);
            reservation.Floats.add(cenaZaDzien);
            reservation.Strings.add(login);
            reservation.Strings.add(imie);
            reservation.Strings.add(nazwisko);
            reservation.Integers.add(numerTelefonu);

            output.writeObject(reservation);
            output.flush();
        }
    }

    private void handleDeleteOffer(NetData data, User session, ObjectOutputStream output) throws IOException {
        NetData res = new NetData(NetData.Operation.DeleteOffer);
        if (!session.isSignedIn || session.username.isEmpty()) {
            SendError(output, "Nie jesteś zalogowany/a!", res);
            return;
        }
        if (!session.canDeleteOffers) {
            SendError(output, "Nie masz uprawnień do usuwania ofert!", res);
            return;
        }

        if (data.Integers.size() == 1) {
            int offerIdToDelete = data.Integers.get(0);

            try {
                DatabaseHandler dbh = new DatabaseHandler();
                if (!checkDBConnection(dbh, output)) {
                    return;
                }

                String query = "SELECT zdjecie, wiekszeZdjecia FROM auta WHERE id_auta = ?";
                try (PreparedStatement selectStatement = dbh.conn.prepareStatement(query)) {
                    selectStatement.setInt(1, offerIdToDelete);
                    ResultSet resultSet = selectStatement.executeQuery();

                    if (resultSet.next()) {
                        String thumbnail = resultSet.getString("zdjecie");
                        String[] photosIndividual = resultSet.getString("wiekszeZdjecia").split(";");

                        query = "DELETE FROM auta WHERE id_auta = ?";
                        try (PreparedStatement deleteStatement = dbh.conn.prepareStatement(query)) {
                            deleteStatement.setInt(1, offerIdToDelete);
                            int rowsDeleted = deleteStatement.executeUpdate();

                            if (rowsDeleted > 0) {
                                String folderPath = Main.imagePath;
                                if (!thumbnail.isEmpty()) {
                                    String thumbnailPath = folderPath + thumbnail;
                                    Path thumbnailFilePath = Paths.get(thumbnailPath);
                                    try {
                                        Files.delete(thumbnailFilePath);
                                        System.out.println("Thumbnail deleted successfully.");
                                    } catch (IOException e) {
                                        System.err.println("Unable to delete the thumbnail: " + e.getMessage());
                                    }
                                }

                                for (String photo : photosIndividual) {
                                    if (!photo.isEmpty()) {

                                        String photoPath = folderPath + photo;
                                        Path photoFilePath = Paths.get(photoPath);
                                        try {
                                            Files.delete(photoFilePath);
                                            System.out.println("Photo deleted successfully.");
                                        } catch (IOException e) {
                                            System.err.println("Unable to delete the photo: " + e.getMessage());
                                        }
                                    }
                                }
                                NetData successRes = new NetData(NetData.Operation.DeleteOffer);
                                successRes.operationType = NetData.OperationType.Success;
                                output.writeObject(successRes);
                                output.flush();
                                return;
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            SendError(output, "Nie można usunąć tej oferty lub oferta nie istnieje!", res);
        } else {
            SendError(output, "Nieprawidłowe dane przesłane do usuwania oferty!", res);
        }
    }


    private void handleAddOffer(NetData data, User session, ObjectOutputStream output) throws IOException {
        NetData res = new NetData(NetData.Operation.AddOffer);
        if (!session.isSignedIn || session.username.isEmpty())
        {
            SendError(output, "Nie jesteś zalogowany/a!", res);
            return;
        }
        if (!session.canAddOffers)
        {
            SendError(output, "Nie masz uprawnień do dodawania nowych ofert!", res);
            return;
        }
        if (data.Strings.size() == 6)
        {
            if (data.Integers.size()==1 && data.Floats.size() == 1)
            {
                String thumbname = "user/" + session.username + "/" + data.Strings.get(4);
                if (thumbname.length()>64)
                {
                    SendError(output, "Przekroczono maksymalna dlugosc znakow w miniaturce, sprobuj skrocic nazwe pliku!", res);
                    return;
                }
                URL thumburl = Server.class.getResource("/img/"+thumbname);
                //System.err.println("URL: " + thumburl);
                if (Utilities.fileExists(thumburl))
                {
                    SendError(output, "Plik miniaturki o danej nazwie już istnieje!", res);
                    return;
                }
                String dbPhotos = "";
                String[] photosIndividual = null;

                if (data.Strings.get(5) != null) {
                    photosIndividual = data.Strings.get(5).split(";");
                    for (String photo : photosIndividual) {
                        String photoname = "user/" + session.username + "/" + photo;
                        URL resourceUrl = Server.class.getResource("/img/" + photoname);
                        if (Utilities.fileExists(resourceUrl)) {
                            SendError(output, "Conajmniej jeden z przesłanych plików już istnieje!", res);
                            return;
                        }
                        dbPhotos += photoname + ";";
                    }

                    dbPhotos = dbPhotos.trim();
                    if (dbPhotos.endsWith(";")) {
                        dbPhotos = dbPhotos.substring(0, dbPhotos.length() - 1);
                    }
                    if (dbPhotos.length() > 256) {
                        SendError(output, "Przekroczono maksymalna dlugosc znakow w zdjeciach, sprobuj skrocic nazwy!", res);
                        return;
                    }
                }
                DatabaseHandler dbh = new DatabaseHandler();
                if (!checkDBConnection(dbh, output))
                {
                    return;
                }
                String query = "INSERT INTO auta (marka, model, rok_prod, silnik, zdjecie, opis, cenaZaDzien, wiekszeZdjecia) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement preparedStatement = dbh.conn.prepareStatement(query)) {
                    preparedStatement.setString(1, data.Strings.get(0).trim());
                    preparedStatement.setString(2, data.Strings.get(1).trim());
                    preparedStatement.setInt(3, data.Integers.get(0));
                    preparedStatement.setString(4, data.Strings.get(2).trim());
                    preparedStatement.setString(5, thumbname);
                    preparedStatement.setString(6, data.Strings.get(3).trim());
                    preparedStatement.setFloat(7, data.Floats.get(0));
                    preparedStatement.setString(8, dbPhotos);
                    int queryres = preparedStatement.executeUpdate();
                    if (queryres>0)
                    {
                        if (!data.Images.isEmpty()) {
                            byte[] thumb = data.Images.get(0);
                            data.Images.remove(0);
                            if (thumb.length > 0)
                            {
                                try {
                                    BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(thumb));

                                    String folderPath = Main.imagePath + File.separator + "user/" + session.username;
                                        Path folder = Paths.get(folderPath);
                                        if (!Files.exists(folder)) {
                                            // Katalog nie istnieje, więc próbujemy go utworzyć
                                            try {
                                                Files.createDirectories(folder);
                                                System.out.println("Katalog został utworzony pomyślnie.");
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                                System.err.println("Błąd podczas tworzenia katalogu.");
                                            }
                                        }
                                        String imgPath = folderPath + File.separator + data.Strings.get(4).trim();

                                        try {
                                            ImageIO.write(bufferedImage, "jpg", new File(imgPath));
                                            System.out.println("Obraz został zapisany pomyślnie.");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            System.err.println("Błąd podczas zapisywania obrazu.");
                                        }

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (photosIndividual!=null && data.Images.size() == photosIndividual.length) {
                                for (int i = 0; i < photosIndividual.length; i++) {
                                    String photoname = "user/" + session.username + "/" + photosIndividual[i];
                                    byte[] img = data.Images.get(i);
                                    if (img.length > 0) {
                                        try {
                                            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(img));
                                            String folderPath = Main.imagePath + File.separator + "user/" + session.username;

                                            Path folder = Paths.get(folderPath);
                                            if (!Files.exists(folder)) {
                                                try {
                                                    Files.createDirectories(folder);
                                                    System.out.println("Katalog został utworzony pomyślnie.");
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                    System.err.println("Błąd podczas tworzenia katalogu.");
                                                }
                                            }

                                            String imgPath = folderPath + File.separator + photosIndividual[i];

                                            ImageIO.write(bufferedImage, "jpg", new File(imgPath));
                                            System.out.println("Obraz został zapisany pomyślnie.");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            System.err.println("Błąd podczas zapisywania obrazu.");
                                            SendError(output, "Błąd podczas zapisywania obrazu przez serwer.", res);
                                        }
                                    }
                                }
                            } else {
                                System.out.println("Rozmiar zdjęć nieprawidłowy");
                            }
                        }
                        NetData newRes = new NetData(NetData.Operation.AddOffer);
                        newRes.operationType= NetData.OperationType.Success;
                        output.writeObject(newRes);
                        output.flush();
                    }
                    else
                    {
                        SendError(output,"Wystapil problem z dodawaniem ogloszenia.", res);
                    }
                } catch (SQLException e) {
                    SendError(output,"Wystapil problem polaczenia z baza danych.", res);
                    e.printStackTrace();
                }

            }
        }
    }

    private void fetchUserPermissions(User session)
    {
        if (session.isSignedIn)
        {
            DatabaseHandler dbh = new DatabaseHandler();
            if (!checkDBConnection(dbh, null))
                return;
            String query = "SELECT typ.`dodajogloszenia`, typ.`wypozyczauto`, typ.`usunogloszenie`, typ.`manageReservations` FROM typy_uzytkownikow typ INNER JOIN uzytkownicy uz ON typ.id_typu = uz.typy_uzytkownikow_id_typu WHERE uz.login = ?";
            try (PreparedStatement selectStatement = dbh.conn.prepareStatement(query)) {
                selectStatement.setString(1, session.username);
                ResultSet rsS = selectStatement.executeQuery();
                while (rsS.next())
                {
                    session.canReserve = rsS.getBoolean("wypozyczauto");
                    session.canAddOffers = rsS.getBoolean("dodajogloszenia");
                    session.canDeleteOffers = rsS.getBoolean("usunogloszenie");
                    session.canManageReservations = rsS.getBoolean("manageReservations");
                    break;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
    private boolean reservationExists(int id, ObjectOutputStream output, DatabaseHandler dbh) {
        String query = "SELECT * FROM wypozyczenie " +
                "WHERE id_wypozyczenia = ? AND (data_wypozyczenia IS NULL OR (data_wypozyczenia IS NOT NULL AND NOW() BETWEEN data_wypozyczenia AND DATE_ADD(data_wypozyczenia, INTERVAL days DAY)))";

        try (PreparedStatement preparedStatement = dbh.conn.prepareStatement(query)) {
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            SendError(output, "Blad bazy danych!", new NetData(NetData.Operation.Unspecified));
            return true;
        }
    }

    private void handleReservation(NetData data, User session, ObjectOutputStream output) {
        if (session != null && session.isSignedIn) {
            if (!session.canReserve) {
                SendError(output, "Nie masz uprawnień do rezerwacji.", new NetData(NetData.Operation.ReservationRequest));
                return;
            }

            DatabaseHandler dbh = new DatabaseHandler();
            if (!checkDBConnection(dbh, output)) {
                return;
            }

            if (!reservationExists(data.Integers.get(0), output, dbh)) {
                System.out.println("Rezerwacja nie istnieje");

                String query = "INSERT INTO wypozyczenie (`uzytkownicy_id_uzytkownika`, `auta_id_auta`, `days`)" +
                        " SELECT id_uzytkownika, ?, ? FROM uzytkownicy" +
                        " WHERE login = ?";

                try (PreparedStatement insertStatement = dbh.conn.prepareStatement(query)) {
                    insertStatement.setInt(1, data.Integers.get(0));
                    insertStatement.setInt(2, data.Integers.get(1));
                    insertStatement.setString(3, session.username);

                    int res = insertStatement.executeUpdate();

                    if (res <= 0) {
                        SendError(output, "Nie udało się zarezerwować pojazdu. Spróbuj ponownie później.", new NetData(NetData.Operation.ReservationRequest));
                    } else {
                        NetData response = new NetData(NetData.Operation.ReservationRequest);
                        response.operationType = NetData.OperationType.Success;
                        try {
                            output.writeObject(response);
                            output.flush();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    SendError(output, "Błąd bazy danych!", new NetData(NetData.Operation.ReservationRequest));
                } finally {
                    dbh.close();
                }
            } else {
                SendError(output, "Rezerwacja istnieje, albo wystąpił błąd połączenia z bazą danych.", new NetData(NetData.Operation.ReservationRequest));
            }
        } else {
            NetData err = new NetData(NetData.Operation.Unspecified);
            err.operationType = NetData.OperationType.Error;
            err.Strings.add("Nie jesteś zalogowany/a!");
        }
    }
    private void handleOfferDetails(NetData data, User session, ObjectOutputStream output) {
        if (data.Integers.size() == 1 && data.Integers.get(0) > 0) {
            DatabaseHandler dbh = new DatabaseHandler();
            int id = data.Integers.get(0);

            if (!checkDBConnection(dbh, output)) {
                return;
            }

            String query = "SELECT * FROM `auta` WHERE `id_auta` = ?";

            try (PreparedStatement statement = dbh.conn.prepareStatement(query)) {
                statement.setInt(1, id);

                ResultSet result = statement.executeQuery();

                if (result.next()) {
                    NetData response = new NetData(NetData.Operation.OfferDetails);

                    try {
                        String marka = result.getString("marka");
                        String model = result.getString("model");
                        String silnik = result.getString("silnik");
                        int rokProdukcji = result.getInt("rok_prod");
                        float cenaZaDzien = result.getFloat("cenaZaDzien");
                        String opis = result.getString("opis");

                        response.Strings.add(marka + " " + model);
                        response.Strings.add("Silnik: " + silnik + "\nRok produkcji: " + rokProdukcji + "\nCena za dzień: " + String.format("%.2f zł", cenaZaDzien) + "\n\n" + opis);
                        response.Floats.add(cenaZaDzien);
                        response.Integers.add(id);
                        response.Booleans.add(session.isSignedIn && !session.username.isEmpty() && session.canDeleteOffers);

                        String imagesString = result.getString("wiekszeZdjecia");
                        if (imagesString != null && !imagesString.isEmpty()) {
                            String[] images = imagesString.split(";");
                            for (String image : images) {
                                byte[] img = Utilities.loadImageAsBytes(image, false);
                                if (img.length > 0) {
                                    response.Images.add(img);
                                }
                            }
                        }

                        output.writeObject(response);
                        output.flush();
                    } catch (SQLException | IOException e) {
                        e.printStackTrace();
                        SendError(output, "Błąd przetwarzania wyników zapytania.", new NetData(NetData.Operation.OfferDetails));
                    }
                } else {
                    SendError(output, "Brak oferty o podanym identyfikatorze.", new NetData(NetData.Operation.OfferDetails));
                }
            } catch (SQLException e) {
                e.printStackTrace();
                SendError(output, "Błąd bazy danych!", new NetData(NetData.Operation.OfferDetails));
            } finally {
                dbh.close();
            }
        } else {
            SendError(output, "Nieprawidłowe dane zapytania.", new NetData(NetData.Operation.OfferDetails));
        }
    }


    private void handleRegister(NetData data, ObjectOutputStream output, User session) throws IOException, SQLException {
        NetData response = new NetData(NetData.Operation.Register);
        if (session.isSignedIn) {
            SendError(output, "Jesteś już zalogowany!", response);
            return;
        }
        if (data.Strings.size() == 5 && data.Integers.size() == 1) {
            int numtel = data.Integers.get(0);
            if (String.valueOf(numtel).length() == 9) {
                for (String s : data.Strings) {
                    if (s.isEmpty()) {
                        SendError(output, "Żadne pole nie może być puste!", response);
                        return;
                    }
                }
                if (data.Strings.get(1).equals(data.Strings.get(2))) {
                    String login = data.Strings.get(0);
                    String password = data.Strings.get(1); // Uwaga: Haszuj hasło przed zapisaniem!
                    String existsQuery = "SELECT `id_uzytkownika` FROM `uzytkownicy` WHERE `login` = ? OR numer_telefonu = ?";
                    DatabaseHandler dbh = new DatabaseHandler();

                    try {
                        if (!checkDBConnection(dbh, output)) {
                            return;
                        }

                        try (PreparedStatement existsStatement = dbh.conn.prepareStatement(existsQuery)) {
                            existsStatement.setString(1, login);
                            existsStatement.setInt(2, numtel);

                            ResultSet existing = existsStatement.executeQuery();

                            if (existing.next()) {
                                SendError(output, "Użytkownik o podanym loginie lub numerze telefonu już istnieje.", response);
                                return;
                            }
                        }

                        String registerQuery = "INSERT INTO uzytkownicy(`login`,`password`,`imie`,`nazwisko`,`data_utworzenia`,`numer_telefonu`,`typy_uzytkownikow_id_typu`) VALUES (?, ?, ?, ?, NOW(), ?, 1)";
                        try (PreparedStatement registerStatement = dbh.conn.prepareStatement(registerQuery)) {
                            registerStatement.setString(1, login);
                            registerStatement.setString(2, password); // Uwaga: Haszuj hasło przed ustawieniem!
                            registerStatement.setString(3, data.Strings.get(3).trim());
                            registerStatement.setString(4, data.Strings.get(4).trim());
                            registerStatement.setInt(5, numtel);

                            int registerResult = registerStatement.executeUpdate();

                            if (registerResult > 0) {
                                response.operationType = NetData.OperationType.Success;
                                output.writeObject(response);
                                output.flush();
                                session.isSignedIn = true;
                                session.username = login;
                                fetchUserPermissions(session);
                            } else {
                                SendError(output, "Nie udało się zarejestrować.", response);
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        SendError(output, "Błąd bazy danych!", response);
                    } finally {
                        dbh.close();
                    }
                } else {
                    SendError(output, "Podane hasła nie są identyczne!", response);
                }
            } else {
                SendError(output, "Numer telefonu jest nieprawidłowy!", response);
            }
        } else {
            SendError(output, "Niepoprawne dane rejestracyjne.", response);
        }
    }

    private void handleLogin(NetData data, ObjectOutputStream output, User session) throws IOException, SQLException {
        NetData response = new NetData(NetData.Operation.Login);

        if (session.isSignedIn) {
            SendError(output, "Jesteś już zalogowany!", response);
            return;
        }

        if (data.Strings.size() == 2) {
            for (String s : data.Strings) {
                if (s.isEmpty()) {
                    SendError(output, "Żadne pole nie może być puste!", response);
                    return;
                }
            }

            String existsQuery = "SELECT `id_uzytkownika` FROM `uzytkownicy` WHERE BINARY `login` = ? AND BINARY password = ?";
            DatabaseHandler dbh = new DatabaseHandler();

            try {
                if (!checkDBConnection(dbh, output)) {
                    return;
                }

                try (PreparedStatement existsStatement = dbh.conn.prepareStatement(existsQuery)) {
                    existsStatement.setString(1, data.Strings.get(0));
                    existsStatement.setString(2, data.Strings.get(1));

                    ResultSet existing = existsStatement.executeQuery();

                    if (existing.next()) {
                        session.isSignedIn = true;
                        session.username = data.Strings.get(0);
                        fetchUserPermissions(session);
                        response.operationType = NetData.OperationType.Success;
                        output.writeObject(response);
                        output.flush();
                    } else {
                        SendError(output, "Nie udało się zalogować! Upewnij się, że dane są poprawne.", response);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                SendError(output, "Błąd bazy danych!", response);
            } finally {
                dbh.close();
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

    private void handleOfferElement(ObjectOutputStream output, User session) {
        try {
            NetData addBr = new NetData(NetData.Operation.addButton);
            addBr.Booleans.add(session.canAddOffers);
            output.writeObject(addBr);
            output.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DatabaseHandler dbh = new DatabaseHandler();
        if (!checkDBConnection(dbh, output)) {
            return;
        }
        String loginUzytkownika = session.username;
        String query = "SELECT " +
                "    a.`id_auta`, a.`marka`, a.`model`, a.`rok_prod`, a.`silnik`, a.`zdjecie`, a.`opis`, a.`cenaZaDzien`, " +
                "    w.`data_wypozyczenia`, w.`days`, w.`id_wypozyczenia`," +
                "    ABS(DATEDIFF(NOW(), IFNULL(w.`data_wypozyczenia`, NOW()) + INTERVAL IFNULL(w.`days`, 0) DAY)) AS dni_pozostale " +
                "FROM " +
                "    `auta` a " +
                "    LEFT JOIN `wypozyczenie` w ON a.`id_auta` = w.`auta_id_auta` " +
                "    LEFT JOIN `uzytkownicy` u ON w.`uzytkownicy_id_uzytkownika` = u.`id_uzytkownika` " +
                "WHERE " +
                "    ( " +
                "        w.`id_wypozyczenia` IS NULL " +
                "        OR ( " +
                "            w.`data_wypozyczenia` IS NOT NULL " +
                "            AND NOT (NOW() BETWEEN w.`data_wypozyczenia` AND DATE_ADD(w.`data_wypozyczenia`, INTERVAL w.`days` DAY)) " +
                "        ) " +
                "        OR ( " +
                "            u.`id_uzytkownika` IS NOT NULL " +
                "            AND u.`login` = ? " +
                "            AND (NOW() BETWEEN IFNULL(w.`data_wypozyczenia`, NOW()) AND DATE_ADD(IFNULL(w.`data_wypozyczenia`, NOW()), INTERVAL IFNULL(w.`days`, 0) DAY)) " +
                "        ) " +
                "        OR ( " +
                "            w.`data_wypozyczenia` IS NULL " +
                "            AND u.`login` = ? " +
                "        ) " +
                "    ) " +
                "ORDER BY " +
                "    a.`cenaZaDzien` ASC;";
        try {
            PreparedStatement preparedStatement = dbh.conn.prepareStatement(query);
            preparedStatement.setString(1, loginUzytkownika);
            preparedStatement.setString(2, loginUzytkownika);

            ResultSet result = preparedStatement.executeQuery();

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

                int idWypo = result.getInt("id_wypozyczenia");
                if (result.wasNull())
                {
                    response.Booleans.add(false);
                    response.Integers.add(0);
                }
                else
                {
                    Date dataWypo = result.getDate("data_wypozyczenia");
                    if (result.wasNull()) {
                        response.Booleans.add(false);
                        response.Integers.add(-1);
                    } else {
                        int daysLeft = result.getInt("dni_pozostale");
                        if (result.wasNull()) {
                            System.out.println("DNI NULL");
                            response.Booleans.add(false);
                            response.Integers.add(0);
                        } else {
                            System.out.println("DNI EXIST");
                            response.Booleans.add(true);
                            response.Integers.add(daysLeft);
                        }
                    }
                }
                try {
                    String zdjecie = result.getString("zdjecie");
                    if (zdjecie != null && !zdjecie.isEmpty()) {
                        byte[] img = Utilities.loadImageAsBytes(zdjecie, false);
                        //System.out.println("IMG SIZE: " + img.length);
                        response.Images.add(img);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                output.writeObject(response);
                output.flush();
                //System.out.println("SENDING AN OFFER.");
                /*try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }*/
            }
        } catch (SQLException e) {
            e.printStackTrace();
            SendError(output, "Błąd bazy danych.", new NetData(NetData.Operation.Unspecified));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        dbh.close();
    }
    private void SendError(ObjectOutputStream output, String error, NetData data) {
        if (data == null || output == null)
            return;
        data.operationType = NetData.OperationType.Error;
        data.Strings.add(error);
        try {
            output.writeObject(data);
            output.flush();
        } catch (IOException e) {
            System.err.println("Nie udalo sie wyslac komunikatu bledu.");
        }
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
        } catch (SQLException e) {
            if (output!=null) {
                SendError(output, "Błąd połączenia z bazą danych.", response);
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
}

class User {
    public boolean isSignedIn;
    public String username;
    public transient Socket clientSocket;
    boolean canAddOffers = false;
    boolean canDeleteOffers = false;
    boolean canReserve = false;
    boolean canManageReservations = false;
}
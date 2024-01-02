package src;

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
        if (data.Integers.size()==1)
        {
            DatabaseHandler dbh = new DatabaseHandler();
            if (!checkDBConnection(dbh, output))
            {
                return;
            }
            int id = data.Integers.get(0);
            String query = "DELETE FROM wypozyczenie WHERE id_wypozyczenia = " + id;
            int res = dbh.executeUpdate(query);
            if (res>0)
            {
                NetData response = new NetData(NetData.Operation.CancelReservation);
                response.operationType= NetData.OperationType.Success;
                output.writeObject(response);
                output.flush();
            }
            else{
                SendError(output, "Błąd usuwania rezerwacji!", err);
            }
        }
        else{
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

                // Pobierz informacje o zdjęciach i miniaturkach związanych z ofertą
                String query = "SELECT zdjecie, wiekszeZdjecia FROM auta WHERE id_auta = ?";
                try (PreparedStatement selectStatement = dbh.conn.prepareStatement(query)) {
                    selectStatement.setInt(1, offerIdToDelete);
                    ResultSet resultSet = selectStatement.executeQuery();

                    if (resultSet.next()) {
                        String thumbnail = resultSet.getString("zdjecie");
                        String[] photosIndividual = resultSet.getString("wiekszeZdjecia").split(";");

                        // Usuń ofertę z bazy danych
                        query = "DELETE FROM auta WHERE id_auta = ?";
                        try (PreparedStatement deleteStatement = dbh.conn.prepareStatement(query)) {
                            deleteStatement.setInt(1, offerIdToDelete);
                            int rowsDeleted = deleteStatement.executeUpdate();

                            if (rowsDeleted > 0) {

                                ClassLoader classLoader = Server.class.getClassLoader();
                                URL resourceUrl = classLoader.getResource("");
                                String classpath = new File(resourceUrl.getFile()).getPath();
                                String folderPath = classpath + File.separator + "img";

                                if (!thumbnail.isEmpty() && resourceUrl != null) {
                                    String thumbnailPath = folderPath + File.separator + thumbnail;
                                    Path thumbnailFilePath = Paths.get(thumbnailPath);
                                    try {
                                        Files.delete(thumbnailFilePath);
                                        System.out.println("Thumbnail deleted successfully.");
                                    } catch (IOException e) {
                                        // Handle the exception if the file deletion fails
                                        System.err.println("Unable to delete the thumbnail: " + e.getMessage());
                                    }
                                }

                                for (String photo : photosIndividual) {
                                    if (!photo.isEmpty() && resourceUrl != null) {
                                        String photoPath = folderPath + File.separator + photo;
                                        Path photoFilePath = Paths.get(photoPath);
                                        try {
                                            Files.delete(photoFilePath);
                                            System.out.println("Photo deleted successfully.");
                                        } catch (IOException e) {
                                            // Handle the exception if the file deletion fails
                                            System.err.println("Unable to delete the photo: " + e.getMessage());
                                        }
                                    }
                                }
                                // Wyslij potwierdzenie sukcesu
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

            // Jeśli kod dochodzi do tego miejsca, to usuwanie oferty nie powiodło się
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

                                    ClassLoader classLoader = Server.class.getClassLoader();
                                    URL resourceUrl = classLoader.getResource("");
                                    if (resourceUrl != null) {
                                        String classpath = new File(resourceUrl.getFile()).getPath(); // Poprawka dla obsługi ścieżki bez "file:"
                                        String folderPath = classpath + File.separator + "img" + File.separator + "user/" + session.username;
                                        //System.err.println("PATH: " + folderPath);
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
                                    } else {
                                        System.err.println("Nie można uzyskać ścieżki do katalogu classpath.");
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

                                            ClassLoader classLoader = Server.class.getClassLoader();
                                            URL resourceUrl = classLoader.getResource("");
                                            if (resourceUrl != null) {
                                                String classpath = new File(resourceUrl.getFile()).getPath();
                                                String folderPath = classpath + File.separator + "img" + File.separator + "user/" + session.username;
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
                                            } else {
                                                System.err.println("Nie można uzyskać ścieżki do katalogu classpath.");
                                                SendError(output, "Nie można uzyskać ścieżki do katalogu classpath przez serwer.", res);
                                            }
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
                    try {
                        SendError(output,"Wystapil problem polaczenia z baza danych.", res);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
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
            String query = "SELECT typ.`dodajogloszenia`,typ.`wypozyczauto`,typ.`usunogloszenie`, typ.`manageReservations` FROM typy_uzytkownikow typ INNER JOIN uzytkownicy uz ON typ.id_typu = uz.typy_uzytkownikow_id_typu WHERE uz.login = '" + session.username + "';";
            ResultSet rsS = dbh.executeQuery(query);
            try {
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
    private boolean reservationExists (int id, ObjectOutputStream output, DatabaseHandler dbh) {
        String query = "SELECT * FROM wypozyczenie " +
                "WHERE id_wypozyczenia = " + id +
                " AND (data_wypozyczenia IS NULL OR (data_wypozyczenia IS NOT NULL AND NOW() BETWEEN data_wypozyczenia AND DATE_ADD(data_wypozyczenia, INTERVAL days DAY)))";
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
            if (!session.canReserve)
            {
                try {
                    SendError(output, "Nie masz uprawnień do rezerwacji.", new NetData(NetData.Operation.ReservationRequest));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
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
    private void handleOfferDetails(NetData data, User session, ObjectOutputStream output)
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
                //System.out.println("SENDING AN OFFER.");
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
                    response.Booleans.add(session.isSignedIn && !session.username.isEmpty() && session.canDeleteOffers);
                    String imagesString = result.getString("wiekszeZdjecia");
                    if (imagesString != null && !imagesString.isEmpty()) {
                        String[] images = imagesString.split(";");
                        for (String image : images) {
                            byte[] img = Utilities.loadImageAsBytes(image, false);
                            //System.out.println("IMG SIZE: " + img.length);
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
                                fetchUserPermissions(session);
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
                        fetchUserPermissions(session);
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

    private void handleOfferElement(ObjectOutputStream output, User session)  {
        String query = "SELECT a.`id_auta`, a.`marka`, a.`model`, a.`rok_prod`, a.`silnik`, a.`zdjecie`, a.`opis`, a.`cenaZaDzien` " +
                "FROM `auta` a " +
                "LEFT JOIN `wypozyczenie` w ON a.`id_auta` = w.`auta_id_auta` " +
                "WHERE w.`id_wypozyczenia` IS NULL " +
                "   OR (w.`data_wypozyczenia` IS NOT NULL " +
                "       AND NOT (NOW() BETWEEN w.`data_wypozyczenia` AND DATE_ADD(w.`data_wypozyczenia`, INTERVAL w.`days` DAY))) " +
                "ORDER BY a.`cenaZaDzien` ASC;";
        try {
            NetData addBr = new NetData(NetData.Operation.addButton);
            addBr.Booleans.add(session.canAddOffers);
            output.writeObject(addBr);
            output.flush();
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
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
                        byte[] img = Utilities.loadImageAsBytes(zdjecie, false);
                        //System.out.println("IMG SIZE: " + img.length);
                        response.Images.add(img);
                    }
                }
                catch (Exception ex)
                {
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
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        dbh.close();
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
    boolean canAddOffers = false;
    boolean canDeleteOffers = false;
    boolean canReserve = false;
    boolean canManageReservations = false;
}
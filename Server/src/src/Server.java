package src;

import src.DatabaseRepositories.ReservationRepository;
import src.packets.*;
import javax.imageio.ImageIO;
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
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public volatile boolean closing = false;
    public ExecutorService executor;
    public ArrayList<User> connectedClients = new ArrayList<>();
    private ServerSocket serverSocket;

    public void start(String ip, int port) throws IOException {
        initializeServerSocket(port);
        startConsoleThread();
        acceptClientConnections();
    }

    private void initializeServerSocket(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }
    private void startConsoleThread() {
        ConsoleHandler consoleHandler = new ConsoleHandler(serverSocket,this);
        Thread console = new Thread(consoleHandler);
        console.setDaemon(true);
        console.start();
    }
    private void acceptClientConnections() {
        executor = Executors.newCachedThreadPool();
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
            if (closing) {
                break;
            }
        }
    }
    private void handleClient(Socket socket) {
        User session = null;
        try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
            System.out.println("CLIENT CONNECTED " + socket);
            session = initializeSession(socket);
            while (true) {
                NetData data = (NetData) input.readObject();
                if (data.operation.equals(NetData.Operation.Exit)) {
                    handleExit(socket, output, session);
                    break;
                } else if (!data.operation.equals(NetData.Operation.Ping)) {
                    try {
                        handleOperation(data, output, session);
                    } catch (SQLException e) {
                        SendError(output, "Blad polaczenia z baza danych.");
                    }
                }
            }
        } catch (SocketException e) {
            handleUnexpectedClosure(socket, session);
        } catch (IOException | ClassNotFoundException e) {
            handleIOException(socket, session, e);
        }
    }

    private User initializeSession(Socket socket) {
        User session = new User();
        session.isSignedIn = false;
        session.clientSocket = socket;
        connectedClients.add(session);
        return session;
    }

    private void handleExit(Socket socket, ObjectOutputStream output, User session) throws IOException {
        System.out.println("Client " + socket + " sends exit...");
        System.out.println("Connection closing...");
        socket.close();
        System.out.println("Closed");
        connectedClients.remove(session);
    }

    private void handleOperation(NetData data, ObjectOutputStream output, User session) throws SQLException, IOException {
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
            case FilteredOffersRequest:
                handleOfferElement(data, output, session);
                break;
            case OfferDetails:
                handleOfferDetails(data, session, output);
                break;
            case Logout:
                handleLogout(data, output, session);
                break;
            case ReservationRequest:
                handleReservation(data, session, output);
                break;
            case AddOffer:
                handleAddOffer(data, session, output);
                break;
            case DeleteOffer:
                handleDeleteOffer(data, session, output);
                break;
            case RequestConfirmtations:
                handleSendConfirmations(data, session, output);
                break;
            case ManageReservation:
                handleManageReservation(data, session, output);
                break;
            case ConfirmationsButton:
                handleConfirmButton(data, session, output);
                break;
            default:
                System.out.println(session.clientSocket + " requested unknown operation.");
                break;
        }
    }

    private void handleLogout(NetData data, ObjectOutputStream output, User session) throws IOException {
        if (session.isSignedIn) {
            try {
                session.isSignedIn = false;
                session.username = "";
                NetData response = new NetData(NetData.Operation.Logout);
                output.writeObject(response);
                output.flush();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            SendError(output, "Nie jestes zalogowany/a!");
        }
    }

    private void handleUnexpectedClosure(Socket socket, User session) {
        System.out.println("Client " + socket + " unexpectedly closed the connection.");
        if (session != null) {
            connectedClients.remove(session);
        }
    }

    private void handleIOException(Socket socket, User session, Exception e) {
        e.printStackTrace();
        if (session != null) {
            connectedClients.remove(session);
        }
    }

    private void handleConfirmButton(NetData data, User session, ObjectOutputStream output) {
        ConfirmButtonVisibility res = new ConfirmButtonVisibility(NetData.Operation.ConfirmationsButton, false);
        if (session.isSignedIn && !session.username.isEmpty() && session.canManageReservations) {
            res.isVisible = true;
        }
        try {
            output.writeObject(res);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleManageReservation(NetData data, User session, ObjectOutputStream output) throws IOException {
        if (!session.hasPermission(data.operation)) {
            SendError(output, "Nie możesz wykonać tej operacji! (Brak uprawnień lub wylogowano)");
            return;
        }
        ManageReservationRequest req = (ManageReservationRequest) data;
        ReservationRepository reservationRepository = new ReservationRepository(new DatabaseHandler());
        if (req.id != 0) {
            boolean success;
            if (req.confirm)
                success = reservationRepository.confirmReservation(req.id);
            else
                success = reservationRepository.deleteReservation(req.id);
            if (success) {
                req.operationType = NetData.OperationType.Success;
                output.writeObject(req);
                output.flush();
            } else
                SendError(output, "Błąd operacji!");
        } else
            SendError(output, "Błąd przetwarzania żądania!");
    }

    private void handleSendConfirmations(NetData data, User session, ObjectOutputStream output) throws IOException, SQLException {
        if (!session.hasPermission(data.operation)) {
            SendError(output, "Nie możesz wykonać tej operacji! (Brak uprawnień lub wylogowano)");
            return;
        }
        DatabaseHandler dbh = new DatabaseHandler();
        if (!checkDBConnection(dbh, output)) {
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
        while (results != null && results.next()) {
            ReservationElement reservation = new ReservationElement();
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

            reservation.reserveId = reserveId;
            reservation.reserveDays = reserveDays;
            reservation.brand = marka;
            reservation.model = model;
            reservation.productionYear = rokProd;
            reservation.carId = idAuta;
            reservation.dailyPrice = cenaZaDzien;
            reservation.login = login;
            reservation.firstName = imie;
            reservation.lastName = nazwisko;
            reservation.phoneNumber = numerTelefonu;

            output.writeObject(reservation);
            output.flush();
        }
    }

    private void handleDeleteOffer(NetData data, User session, ObjectOutputStream output) throws IOException {
        if (!session.hasPermission(data.operation)) {
            SendError(output, "Nie możesz wykonać tej operacji! (Brak uprawnień lub wylogowano)");
            return;
        }
        DeleteOfferRequestPacket req = (DeleteOfferRequestPacket) data;
        if (req.id != 0) {
            try {
                DatabaseHandler dbh = new DatabaseHandler();
                if (!checkDBConnection(dbh, output)) {
                    return;
                }

                String query = "SELECT zdjecie, wiekszeZdjecia FROM auta WHERE id_auta = ?";
                try (PreparedStatement selectStatement = dbh.conn.prepareStatement(query)) {
                    selectStatement.setInt(1, req.id);
                    ResultSet resultSet = selectStatement.executeQuery();

                    if (resultSet.next()) {
                        String thumbnail = resultSet.getString("zdjecie");
                        String[] photosIndividual = resultSet.getString("wiekszeZdjecia").split(";");

                        query = "DELETE FROM auta WHERE id_auta = ?";
                        try (PreparedStatement deleteStatement = dbh.conn.prepareStatement(query)) {
                            deleteStatement.setInt(1, req.id);
                            int rowsDeleted = deleteStatement.executeUpdate();

                            if (rowsDeleted > 0) {
                                String folderPath = ServerMain.imagePath;
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
            SendError(output, "Nie można usunąć tej oferty lub oferta nie istnieje!");
        } else {
            SendError(output, "Nieprawidłowe dane przesłane do usuwania oferty!");
        }
    }


    private void handleAddOffer(NetData data, User session, ObjectOutputStream output) throws IOException {
        if (!session.hasPermission(data.operation)) {
            SendError(output, "Nie możesz wykonać tej operacji! (Brak uprawnień lub wylogowano)");
            return;
        }
        VehiclePacket vp = (VehiclePacket) data;
        if (!vp.isAnyRequiredEmpty()) {
            String thumbname = "";
            if (vp.thumbnail != null && vp.thumbnail.length > 0) {
                System.out.println(vp.thumbnailPath);
                thumbname = "user/" + session.username + "/" + vp.thumbnailPath;
                if (thumbname.length() > 64) {
                    SendError(output, "Przekroczono maksymalna dlugosc znakow w miniaturce, sprobuj skrocic nazwe pliku!");
                    return;
                }
                URL thumburl = Server.class.getResource("/img/" + thumbname);
                //System.err.println("URL: " + thumburl);
                if (Utilities.fileExists(thumburl)) {
                    SendError(output, "Plik miniaturki o danej nazwie już istnieje!");
                    return;
                }
            }
            String dbPhotos = "";
            if (vp.engineCap <= 0) {
                SendError(output, "Podano złą pojemność silnika!");
            }
            if (!vp.imagePaths.isEmpty() && !vp.images.isEmpty()) {
                for (String photo : vp.imagePaths) {
                    String photoname = "user/" + session.username + "/" + photo;
                    URL resourceUrl = Server.class.getResource("/img/" + photoname);
                    if (Utilities.fileExists(resourceUrl)) {
                        SendError(output, "Conajmniej jeden z przesłanych plików już istnieje!");
                        return;
                    }
                    dbPhotos += photoname + ";";
                }

                dbPhotos = dbPhotos.trim();
                if (dbPhotos.endsWith(";")) {
                    dbPhotos = dbPhotos.substring(0, dbPhotos.length() - 1);
                }
                if (dbPhotos.length() > 256) {
                    SendError(output, "Przekroczono maksymalna dlugosc znakow w zdjeciach, sprobuj skrocic nazwy!");
                    return;
                }
            }
            DatabaseHandler dbh = new DatabaseHandler();
            if (!checkDBConnection(dbh, output)) {
                return;
            }
            String query = "INSERT INTO auta (marka, model, rok_prod, silnik, zdjecie, opis, cenaZaDzien, wiekszeZdjecia, pojemnosc) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = dbh.conn.prepareStatement(query)) {
                preparedStatement.setString(1, vp.brand);
                preparedStatement.setString(2, vp.model);
                preparedStatement.setInt(3, vp.year);
                preparedStatement.setString(4, vp.engine);
                preparedStatement.setString(5, thumbname);
                preparedStatement.setString(6, vp.description);
                preparedStatement.setFloat(7, vp.price);
                preparedStatement.setString(8, dbPhotos);
                preparedStatement.setInt(9, vp.engineCap);
                int queryres = preparedStatement.executeUpdate();
                if (queryres > 0) {
                    if (vp.thumbnail != null) {
                        byte[] thumb = vp.thumbnail;
                        if (thumb.length > 0) {
                            try {
                                BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(thumb));

                                String folderPath = ServerMain.imagePath + File.separator + "user/" + session.username;
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
                                String imgPath = folderPath + File.separator + vp.thumbnailPath;

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

                        if (vp.imagePaths != null && vp.images.size() == vp.imagePaths.size()) {
                            for (int i = 0; i < vp.imagePaths.size(); i++) {
                                String photoname = "user/" + session.username + "/" + vp.imagePaths.get(i);
                                byte[] img = vp.images.get(i);
                                if (img.length > 0) {
                                    try {
                                        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(img));
                                        String folderPath = ServerMain.imagePath + File.separator + "user/" + session.username;

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

                                        String imgPath = folderPath + File.separator + vp.imagePaths.get(i);

                                        ImageIO.write(bufferedImage, "jpg", new File(imgPath));
                                        System.out.println("Obraz został zapisany pomyślnie.");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        System.err.println("Błąd podczas zapisywania obrazu.");
                                        SendError(output, "Błąd podczas zapisywania obrazu przez serwer.");
                                    }
                                }
                            }
                        } else {
                            System.out.println("Rozmiar zdjęć nieprawidłowy.\nIMG: " + vp.images.size() + "\nIMG PATHS: " + vp.imagePaths.size());
                        }
                    }
                    NetData newRes = new NetData(NetData.Operation.AddOffer);
                    newRes.operationType = NetData.OperationType.Success;
                    output.writeObject(newRes);
                    output.flush();
                } else {
                    SendError(output, "Wystapil problem z dodawaniem ogloszenia.");
                }
            } catch (SQLException e) {
                SendError(output, "Wystapil problem polaczenia z baza danych.");
                e.printStackTrace();
            }

        }
    }

    private void fetchUserPermissions(User session) {
        if (session.isSignedIn) {
            DatabaseHandler dbh = new DatabaseHandler();
            if (!checkDBConnection(dbh, null))
                return;
            String query = "SELECT typ.`dodajogloszenia`, typ.`wypozyczauto`, typ.`usunogloszenie`, typ.`manageReservations` FROM typy_uzytkownikow typ INNER JOIN uzytkownicy uz ON typ.id_typu = uz.typy_uzytkownikow_id_typu WHERE uz.login = ?";
            try (PreparedStatement selectStatement = dbh.conn.prepareStatement(query)) {
                selectStatement.setString(1, session.username);
                ResultSet rsS = selectStatement.executeQuery();
                while (rsS.next()) {
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
            SendError(output, "Blad bazy danych!");
            return true;
        }
    }

    private void handleReservation(NetData data, User session, ObjectOutputStream output) {

        if (!session.hasPermission(data.operation)) {
            SendError(output, "Nie możesz wykonać tej operacji! (Brak uprawnień lub wylogowano)");
            return;
        }
        ReservationRequestPacket resData = (ReservationRequestPacket) data;
        DatabaseHandler dbh = new DatabaseHandler();
        if (!checkDBConnection(dbh, output)) {
            return;
        }

        if (!reservationExists(resData.id, output, dbh)) {
            System.out.println("Rezerwacja nie istnieje");

            String query = "INSERT INTO wypozyczenie (`uzytkownicy_id_uzytkownika`, `auta_id_auta`, `days`)" +
                    " SELECT id_uzytkownika, ?, ? FROM uzytkownicy" +
                    " WHERE login = ?";

            try (PreparedStatement insertStatement = dbh.conn.prepareStatement(query)) {
                insertStatement.setInt(1, resData.id);
                insertStatement.setInt(2, resData.days);
                insertStatement.setString(3, session.username);
                int res = insertStatement.executeUpdate();
                if (res <= 0) {
                    SendError(output, "Nie udało się zarezerwować pojazdu. Spróbuj ponownie później.");
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
                SendError(output, "Błąd bazy danych!");
            } finally {
                dbh.close();
            }
        } else {
            SendError(output, "Rezerwacja istnieje, albo wystąpił błąd połączenia z bazą danych.");
        }
    }
    private void handleOfferDetails(NetData data, User session, ObjectOutputStream output) {

        OfferDetailsRequestPacket req = (OfferDetailsRequestPacket)data;
        if (req.id > 0) {
            DatabaseHandler dbh = new DatabaseHandler();
            if (!checkDBConnection(dbh, output)) {
                return;
            }
            String query = "SELECT * FROM `auta` WHERE `id_auta` = ?";
            try (PreparedStatement statement = dbh.conn.prepareStatement(query)) {
                statement.setInt(1, req.id);

                ResultSet result = statement.executeQuery();

                if (result.next()) {
                    VehiclePacket response = new VehiclePacket();
                    response.operation= NetData.Operation.OfferDetails;
                    try {
                        String marka = result.getString("marka");
                        String model = result.getString("model");
                        String silnik = result.getString("silnik");
                        int rokProdukcji = result.getInt("rok_prod");
                        int poj = result.getInt("pojemnosc");
                        float cenaZaDzien = result.getFloat("cenaZaDzien");
                        String opis = result.getString("opis");

                        response.brand=marka;
                        response.model=model;
                        response.engine=silnik;
                        response.year=rokProdukcji;
                        response.price=cenaZaDzien;
                        response.description=opis;
                        response.databaseId=req.id;
                        response.engineCap=poj;
                        response.canBeDeleted=session.isSignedIn && !session.username.isEmpty() && session.canDeleteOffers;

                        String imagesString = result.getString("wiekszeZdjecia");
                        if (imagesString != null && !imagesString.isEmpty()) {
                            String[] images = imagesString.split(";");
                            for (String image : images) {
                                byte[] img = Utilities.loadImageAsBytes(image, false);
                                if (img.length > 0) {
                                    response.images.add(img);
                                }
                            }
                        }

                        output.writeObject(response);
                        output.flush();
                    } catch (SQLException | IOException e) {
                        e.printStackTrace();
                        SendError(output, "Błąd przetwarzania wyników zapytania.");
                    }
                } else {
                    SendError(output, "Brak oferty o podanym identyfikatorze.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                SendError(output, "Błąd bazy danych!");
            } finally {
                dbh.close();
            }
        } else {
            SendError(output, "Nieprawidłowe dane zapytania.");
        }
    }


    private void handleRegister(NetData data, ObjectOutputStream output, User session) throws IOException, SQLException {
        NetData response = new NetData(NetData.Operation.Register);
        if (session.isSignedIn) {
            SendError(output, "Jesteś już zalogowany!");
            return;
        }
        RegisterPacket regData = (RegisterPacket) data;

        if (String.valueOf(regData.phonenumber).length() == 9) {
            if(regData.anyEmpty())
            {
                SendError(output, "Żadne pole nie może być puste!");
                return;
            }
            if (regData.password.equals(regData.repeat_password))
            {
                String existsQuery = "SELECT `id_uzytkownika` FROM `uzytkownicy` WHERE `login` = ? OR numer_telefonu = ?";
                DatabaseHandler dbh = new DatabaseHandler();

                try {
                    if (!checkDBConnection(dbh, output)) {
                        return;
                    }

                    try (PreparedStatement existsStatement = dbh.conn.prepareStatement(existsQuery)) {
                        existsStatement.setString(1, regData.login);
                        existsStatement.setInt(2, regData.phonenumber);

                        ResultSet existing = existsStatement.executeQuery();

                        if (existing.next()) {
                            SendError(output, "Użytkownik o podanym loginie lub numerze telefonu już istnieje.");
                            return;
                        }
                    }

                    String registerQuery = "INSERT INTO uzytkownicy(`login`,`password`,`imie`,`nazwisko`,`data_utworzenia`,`numer_telefonu`,`typy_uzytkownikow_id_typu`) VALUES (?, ?, ?, ?, NOW(), ?, 1)";
                    try (PreparedStatement registerStatement = dbh.conn.prepareStatement(registerQuery)) {
                        registerStatement.setString(1, regData.login);
                        registerStatement.setString(2, regData.password);
                        registerStatement.setString(3, regData.imie);
                        registerStatement.setString(4, regData.nazwisko);
                        registerStatement.setInt(5, regData.phonenumber);

                        int registerResult = registerStatement.executeUpdate();

                        if (registerResult > 0) {
                            response.operationType = NetData.OperationType.Success;
                            output.writeObject(response);
                            output.flush();
                            session.isSignedIn = true;
                            session.username = regData.login;
                            fetchUserPermissions(session);
                        } else {
                            SendError(output, "Nie udało się zarejestrować.");
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    SendError(output, "Błąd bazy danych!");
                } finally {
                    dbh.close();
                }
            } else {
                SendError(output, "Podane hasła nie są identyczne!");
            }
        } else {
            SendError(output, "Numer telefonu jest nieprawidłowy!");
        }
    }

    private void handleLogin(NetData data, ObjectOutputStream output, User session) throws IOException, SQLException {

        if (session.isSignedIn) {
            SendError(output, "Jesteś już zalogowany!");
            return;
        }
        LoginPacket logdata = (LoginPacket) data;
        if (logdata.login.isEmpty() || logdata.password.isEmpty()) {
            SendError(output, "Żadne pole nie może być puste!");
        }

        String existsQuery = "SELECT `id_uzytkownika` FROM `uzytkownicy` WHERE BINARY `login` = ? AND BINARY password = ?";
        DatabaseHandler dbh = new DatabaseHandler();

        try {
            if (!checkDBConnection(dbh, output)) {
                return;
            }

            try (PreparedStatement existsStatement = dbh.conn.prepareStatement(existsQuery)) {
                existsStatement.setString(1, logdata.login);
                existsStatement.setString(2, logdata.password);

                ResultSet existing = existsStatement.executeQuery();
                if (existing.next()) {
                    session.isSignedIn = true;
                    session.username = logdata.login;
                    fetchUserPermissions(session);
                    NetData response = new NetData(NetData.Operation.Login);
                    response.operationType = NetData.OperationType.Success;
                    output.writeObject(response);
                    output.flush();
                } else {
                    SendError(output, "Nie udało się zalogować! Upewnij się, że dane są poprawne.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            SendError(output, "Błąd bazy danych!");
        } finally {
            dbh.close();
        }

    }
    private void handleOfferUsername(ObjectOutputStream output, User session) throws IOException {
        UsernamePacket response = new UsernamePacket();
        response.isSignedIn=session.isSignedIn;
        if (session.isSignedIn) {
            response.username=session.username;
        }
        output.writeObject(response);
        output.flush();
    }
    private void handleOfferElement(NetData data, ObjectOutputStream output, User session) {
        try {
            AddOfferButtonVisibility addBr = new AddOfferButtonVisibility(session.canAddOffers);
            output.writeObject(addBr);
            output.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DatabaseHandler dbh = new DatabaseHandler();
        if (!checkDBConnection(dbh, output)) {
            return;
        }
        FilteredOffersRequestPacket requestPacket = (FilteredOffersRequestPacket)data;
        System.out.println("DESC? " + requestPacket.priceDESC);

        String query = "SELECT " +
                "    a.`id_auta`, a.`marka`, a.`model`, a.`rok_prod`, a.`silnik`, a.`zdjecie`, a.`opis`, a.`cenaZaDzien`, a.`pojemnosc`, " +
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
                "    ) ";
        if (requestPacket.brand != null && !requestPacket.brand.isEmpty() && !requestPacket.brand.equals("KAŻDA")) {
            query += "AND a.`marka` = ? ";
        }

        if (requestPacket.yearMin != -1) {
            query += "AND a.`rok_prod` >= ? ";
        }

        if (requestPacket.yearMax != -1) {
            query += "AND a.`rok_prod` <= ? ";
        }

        if (requestPacket.engineCapMin != -1) {
            query += "AND a.`pojemnosc` >= ? ";
        }

        if (requestPacket.engineCapMax != -1) {
            query += "AND a.`pojemnosc` <= ? ";
        }

        if (requestPacket.priceMin != -1) {
            query += "AND a.`cenaZaDzien` >= ? ";
        }

        if (requestPacket.priceMax != -1) {
            query += "AND a.`cenaZaDzien` <= ? ";
        }
        query += "ORDER BY a.`cenaZaDzien`" + (requestPacket.priceDESC ? "DESC" : "ASC") + ";";
        try {
            PreparedStatement preparedStatement = dbh.conn.prepareStatement(query);
            preparedStatement.setString(1, session.username);
            preparedStatement.setString(2, session.username);
            int parameterIndex = 3;
            if (requestPacket.brand != null && !requestPacket.brand.isEmpty() && !requestPacket.brand.equals("KAŻDA")) {
                preparedStatement.setString(parameterIndex++, requestPacket.brand);
            }

            if (requestPacket.yearMin != -1) {
                preparedStatement.setInt(parameterIndex++, requestPacket.yearMin);
            }

            if (requestPacket.yearMax != -1) {
                preparedStatement.setInt(parameterIndex++, requestPacket.yearMax);
            }

            if (requestPacket.engineCapMin != -1) {
                preparedStatement.setInt(parameterIndex++, requestPacket.engineCapMin);
            }

            if (requestPacket.engineCapMax != -1) {
                preparedStatement.setInt(parameterIndex++, requestPacket.engineCapMax);
            }

            if (requestPacket.priceMin != -1) {
                preparedStatement.setFloat(parameterIndex++, requestPacket.priceMin);
            }

            if (requestPacket.priceMax != -1) {
                preparedStatement.setFloat(parameterIndex++, requestPacket.priceMax);
            }
            ResultSet result = preparedStatement.executeQuery();
            while (result.next()) {
                VehiclePacket response = new VehiclePacket();
                int id = result.getInt("id_auta");
                String marka = result.getString("marka");
                String model = result.getString("model");
                String silnik = result.getString("silnik");
                int prod = result.getInt("rok_prod");
                float cena = result.getFloat("cenaZaDzien");
                int poj = result.getInt("pojemnosc");
                response.brand=marka;
                response.model=model;
                response.engine=silnik;
                response.year=prod;
                response.price = cena;
                response.databaseId=id;
                if (!result.wasNull())
                    response.engineCap=poj;
                int idWypo = result.getInt("id_wypozyczenia");
                if (!result.wasNull())
                {
                    Date dataWypo = result.getDate("data_wypozyczenia");
                    if (result.wasNull()) {
                        response.daysLeft=-1;
                    } else {
                        int daysLeft = result.getInt("dni_pozostale");
                        if (!result.wasNull()) {
                            response.isRented=true;
                            response.daysLeft=daysLeft;
                        }
                    }
                }
                try {
                    String zdjecie = result.getString("zdjecie");
                    if (zdjecie != null && !zdjecie.isEmpty()) {
                        byte[] img = Utilities.loadImageAsBytes(zdjecie, false);
                        //System.out.println("IMG SIZE: " + img.length);
                        response.thumbnail=img;
                    }
                } catch (Exception ex) {
                    System.err.println(ex.getLocalizedMessage());
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
            BrandsList brands = new BrandsList();
            brands.brands=getFilteredBrandsForOffers(requestPacket,session);
            output.writeObject(brands);
            output.flush();
        } catch (SQLException e) {
            e.printStackTrace();
            SendError(output, "Błąd bazy danych.");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        dbh.close();
    }
    private HashSet<String> getFilteredBrandsForOffers(FilteredOffersRequestPacket requestPacket, User session) {
        HashSet<String> filteredBrands = new HashSet<>();

        try (DatabaseHandler dbh = new DatabaseHandler()) {
            String brandQuery = "SELECT DISTINCT a.`marka` FROM `auta` a " +
                    "LEFT JOIN `wypozyczenie` w ON a.`id_auta` = w.`auta_id_auta` " +
                    "LEFT JOIN `uzytkownicy` u ON w.`uzytkownicy_id_uzytkownika` = u.`id_uzytkownika` " +
                    "WHERE " +
                    "( " +
                    "    w.`id_wypozyczenia` IS NULL " +
                    "    OR ( " +
                    "        w.`data_wypozyczenia` IS NOT NULL " +
                    "        AND NOT (NOW() BETWEEN w.`data_wypozyczenia` AND DATE_ADD(w.`data_wypozyczenia`, INTERVAL w.`days` DAY)) " +
                    "    ) " +
                    "    OR ( " +
                    "        u.`id_uzytkownika` IS NOT NULL " +
                    "        AND u.`login` = ? " +
                    "        AND (NOW() BETWEEN IFNULL(w.`data_wypozyczenia`, NOW()) AND DATE_ADD(IFNULL(w.`data_wypozyczenia`, NOW()), INTERVAL IFNULL(w.`days`, 0) DAY)) " +
                    "    ) " +
                    "    OR ( " +
                    "        w.`data_wypozyczenia` IS NULL " +
                    "        AND u.`login` = ? " +
                    "    ) " +
                    ") ";
            PreparedStatement brandStatement = dbh.conn.prepareStatement(brandQuery);
            brandStatement.setString(1, session.username);
            brandStatement.setString(2, session.username);
            ResultSet brandResult = brandStatement.executeQuery();

            while (brandResult.next()) {
                filteredBrands.add(brandResult.getString("marka"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return filteredBrands;
    }
    private void SendError(ObjectOutputStream output, String error) {
        if (output == null)
            return;
        ErrorPacket err = new ErrorPacket(error);
        err.operationType = NetData.OperationType.Error;
        try {
            output.writeObject(err);
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
                    SendError(output, "Błąd połączenia z bazą danych.");
                dbh.close();
                return false;
            }
        } catch (SQLException e) {
            if (output!=null) {
                SendError(output, "Błąd połączenia z bazą danych.");
            }
            return false;
        }
        return true;
    }
    /*void SendMessage(ObjectOutputStream output, String mes, NetData data) throws IOException {
        if (data == null || output == null)
            return;
        data.operationType = NetData.OperationType.MessageBox;
        data.Strings.add(mes);
        output.writeObject(data);
        output.flush();
    }*/
}

class User {
    public boolean isSignedIn;
    public String username;
    public transient Socket clientSocket;
    boolean canAddOffers = false;
    boolean canDeleteOffers = false;
    boolean canReserve = false;
    boolean canManageReservations = false;
    public boolean hasPermission(NetData.Operation operation) {
        if (!isSignedIn || username.isEmpty())
            return false;
        return switch (operation) {
            case AddOffer -> canAddOffers;
            case DeleteOffer -> canDeleteOffers;
            case ReservationRequest -> canReserve;
            case RequestConfirmtations, ManageReservation -> canManageReservations;
            default -> false;
        };
    }
}
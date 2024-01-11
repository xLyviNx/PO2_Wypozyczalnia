package src;

import src.DatabaseRepositories.ConfirmationRepository;
import src.DatabaseRepositories.OfferRepository;
import src.DatabaseRepositories.ReservationRepository;
import src.DatabaseRepositories.UserRepository;
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
import java.util.List;
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
        ConfirmationRepository confirmationRepository = new ConfirmationRepository(dbh);
        List<ReservationElement> reservations = confirmationRepository.getUnconfirmedReservations();
        reservations.forEach(reservation -> {
            try {
                output.writeObject(reservation);
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void handleDeleteOffer(NetData data, User session, ObjectOutputStream output) throws IOException {
        if (!session.hasPermission(data.operation)) {
            SendError(output, "Nie możesz wykonać tej operacji! (Brak uprawnień lub wylogowano)");
            return;
        }
        DeleteOfferRequestPacket req = (DeleteOfferRequestPacket) data;
        if (req.id != 0) {
            try {
                OfferRepository offerRepository = new OfferRepository(new DatabaseHandler());
                boolean success = offerRepository.deleteOffer(req.id);
                if (success) {
                    NetData successRes = new NetData(NetData.Operation.DeleteOffer);
                    successRes.operationType = NetData.OperationType.Success;
                    output.writeObject(successRes);
                    output.flush();
                    return;
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
            OfferRepository offerRepository = new OfferRepository(new DatabaseHandler());
            boolean success = offerRepository.addOffer(vp, session);
            if (success) {
                NetData newRes = new NetData(NetData.Operation.AddOffer);
                newRes.operationType = NetData.OperationType.Success;
                output.writeObject(newRes);
                output.flush();
            } else {
                SendError(output, "Wystąpił problem z dodawaniem ogłoszenia.");
            }
        }
    }

    private void fetchUserPermissions(User session) {
        if (session.isSignedIn) {
            UserRepository userRepository = new UserRepository(new DatabaseHandler());
            userRepository.fetchUserPermissions(session);
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
        ReservationRepository reservationRepository = new ReservationRepository(new DatabaseHandler());
        if (!reservationRepository.reservationExists(resData.id)) {
            try {
                reservationRepository.makeReservation(session.username, resData.id, resData.days);
                NetData response = new NetData(NetData.Operation.ReservationRequest);
                response.operationType = NetData.OperationType.Success;
                output.writeObject(response);
                output.flush();
            } catch (SQLException | IOException e) {
                e.printStackTrace();
                SendError(output, "Błąd podczas przetwarzania rezerwacji.");
            }
        } else {
            SendError(output, "Rezerwacja istnieje, albo wystąpił błąd połączenia z bazą danych.");
        }
    }
    private void handleOfferDetails(NetData data, User session, ObjectOutputStream output) {
        OfferDetailsRequestPacket req = (OfferDetailsRequestPacket) data;
        if (req.id > 0) {
            DatabaseHandler dbh = new DatabaseHandler();
            if (!checkDBConnection(dbh, output)) {
                return;
            }
            try {
                OfferRepository offerRepository = new OfferRepository(dbh);
                VehiclePacket response = offerRepository.getOfferDetails(req.id, session);
                if (response != null) {
                    output.writeObject(response);
                    output.flush();
                } else {
                    SendError(output, "Brak oferty o podanym identyfikatorze.");
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
                SendError(output, "Błąd przetwarzania zapytania o ofertę.");
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
            SendError(output, "Jesteś już zalogowany/a!");
            return;
        }
        RegisterPacket regData = (RegisterPacket) data;

        if (String.valueOf(regData.phonenumber).length() == 9) {
            if (regData.anyEmpty()) {
                SendError(output, "Żadne pole nie może być puste!");
                return;
            }
            if (regData.password.equals(regData.repeat_password)) {
                DatabaseHandler dbh = new DatabaseHandler();

                try (dbh) {
                    UserRepository userRepository = new UserRepository(dbh);
                    if (!checkDBConnection(dbh, output)) {
                        return;
                    }

                    if (userRepository.isUserExists(regData.login, regData.phonenumber)) {
                        SendError(output, "Użytkownik o podanym loginie lub numerze telefonu już istnieje.");
                        return;
                    }

                    if (userRepository.registerUser(regData.login, regData.password, regData.imie, regData.nazwisko, regData.phonenumber)) {
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
        DatabaseHandler dbh = new DatabaseHandler();
        UserRepository userRepository = new UserRepository(dbh);
        try {
            if (!checkDBConnection(dbh, output)) {
                return;
            }

            if (userRepository.loginUser(logdata.login, logdata.password)) {
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
        try (DatabaseHandler dbh = new DatabaseHandler()) {
            if (!checkDBConnection(dbh, output)) {
                return;
            }
            OfferRepository offerRepository = new OfferRepository(dbh);
            FilteredOffersRequestPacket requestPacket = (FilteredOffersRequestPacket) data;

            try {
                List<VehiclePacket> offers = offerRepository.getFilteredOffers(requestPacket, session);
                BrandsList brands = new BrandsList();
                brands.brands = offerRepository.getFilteredBrands(requestPacket, session);

                for (VehiclePacket offer : offers) {
                    output.writeObject(offer);
                    output.flush();
                }

                output.writeObject(brands);
                output.flush();

            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
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
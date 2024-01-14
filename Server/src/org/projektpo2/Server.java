package org.projektpo2;

import org.projektpo2.packets.*;
import org.projektpo2.DatabaseRepositories.ConfirmationRepository;
import org.projektpo2.DatabaseRepositories.OfferRepository;
import org.projektpo2.DatabaseRepositories.ReservationRepository;
import org.projektpo2.DatabaseRepositories.UserRepository;
import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasa reprezentująca serwer.
 */
public class Server {
    private static final Logger logger = Utilities.getLogger(Server.class);
    /** Flaga informująca o zamykaniu serwera. */
    public volatile boolean closing = false;

    /** Executor obsługujący wątki klientów. */
    public ExecutorService executor;

    /** Lista podłączonych klientów. */
    public ArrayList<User> connectedClients = new ArrayList<>();

    /** Gniazdo serwera. */
    private ServerSocket serverSocket;

    /**
     * Metoda rozpoczynająca działanie serwera.
     *
     * @param port Numer portu serwera.
     * @throws IOException W przypadku problemów związanych z gniazdem serwera.
     */
    public void start(int port) throws IOException {
        initializeServerSocket(port);
        startConsoleThread();
        acceptClientConnections();
    }

    /**
     * Inicjalizuje gniazdo serwera.
     *
     * @param port Numer portu serwera.
     * @throws IOException W przypadku problemów związanych z gniazdem serwera.
     */
    private void initializeServerSocket(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    /**
     * Rozpoczyna wątek obsługujący konsolę serwera.
     */
    private void startConsoleThread() {
        ConsoleHandler consoleHandler = new ConsoleHandler(serverSocket, this);
        Thread console = new Thread(consoleHandler);
        console.setDaemon(true);
        console.start();
    }

    /**
     * Akceptuje połączenia klientów i uruchamia wątek obsługujący każde połączenie.
     */
    private void acceptClientConnections() {
        executor = Executors.newCachedThreadPool();
        do {
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
        } while (!closing);
    }
    /**
     * Metoda obsługująca połączenie z klientem.
     *
     * @param socket Socket klienta.
     */
    private void handleClient(Socket socket) {
        User session = null;
        try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
            logger.info("CLIENT CONNECTED " + socket);
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
                        sendError(output, "Blad polaczenia z baza danych.");
                    }
                }
            }
        } catch (SocketException e) {
            handleUnexpectedClosure(socket, session);
        } catch (IOException | ClassNotFoundException e) {
            handleIOException(socket, session, e);
        }
    }

    /**
     * Inicjalizuje sesję klienta.
     *
     * @param socket Socket klienta.
     * @return Obiekt reprezentujący sesję klienta.
     */
    private User initializeSession(Socket socket) {
        User session = new User();
        session.isSignedIn = false;
        session.clientSocket = socket;
        connectedClients.add(session);
        return session;
    }

    /**
     * Metoda obsługująca żądanie wyjścia z połączenia.
     *
     * @param socket Socket klienta.
     * @param output Strumień wyjściowy.
     * @param session Sesja klienta.
     * @throws IOException W przypadku błędu związanego z operacją na strumieniu.
     */
    private void handleExit(Socket socket, ObjectOutputStream output, User session) throws IOException {
        logger.info("Client " + socket + " sends exit...");
        logger.info("Connection closing...");
        socket.close();
        logger.info("Closed");
        connectedClients.remove(session);
    }

    /**
     * Metoda obsługująca otrzymane od klienta operacje.
     *
     * @param data   Otrzymane dane od klienta.
     * @param output Strumień wyjściowy.
     * @param session Sesja klienta.
     * @throws SQLException W przypadku błędu związanego z bazą danych.
     * @throws IOException  W przypadku błędu związanego z operacją na strumieniu.
     */
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
            case RequestConfirmations:
                handleSendConfirmations(data, session, output);
                break;
            case ManageReservation:
                handleManageReservation(data, session, output);
                break;
            case ConfirmationsButton:
                handleConfirmButton(data, session, output);
                break;
            default:
                logger.info(session.clientSocket + " requested unknown operation.");
                break;
        }
    }

    /**
     * Metoda obsługująca wylogowanie klienta.
     *
     * @param data   Otrzymane dane od klienta.
     * @param output Strumień wyjściowy.
     * @param session Sesja klienta.
     * @throws IOException W przypadku błędu związanego z operacją na strumieniu.
     */
    private void handleLogout(NetData data, ObjectOutputStream output, User session) throws IOException {
        if (session.isSignedIn) {
            try {
                session.isSignedIn = false;
                session.username = "";
                NetData response = new NetData(NetData.Operation.Logout);
                output.writeObject(response);
                output.flush();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Error handling logout", ex);
            }
        } else {
            sendError(output, "Nie jestes zalogowany/a!");
        }
    }

    /**
     * Metoda obsługująca nieoczekiwane zamknięcie połączenia przez klienta.
     *
     * @param socket  Socket klienta.
     * @param session Sesja klienta.
     */
    private void handleUnexpectedClosure(Socket socket, User session) {
        logger.info("Client " + socket + " unexpectedly closed the connection.");
        if (session != null) {
            connectedClients.remove(session);
        }
    }

    /**
     * Metoda obsługująca błąd wejścia/wyjścia.
     *
     * @param socket  Socket klienta.
     * @param session Sesja klienta.
     * @param e       Wyjątek.
     */
    private void handleIOException(Socket socket, User session, Exception e) {
        logger.log(Level.SEVERE, "Error handling IO", e);
        if (session != null) {
            connectedClients.remove(session);
        }
    }

    /**
     * Metoda obsługująca żądanie potwierdzeń rezerwacji od klienta.
     *
     * @param data   Otrzymane dane od klienta.
     * @param session Sesja klienta.
     * @param output Strumień wyjściowy.
     * @throws IOException  W przypadku błędu związanego z operacją na strumieniu.
     * @throws SQLException W przypadku błędu związanego z bazą danych.
     */
    private void handleSendConfirmations(NetData data, User session, ObjectOutputStream output) throws IOException, SQLException {
        if (!session.hasPermission(data.operation)) {
            sendError(output, "Nie możesz wykonać tej operacji! (Brak uprawnień lub wylogowano)");
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
                logger.log(Level.SEVERE, "Error handling send confirmations", e);
            }
        });
    }

    /**
     * Metoda obsługująca zarządzanie rezerwacją od klienta.
     *
     * @param data   Otrzymane dane od klienta.
     * @param session Sesja klienta.
     * @param output Strumień wyjściowy.
     * @throws IOException W przypadku błędu związanego z operacją na strumieniu.
     */
    private void handleManageReservation(NetData data, User session, ObjectOutputStream output) throws IOException {
        if (!session.hasPermission(data.operation)) {
            sendError(output, "Nie możesz wykonać tej operacji! (Brak uprawnień lub wylogowano)");
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
                try {
                    output.writeObject(req);
                    output.flush();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error handling manage reservation", e);
                }
            } else
                sendError(output, "Błąd operacji!");
        } else
            sendError(output, "Błąd przetwarzania żądania!");
    }

    /**
     * Metoda obsługująca przycisk potwierdzeń rezerwacji od klienta.
     *
     * @param data   Otrzymane dane od klienta.
     * @param session Sesja klienta.
     * @param output Strumień wyjściowy.
     */
    private void handleConfirmButton(NetData data, User session, ObjectOutputStream output) {
        ConfirmButtonVisibility res = new ConfirmButtonVisibility(NetData.Operation.ConfirmationsButton, false);
        if (session.isSignedIn && !session.username.isEmpty() && session.canManageReservations) {
            res.isVisible = true;
        }
        try {
            output.writeObject(res);
            output.flush();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error handling confirm button", e);
        }
    }

    /**
     * Metoda obsługująca żądanie usunięcia oferty od klienta.
     *
     * @param data   Otrzymane dane od klienta.
     * @param session Sesja klienta.
     * @param output Strumień wyjściowy.
     * @throws IOException W przypadku błędu związanego z operacją na strumieniu.
     */
    private void handleDeleteOffer(NetData data, User session, ObjectOutputStream output) throws IOException {
        if (!session.hasPermission(data.operation)) {
            sendError(output, "Nie możesz wykonać tej operacji! (Brak uprawnień lub wylogowano)");
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
                logger.log(Level.SEVERE, "Error handling delete offer", e);
            }
            sendError(output, "Nie można usunąć tej oferty lub oferta nie istnieje!");
        } else {
            sendError(output, "Nieprawidłowe dane przesłane do usuwania oferty!");
        }
    }

    /**
     * Metoda obsługująca dodawanie nowej oferty od klienta.
     *
     * @param data   Otrzymane dane od klienta.
     * @param session Sesja klienta.
     * @param output Strumień wyjściowy.
     */
    private void handleAddOffer(NetData data, User session, ObjectOutputStream output) {
        try {
            if (!session.hasPermission(data.operation)) {
                sendError(output, "Nie możesz wykonać tej operacji! (Brak uprawnień lub wylogowano)");
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
                    sendError(output, "Wystąpił problem z dodawaniem ogłoszenia.");
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error handling add offer", ex);
        }
    }

    /**
     * Metoda pobierająca uprawnienia użytkownika.
     *
     * @param session Sesja użytkownika.
     */
    private void fetchUserPermissions(User session) {
        if (session.isSignedIn) {
            UserRepository userRepository = new UserRepository(new DatabaseHandler());
            userRepository.fetchUserPermissions(session);
        }
    }

    /**
     * Metoda obsługująca rezerwację pojazdu od klienta.
     *
     * @param data   Otrzymane dane od klienta.
     * @param session Sesja klienta.
     * @param output Strumień wyjściowy.
     */
    private void handleReservation(NetData data, User session, ObjectOutputStream output) {
        if (!session.hasPermission(data.operation)) {
            sendError(output, "Nie możesz wykonać tej operacji! (Brak uprawnień lub wylogowano)");
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
                logger.log(Level.SEVERE, "Error handling reservation", e);
                sendError(output, "Błąd podczas przetwarzania rezerwacji.");
            }
        } else {
            sendError(output, "Rezerwacja istnieje, albo wystąpił błąd połączenia z bazą danych.");
        }
    }

    /**
     * Metoda obsługująca żądanie szczegółów oferty od klienta.
     *
     * @param data   Otrzymane dane od klienta.
     * @param session Sesja klienta.
     * @param output Strumień wyjściowy.
     */
    private void handleOfferDetails(NetData data, User session, ObjectOutputStream output) {
        OfferDetailsRequestPacket req = (OfferDetailsRequestPacket) data;
        if (req.id > 0) {
            DatabaseHandler dbh = new DatabaseHandler();
            try (dbh) {
                if (!checkDBConnection(dbh, output)) {
                    return;
                }
                OfferRepository offerRepository = new OfferRepository(dbh);
                VehiclePacket response = offerRepository.getOfferDetails(req.id, session);
                if (response != null) {
                    output.writeObject(response);
                    output.flush();
                } else {
                    sendError(output, "Brak oferty o podanym identyfikatorze.");
                }
            } catch (SQLException | IOException e) {
                logger.log(Level.SEVERE, "Error handling offer details request", e);
                sendError(output, "Błąd przetwarzania zapytania o ofertę.");
            }
        } else {
            sendError(output, "Nieprawidłowe dane zapytania.");
        }
    }

    /**
     * Metoda obsługująca rejestrację użytkownika.
     *
     * @param data   Otrzymane dane od klienta.
     * @param output Strumień wyjściowy.
     * @param session Sesja klienta.
     * @throws IOException  W przypadku błędu związanego z operacją na strumieniu.
     * @throws SQLException W przypadku błędu związanego z operacją na bazie danych.
     */
    private void handleRegister(NetData data, ObjectOutputStream output, User session) throws IOException, SQLException {
        NetData response = new NetData(NetData.Operation.Register);
        if (session.isSignedIn) {
            sendError(output, "Jesteś już zalogowany/a!");
            return;
        }
        RegisterPacket regData = (RegisterPacket) data;

        if (String.valueOf(regData.phonenumber).length() == 9) {
            if (regData.anyEmpty()) {
                sendError(output, "Żadne pole nie może być puste!");
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
                        sendError(output, "Użytkownik o podanym loginie lub numerze telefonu już istnieje.");
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
                        sendError(output, "Nie udało się zarejestrować.");
                    }
                }
            } else {
                sendError(output, "Podane hasła nie są identyczne!");
            }
        } else {
            sendError(output, "Numer telefonu jest nieprawidłowy!");
        }
    }

    /**
     * Metoda obsługująca logowanie użytkownika.
     *
     * @param data   Otrzymane dane od klienta.
     * @param output Strumień wyjściowy.
     * @param session Sesja klienta.
     * @throws IOException  W przypadku błędu związanego z operacją na strumieniu.
     * @throws SQLException W przypadku błędu związanego z operacją na bazie danych.
     */
    private void handleLogin(NetData data, ObjectOutputStream output, User session) throws IOException, SQLException {
        if (session.isSignedIn) {
            sendError(output, "Jesteś już zalogowany!");
            return;
        }
        LoginPacket logdata = (LoginPacket) data;
        if (logdata.login.isEmpty() || logdata.password.isEmpty()) {
            sendError(output, "Żadne pole nie może być puste!");
        }
        DatabaseHandler dbh = new DatabaseHandler();
        try (dbh) {
            UserRepository userRepository = new UserRepository(dbh);
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
                sendError(output, "Nie udało się zalogować! Upewnij się, że dane są poprawne.");
            }
        }
    }

    /**
     * Metoda obsługująca żądanie przesyłania nazwy użytkownika oferty.
     *
     * @param output Strumień wyjściowy.
     * @param session Sesja klienta.
     * @throws IOException W przypadku błędu związanego z operacją na strumieniu.
     */
    private void handleOfferUsername(ObjectOutputStream output, User session) throws IOException {
        UsernamePacket response = new UsernamePacket();
        response.isSignedIn = session.isSignedIn;
        if (session.isSignedIn) {
            response.username = session.username;
        }
        output.writeObject(response);
        output.flush();
    }

    /**
     * Metoda obsługująca żądanie przesyłania informacji o dostępności przycisku dodawania oferty.
     *
     * @param data   Otrzymane dane od klienta.
     * @param output Strumień wyjściowy.
     * @param session Sesja klienta.
     */
    private void handleOfferElement(NetData data, ObjectOutputStream output, User session) {
        try {
            AddOfferButtonVisibility addBr = new AddOfferButtonVisibility(session.canAddOffers);
            output.writeObject(addBr);
            output.flush();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error handling offer element request", e);
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
                logger.log(Level.SEVERE, "Error handling offer element request", e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Metoda do wysyłania komunikatu błędu do klienta.
     *
     * @param output Strumień wyjściowy.
     * @param error  Komunikat błędu.
     */
    private void sendError(ObjectOutputStream output, String error) {
        if (output == null) {
            return;
        }
        ErrorPacket err = new ErrorPacket(error);
        err.operationType = NetData.OperationType.Error;
        try {
            output.writeObject(err);
            output.flush();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error sending error message", e);
        }
    }

    /**
     * Metoda sprawdzająca połączenie z bazą danych.
     *
     * @param dbh    Obiekt obsługujący połączenie z bazą danych.
     * @param output Strumień wyjściowy.
     * @return true, jeśli połączenie z bazą danych jest poprawne, w przeciwnym razie false.
     */
    boolean checkDBConnection(DatabaseHandler dbh, ObjectOutputStream output) {
        try {
            if (dbh.conn == null || dbh.conn.isClosed()) {
                if (output != null)
                    sendError(output, "Błąd połączenia z bazą danych.");
                dbh.close();
                return false;
            }
        } catch (SQLException e) {
            if (output != null) {
                sendError(output, "Błąd połączenia z bazą danych.");
            }
            return false;
        }
        return true;
    }

}
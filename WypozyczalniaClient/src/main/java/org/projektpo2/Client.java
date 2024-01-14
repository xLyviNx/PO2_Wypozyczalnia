package org.projektpo2;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import org.projektpo2.controllers.*;
import org.projektpo2.packets.*;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;
/**
 * Klasa reprezentująca klienta w systemie obsługującym wypożyczalnię pojazdów.
 * Klient odpowiada za komunikację z serwerem oraz zarządzanie interfejsem użytkownika.
 */
public class Client {
    private static final Logger logger = Utilities.getLogger(Client.class);
    /**
     * Instancja klienta (singleton).
     */
    public static Client instance;
    /**
     * Czy połączenie klienta jest zamykane.
     */
    public static boolean quitting;

    /**
     * Socket służący do komunikacji z serwerem.
     */
    private Socket socket;

    /**
     * Strumień do odbierania obiektów z serwera.
     */
    private ObjectInputStream input;

    /**
     * Strumień do wysyłania obiektów do serwera.
     */
    private ObjectOutputStream output;
    /**
     * Rozpoczyna proces łączenia z serwerem.
     *
     * @param address Adres serwera.
     * @param port    Numer portu serwera.
     * @throws Exception Wyjątek np. w przypadku problemów z połączeniem.
     */
    public void start(String address, int port) throws Exception {
        if (isInstanceAlreadyRunning()) {
            return;
        }

        waitForWindowInstance();

        try {
            initializeSocket(address, port);
            startPingThread();

            while (socket.isConnected() && isWindowInstanceAvailable()) {
                if (quitting)
                    return;
                NetData data = receiveData();
                if (data != null) {
                    handleReceivedData(data);
                }
            }

            logger.log(Level.INFO, "DISCONNECTED.");
        } catch (ConnectException e) {
            if (quitting) return;
            handleConnectionException(e);
        } catch (IOException | ClassNotFoundException e) {
            if (quitting) return;
            logger.log(Level.SEVERE, "Error during communication with the server: " + e.getMessage(), e);
            throw new DisconnectException();
        } finally {
            closeResources();
            resetInstance();
        }

        logger.log(Level.INFO, "DISCONNECTED.");
    }
    /**
     * Sprawdza, czy już istnieje instancja klienta.
     *
     * @return True, jeśli instancja klienta już istnieje; False w przeciwnym razie.
     */
    private boolean isInstanceAlreadyRunning() {
        return instance != null && instance != this;
    }
    /**
     * Oczekuje na utworzenie instancji okna interfejsu użytkownika.
     */
    private void waitForWindowInstance() {
        while (WypozyczalniaOkno.instance == null) {
            sleep(300);
        }
    }
    /**
     * Inicjalizuje socket i nawiązuje połączenie z serwerem.
     *
     * @param address Adres serwera.
     * @param port    Numer portu serwera.
     * @throws IOException Wyjątek w przypadku problemów związanych z socketem.
     */
    private void initializeSocket(String address, int port) throws IOException {
        instance = this;
        logger.log(Level.INFO, "Connecting to the server at " + address + ":" + port);
        socket = new Socket(address, port);
        output = new ObjectOutputStream(socket.getOutputStream());
        input = new ObjectInputStream(socket.getInputStream());
    }
    /**
     * Rozpoczyna wątek pingujący serwer w celu utrzymania połączenia.
     */
    private void startPingThread() {
        Thread pings = new Thread(() -> {
            while (socket.isConnected() && isWindowInstanceAvailable() && !quitting) {
                try {
                    sendPing();
                    sleep(1000);
                } catch (SocketException socketExc) {
                    handlePingError(socketExc);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        pings.start();
    }
    /**
     * Wstrzymuje wątek na określoną liczbę milisekund.
     *
     * @param millis Czas w milisekundach.
     */
    private void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Sprawdza dostępność instancji okna interfejsu użytkownika.
     *
     * @return True, jeśli instancja okna interfejsu jest dostępna; False w przeciwnym razie.
     */
    private boolean isWindowInstanceAvailable() {
        return WypozyczalniaOkno.instance != null;
    }
    /**
     * Wysyła ping do serwera w celu utrzymania połączenia.
     *
     * @throws IOException Wyjątek w przypadku problemów związanych z socketem.
     */
    private void sendPing() throws IOException
    {
        if (quitting)return;
        output.writeObject(new NetData(NetData.Operation.Ping));
        output.flush();
    }
    /**
     * Obsługuje błąd związany z pingowaniem serwera.
     *
     * @param socketExc Wyjątek związany z socketem.
     */
    private void handlePingError(SocketException socketExc) {
        logger.log(Level.SEVERE, "Error pinging server: " + socketExc.getMessage(), socketExc);
        throw new DisconnectException();
    }
    /**
     * Odbiera dane od serwera.
     *
     * @return Odebrane dane w postaci obiektu NetData.
     * @throws IOException            Wyjątek w przypadku problemów związanych z socketem.
     * @throws ClassNotFoundException Wyjątek w przypadku problemów związanych z klasą.
     */
    private NetData receiveData() throws IOException, ClassNotFoundException {
        return (NetData) input.readObject();
    }
    /**
     * Obsługuje wyjątek związany z problemami podczas nawiązywania połączenia.
     *
     * @param e Wyjątek ConnectException.
     */
    private void handleConnectionException(ConnectException e) {
        logger.log(Level.SEVERE, "Unable to connect to the server: " + e.getMessage());
        throw new DisconnectException();
    }
    /**
     * Zamyka zasoby, takie jak socket i strumienie.
     */
    private void closeResources() {
        closeQuietly(input);
        closeQuietly(output);
        closeQuietly(socket);
        logger.log(Level.INFO, "Closed socket and streams");
    }
    /**
     * Zamyka zasób bez wywoływania wyjątku w przypadku błędu.
     *
     * @param closeable Zasób do zamknięcia.
     */
    private void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error closing resource: " + e.getMessage(), e);
        }
    }
    /**
     * Resetuje instancję klienta, jeśli obecna instancja to ta, która jest resetowana.
     */
    private void resetInstance() {
        if (instance == this) {
            instance = null;
        }
    }
    /**
     * Wysyła żądanie dodania oferty wypożyczalni do serwera.
     *
     * @param brand       Marka oferowanego pojazdu.
     * @param model       Model oferowanego pojazdu.
     * @param year        Rok produkcji oferowanego pojazdu.
     * @param engine      Silnik oferowanego pojazdu.
     * @param price       Cena oferowanego pojazdu.
     * @param desc        Opis oferowanego pojazdu.
     * @param thumbnail   Miniatura oferowanego pojazdu w postaci tablicy bajtów.
     * @param thumbnailname Nazwa pliku miniatury.
     * @param images      Lista obrazów oferowanego pojazdu w postaci tablic bajtów.
     * @param imagesnames Lista nazw plików obrazów.
     * @param ecap        Pojemność silnika oferowanego pojazdu.
     */
    public void RequestAddOffer(String brand, String model, int year, String engine, float price, String desc, byte[] thumbnail, String thumbnailname, ArrayList<byte[]> images, ArrayList<String> imagesnames, int ecap)
    {
        if (brand.length()>32)
        {
            logger.log(Level.WARNING, "Brand cannot be longer than 32 characters");
            MessageBox("Marka nie może być dłuższa niż 32 znaki.", Alert.AlertType.ERROR);
            return;
        }
        if (model.length()>32)
        {
            logger.log(Level.WARNING, "Model cannot be longer than 32 characters");
            MessageBox("Model nie może być dłuższy niż 32 znaki.", Alert.AlertType.ERROR);
            return;
        }
        if (engine.length()>32)
        {
            logger.log(Level.WARNING, "Engine cannot be longer than 32 characters");
            MessageBox("Silnik nie może być dłuższy niż 32 znaki.", Alert.AlertType.ERROR);
            return;
        }

        VehiclePacket data = new VehiclePacket();

        data.brand = brand;
        data.model = model;
        data.year = year;
        data.engine = engine;
        data.price = price;
        data.description = desc;
        data.thumbnailPath = thumbnailname;
        data.imagePaths = imagesnames;
        data.thumbnail = thumbnail;
        data.images.addAll(images);
        data.databaseId=-1;
        data.operation = NetData.Operation.AddOffer;
        data.engineCap = ecap;
        SendRequest(data);

    }
    /**
     * Obsługuje otrzymane dane od serwera.
     *
     * @param data Odebrane dane w postaci obiektu NetData.
     * @throws IOException Wyjątek w przypadku problemów.
     */
    private void handleReceivedData(NetData data) throws IOException {
        try {
            if (data.operationType == NetData.OperationType.Error) {
                handleErrorResponse(data);
            } else {
                handleOtherResponses(data);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error handling received data: " + ex.getMessage(), ex);
        }
    }
    /**
     * Obsługuje błąd w odpowiedzi od serwera.
     *
     * @param data Odebrane dane w postaci obiektu NetData.
     */
    private void handleErrorResponse(NetData data) {
        ErrorPacket ep = (ErrorPacket)data;
        if (ep==null)return;
        if (data.operation == NetData.Operation.ReservationRequest && ReservationController.instance != null) {
            Platform.runLater(() -> ReservationController.instance.but_reserve.setVisible(true));
        }
        logger.log(Level.WARNING, "Error response from server: " + ep.ErrorMessage);
        MessageBox(ep.ErrorMessage, Alert.AlertType.ERROR);
    }
    /**
     * Obsługuje inne rodzaje odpowiedzi od serwera.
     *
     * @param data Odebrane dane w postaci obiektu NetData.
     */
    private void handleOtherResponses(NetData data) {
        switch (data.operation) {
            case Register:
                handleRegisterResponse(data);
                break;
            case Login:
                handleLoginResponse(data);
                break;
            case OfferUsername:
                handleOfferUsernameResponse(data);
                break;
            case OfferElement:
                handleOfferElementResponse(data);
                break;
            case OfferDetails:
                handleOfferDetailsResponse(data);
                break;
            case Logout:
                handleLogoutResponse(data);
                break;
            case ReservationRequest:
                handleReservationRequestResponse(data);
                break;
            case addButton:
                handleAddButtonResponse(data);
                break;
            case AddOffer:
                handleAddOfferResponse(data);
                break;
            case DeleteOffer:
                handleDeleteOfferResponse(data);
                break;
            case ReservationElement:
                handleReservationElementResponse(data);
                break;
            case ManageReservation:
                handleReservationManagementResponse(data);
                break;
            case ConfirmationsButton:
                handleConfirmButton(data);
                break;
            case BrandsList:
                handleBrandsList(data);
                break;
            default:
                break;
        }
    }
    /**
     * Obsługuje odpowiedź serwera z listą marek pojazdów.
     *
     * @param data Odebrane dane w postaci obiektu NetData.
     */
    private void handleBrandsList(NetData data)
    {
        BrandsList blist = (BrandsList) data;
        Platform.runLater(() -> {
            if (OffersController.instance != null) {
                OffersController.instance.filterBrandsParent.getChildren().clear();
                OffersController.instance.AddFilterBrand("KAŻDA");
                for (String br : blist.brands) {
                    if(br.isEmpty()) continue;
                    OffersController.instance.AddFilterBrand(br);
                }
            }
        });

    }
    /**
     * Obsługuje odpowiedź serwera dotyczącą widoczności przycisku potwierdzenia.
     *
     * @param data Odebrane dane w postaci obiektu NetData.
     */
    private void handleConfirmButton(NetData data)
    {
        ConfirmButtonVisibility cbv = (ConfirmButtonVisibility) data;
        if (cbv==null)return;
        Platform.runLater(() -> {
            if (OffersController.instance!=null) {
                HBox hbx = (HBox) OffersController.instance.confirmationsButton.getParent();
                OffersController.instance.confirmationsButton.setVisible(cbv.isVisible);
            }
        });
    }
    /**
     * Metoda do wysyłania żądania dotyczącego widoczności przycisku potwierdzenia.
     */
    public void RequestConfButton()
    {
        NetData req = new NetData(NetData.Operation.ConfirmationsButton);
        SendRequest(req);
    }

    private void handleReservationManagementResponse(NetData data) {
        ManageReservationRequest mreq= (ManageReservationRequest)data;
        if (mreq==null)return;
        Platform.runLater(() -> {
            if (ConfirmationController.instance != null) {
                ConfirmationController.instance.Refresh();
                if (mreq.confirm)
                    MessageBox("Pomyślnie potwierdzono rezerwację.", Alert.AlertType.INFORMATION);
                else
                    MessageBox("Pomyślnie anulowano rezerwację.", Alert.AlertType.INFORMATION);
            }
        });
    }
    private void handleRegisterResponse(NetData data) {
        if (data.operationType == NetData.OperationType.Success) {
            MessageBox("Zarejestrowano.", Alert.AlertType.INFORMATION);
            Platform.runLater(OffersController::openScene);
        }
    }

    private void handleLoginResponse(NetData data) {
        if (data.operationType == NetData.OperationType.Success) {
            MessageBox("Zalogowano.", Alert.AlertType.INFORMATION);
            Platform.runLater(OffersController::openScene);
        }
    }

    private void handleOfferUsernameResponse(NetData data) {
        UsernamePacket up = (UsernamePacket) data;
        if (up != null) {
            OffersController.setUsername(up.username);
        }
    }

    private void handleOfferElementResponse(NetData data)
    {
        VehiclePacket vp = (VehiclePacket) data;
        if (vp!=null) {
            try {
                byte[] imgs = vp.thumbnail!=null ? vp.thumbnail : new byte[0];
                OffersController.AddOfferNode(vp.brand + " " + vp.model + " (" + vp.year + ") " + vp.engine, vp.price, imgs,
                        vp.databaseId, vp.isRented, vp.daysLeft);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Error adding car: " + ex.getMessage(), ex);
            }
        }
    }

    private void handleOfferDetailsResponse(NetData data) {
        if (OfferDetailsController.instance == null) {
            return;
        }

        VehiclePacket vp = (VehiclePacket) data;
        OfferDetailsController offerDetailsController = OfferDetailsController.instance;
        offerDetailsController.updateHeaderAndDetails(vp);
        offerDetailsController.updateDeleteButtonVisibility(vp);
        offerDetailsController.addImages(vp.images);
        offerDetailsController.checkImage();
    }

    private void handleLogoutResponse(NetData data) {
        Platform.runLater(() -> {
            try {
                StartPageController spc = new StartPageController();
                spc.load_scene();
                MessageBox("Wylogowano.", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void handleReservationRequestResponse(NetData data) {
        if (data.operationType == NetData.OperationType.Success) {
            Platform.runLater(() -> {
                MessageBox("Zarezerwowano pojazd. Oczekuj próby kontaktu naszego agenta na twój numer telefonu.", Alert.AlertType.INFORMATION);
                OffersController.openScene();
            });
        }
    }

    private void handleAddButtonResponse(NetData data) {
        AddOfferButtonVisibility vis = (AddOfferButtonVisibility) data;
        Platform.runLater(() -> {
            if (OffersController.instance != null) {
                OffersController.instance.addOfferButton.setVisible(vis.isVisible);
            }
        });
    }

    private void handleAddOfferResponse(NetData data) {
        Platform.runLater(() -> {
            if (data.operationType == NetData.OperationType.Success) {
                MessageBox("Dodano ofertę.", Alert.AlertType.INFORMATION);
                OffersController.openScene();
            } else if (data.operationType == NetData.OperationType.Error) {
                if (AddOfferController.instance != null) {
                    AddOfferController.instance.but_confirm.setVisible(true);
                }
            }
        });
    }

    private void handleDeleteOfferResponse(NetData data) {
        Platform.runLater(() -> {
            if (data.operationType == NetData.OperationType.Success) {
                MessageBox("Usunięto ofertę.", Alert.AlertType.INFORMATION);
                OffersController.openScene();
            }
        });
    }

    private void handleReservationElementResponse(NetData data) {
        ReservationElement element = (ReservationElement) data;
        Platform.runLater(() -> {
            if (element!=null && element.carId!=0 && element.reserveId!=0) {
                if (ConfirmationController.instance != null) {
                    String text = "(" + element.reserveId + ") " + element.firstName + " " + element.lastName + " (" + element.login + ", " + element.phoneNumber + ")\n";
                    text += element.brand + " " + element.model + " (" + element.productionYear + ", ID: " + element.carId + "), CZAS: " + element.reserveDays + " dni, KOSZT: " + String.format("%.2f zł", element.reserveDays * element.dailyPrice) + ".";
                    ConfirmationController.instance.AddButton(text, element.reserveId);
                }
            }
        });
    }
    /**
     * Metoda do wysyłania żądania dotyczącego anulowania rezerwacji.
     *
     * @param id Identyfikator rezerwacji do anulowania.
     */
    public void RequestCancelReservation(int id)
    {
        ManageReservationRequest req = new ManageReservationRequest(id,false);
        SendRequest(req);
    }
    /**
     * Metoda do wysyłania żądania dotyczącego potwierdzenia rezerwacji.
     *
     * @param id Identyfikator rezerwacji do potwierdzenia.
     */
    public void RequestConfirmReservation(int id)
    {
        ManageReservationRequest req = new ManageReservationRequest(id,true);
        SendRequest(req);
    }
    /**
     * Wysyła żądanie dotyczące potwierdzeń rezerwacji do serwera.
     */
    public void RequestConfirmations()
    {
        NetData req = new NetData(NetData.Operation.RequestConfirmations);
        SendRequest(req);
    }
    /**
     * Metoda do wysyłania żądania dotyczącego usuwania oferty.
     *
     * @param id Identyfikator oferty do usunięcia.
     */
    public void RequestDelete(int id)
    {
        DeleteOfferRequestPacket req = new DeleteOfferRequestPacket(id);
        SendRequest(req);
    }
    /**
     * Metoda do wysyłania żądania dotyczącego rezerwacji.
     *
     * @param id   Identyfikator pojazdu.
     * @param days Ilość dni rezerwacji.
     */
    public void RequestReservation(int id, int days)
    {
        ReservationRequestPacket req = new ReservationRequestPacket(id,days);
        SendRequest(req);
    }
    /**
     * Wysyła żądanie szczegółów oferty o określonym identyfikatorze do serwera.
     *
     * @param id Identyfikator oferty.
     */
    public void RequestOffer(int id)
    {
        OfferDetailsRequestPacket request = new OfferDetailsRequestPacket(id);
        SendRequest(request);
    }
    /**
     * Wysyła żądanie rejestracji nowego użytkownika do serwera.
     *
     * @param username    Nazwa użytkownika.
     * @param pwd         Hasło użytkownika.
     * @param pwdR        Powtórzone hasło użytkownika w celu potwierdzenia.
     * @param phone       Numer telefonu użytkownika.
     * @param imie        Imię użytkownika.
     * @param nazwisko    Nazwisko użytkownika.
     * @throws DisconnectException Wyjątek w przypadku problemów z połączeniem.
     */
    public void RequestRegister(String username, String pwd, String pwdR, int phone, String imie, String nazwisko)
            throws DisconnectException {
        RegisterPacket request = new RegisterPacket();
        request.login=username;
        request.password = (MD5Encryptor.encryptPassword(pwd));
        request.repeat_password = (MD5Encryptor.encryptPassword(pwdR));
        request.phonenumber=phone;
        request.imie=imie;
        request.nazwisko=nazwisko;
        SendRequest(request);
    }
    /**
     * Wysyła żądanie wylogowania do serwera.
     */
    public void SendLogout()
    {
        NetData request = new NetData(NetData.Operation.Logout);
        SendRequest(request);

    }
    /**
     * Wysyła żądanie logowania do serwera.
     *
     * @param username Nazwa użytkownika.
     * @param pwd      Hasło użytkownika.
     */
    public void RequestLogin(String username, String pwd) {
        LoginPacket request = new LoginPacket(username, MD5Encryptor.encryptPassword(pwd));
        SendRequest(request);
    }
    /**
     * Wysyła żądanie pobrania nazwy użytkownika do serwera.
     */
    public void RequestUsername() {
        NetData req = new NetData(NetData.Operation.OfferUsername);
        SendRequest(req);
    }
    /**
     * Wysyła żądanie pobrania ofert pojazdów spełniających określone kryteria do serwera.
     *
     * @param brand        Marka pojazdu.
     * @param yearMin      Minimalny rok produkcji.
     * @param yearMax      Maksymalny rok produkcji.
     * @param engineCapMin Minimalna pojemność silnika.
     * @param engineCapMax Maksymalna pojemność silnika.
     * @param priceMin     Minimalna cena.
     * @param priceMax     Maksymalna cena.
     * @param priceDESC    True, jeśli sortowanie według ceny ma być malejące; False w przeciwnym razie.
     */
    public void RequestOffers(String brand, int yearMin, int yearMax, int engineCapMin, int engineCapMax, float priceMin, float priceMax, boolean priceDESC) {
        FilteredOffersRequestPacket request = new FilteredOffersRequestPacket(brand, yearMin, yearMax, engineCapMin, engineCapMax, priceMin, priceMax, priceDESC);
        SendRequest(request);
    }
    /**
     * Wysyła ogólne żądanie do serwera.
     *
     * @param request Obiekt reprezentujący żądanie do wysłania.
     * @throws DisconnectException Wyjątek w przypadku problemów z połączeniem.
     */
    void SendRequest(Object request) throws DisconnectException {
        if (!(request instanceof NetData))
            return;
        if (output != null && socket != null) {
            try {
                output.writeObject(request);
                output.flush();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Błąd podczas wysyłania żądania.", e);
                if (WypozyczalniaOkno.instance != null)
                    WypozyczalniaOkno.instance.NoConnection();
                throw new DisconnectException();
            }
        }
    }
    /**
     * Wyświetla okno dialogowe z informacją.
     *
     * @param content Treść informacji.
     * @param mtype   Typ okna dialogowego (ERROR, INFORMATION, itp.).
     */
    public static void MessageBox(String content, Alert.AlertType mtype) {
        Platform.runLater(() -> {
            Alert errorAlert = new Alert(mtype);
            errorAlert.setTitle(mtype == Alert.AlertType.ERROR ? "BŁĄD" : "Informacja");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText(content);
            errorAlert.show();
        });
    }
    /**
     * Wysyła do serwera informację o zamknięciu aplikacji.
     */
    public void SendQuit()
    {
        quitting=true;
        NetData quitpacket = new NetData(NetData.Operation.Exit);
        SendRequest(quitpacket);
    }
}

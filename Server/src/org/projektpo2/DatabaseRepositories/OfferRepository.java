package org.projektpo2.DatabaseRepositories;

import org.projektpo2.DatabaseHandler;
import org.projektpo2.ServerMain;
import org.projektpo2.User;
import org.projektpo2.Utilities;
import org.projektpo2.packets.FilteredOffersRequestPacket;
import org.projektpo2.packets.NetData;
import org.projektpo2.packets.VehiclePacket;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasa odpowiedzialna za operacje związane z ofertami w bazie danych.
 */
public class OfferRepository {
    private final DatabaseHandler dbh;
    private static final Logger logger = Logger.getLogger(OfferRepository.class.getName());

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     *
     * @param dbh Obiekt obsługujący połączenie z bazą danych.
     */
    public OfferRepository(DatabaseHandler dbh) {
        this.dbh = dbh;
    }

    /**
     * Usuwa ofertę o podanym identyfikatorze wraz z powiązanymi zdjęciami.
     *
     * @param offerId Identyfikator oferty do usunięcia.
     * @return true, jeśli usunięcie powiodło się, w przeciwnym razie false.
     * @throws SQLException Błąd SQL podczas wykonania zapytania.
     */
    public boolean deleteOffer(int offerId) throws SQLException {
        String query = "SELECT zdjecie, wiekszeZdjecia FROM auta WHERE id_auta = ?";
        try (PreparedStatement selectStatement = dbh.conn.prepareStatement(query)) {
            selectStatement.setInt(1, offerId);
            ResultSet resultSet = selectStatement.executeQuery();

            if (resultSet.next()) {
                String thumbnail = resultSet.getString("zdjecie");
                String[] photosIndividual = resultSet.getString("wiekszeZdjecia").split(";");

                query = "DELETE FROM auta WHERE id_auta = ?";
                try (PreparedStatement deleteStatement = dbh.conn.prepareStatement(query)) {
                    deleteStatement.setInt(1, offerId);
                    int rowsDeleted = deleteStatement.executeUpdate();

                    if (rowsDeleted > 0) {
                        deleteImages(thumbnail, photosIndividual);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting offer", e);
        }

        return false;
    }

    /**
     * Usuwa zdjęcia powiązane z ofertą z systemu plików.
     *
     * @param thumbnail      Ścieżka do miniaturki.
     * @param photosIndividual Tablica ścieżek do zdjęć.
     */
    private void deleteImages(String thumbnail, String[] photosIndividual) {
        String folderPath = ServerMain.imagePath;
        if (!thumbnail.isEmpty())
            deleteImageIfExists(folderPath + thumbnail);
        for (String photo : photosIndividual) {
            if (photo.isEmpty())
                continue;
            deleteImageIfExists(folderPath + photo);
        }
    }

    /**
     * Usuwa zdjęcie, jeśli istnieje.
     *
     * @param filePath Ścieżka do pliku.
     */
    private void deleteImageIfExists(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
            logger.log(Level.INFO, "File deleted successfully: {0}", filePath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to delete the image " + filePath + ": " + e.getMessage());
        }
    }

    /**
     * Dodaje nową ofertę do bazy danych wraz z powiązanymi zdjęciami.
     *
     * @param vp      Pakiet danych oferty.
     * @param session Sesja użytkownika.
     * @return true, jeśli dodanie oferty powiodło się, w przeciwnym razie false.
     */
    public boolean addOffer(VehiclePacket vp, User session) {
        String thumbname = generateThumbnail(vp.thumbnail, vp.thumbnailPath, session.username);

        String dbPhotos = generatePhotoPaths(vp.imagePaths, vp.images, session.username);

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

            int queryResult = preparedStatement.executeUpdate();

            if (queryResult > 0) {
                saveImages(vp.thumbnail, vp.thumbnailPath, session.username);
                saveAdditionalImages(vp.imagePaths, vp.images, session.username);
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error adding offer", e);
        }

        return false;
    }

    /**
     * Generuje nazwę pliku dla miniaturek i zapisuje je do systemu plików.
     *
     * @param thumbnail    Miniatura w formie bajtów.
     * @param thumbnailPath Ścieżka do miniaturek.
     * @param username      Nazwa użytkownika.
     * @return Wygenerowana nazwa pliku dla miniaturek.
     */
    private String generateThumbnail(byte[] thumbnail, String thumbnailPath, String username) {
        if (thumbnail != null && thumbnail.length > 0) {
            String thumbname = "user/" + username + "/" + thumbnailPath;
            if (thumbname.length() <= 64 && !fileExists(thumbname)) {
                saveImage(thumbnail, thumbname, username);
                return thumbname;
            }
        }
        return "";
    }

    /**
     * Generuje ścieżki do zdjęć dla bazy danych.
     *
     * @param imagePaths Ścieżki do zdjęć.
     * @param images     Zdjęcia w formie bajtów.
     * @param username   Nazwa użytkownika.
     * @return Zbiorcza ścieżka do zdjęć w bazie danych.
     */
    private String generatePhotoPaths(ArrayList<String> imagePaths, ArrayList<byte[]> images, String username) {
        StringBuilder dbPhotos = new StringBuilder();
        if (!imagePaths.isEmpty() && images.size() == imagePaths.size()) {
            for (int i = 0; i < imagePaths.size(); i++) {
                String photoname = "user/" + username + "/" + imagePaths.get(i);
                if (photoname.length() <= 256 && !fileExists(photoname)) {
                    dbPhotos.append(photoname).append(";");
                    saveImage(images.get(i), photoname, username);
                }
            }
        }
        return dbPhotos.toString();
    }

    /**
     * Zapisuje główne zdjęcia oraz ich miniaturek do systemu plików.
     *
     * @param image    Zdjęcie w formie bajtów.
     * @param imagePath Ścieżka do zdjęcia.
     * @param username  Nazwa użytkownika.
     */
    private void saveImages(byte[] image, String imagePath, String username) {
        logger.log(Level.INFO, "Saving image: {0}", image);
        if (image != null && image.length > 0 && imagePath != null) {
            saveImage(image, "user/" + username + "/" + imagePath, username);
        }
    }

    /**
     * Zapisuje dodatkowe zdjęcia do systemu plików.
     *
     * @param imagePaths Ścieżki do dodatkowych zdjęć.
     * @param images     Zdjęcia w formie bajtów.
     * @param username   Nazwa użytkownika.
     */
    private void saveAdditionalImages(ArrayList<String> imagePaths, ArrayList<byte[]> images, String username) {
        if (imagePaths != null && images.size() == imagePaths.size()) {
            for (int i = 0; i < imagePaths.size(); i++) {
                saveImages(images.get(i), imagePaths.get(i), username);
            }
        }
    }

    /**
     * Zapisuje zdjęcie do systemu plików.
     *
     * @param image    Zdjęcie w formie bajtów.
     * @param imagePath Ścieżka do zdjęcia.
     * @param username  Nazwa użytkownika.
     */
    private void saveImage(byte[] image, String imagePath, String username) {
        try {
            Files.createDirectories(Paths.get(ServerMain.imagePath, "user", username));
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(image));
            ImageIO.write(bufferedImage, "jpg", new File(ServerMain.imagePath + File.separator + imagePath));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error saving image: " + imagePath, e);
        }
    }

    /**
     * Sprawdza, czy plik istnieje.
     *
     * @param filePath Ścieżka do pliku.
     * @return true, jeśli plik istnieje, w przeciwnym razie false.
     */
    private boolean fileExists(String filePath) {
        URL resourceUrl = ServerMain.class.getResource("/img/" + filePath);
        return Utilities.fileExists(resourceUrl);
    }

    /**
     * Pobiera szczegóły oferty o podanym identyfikatorze.
     *
     * @param offerId Identyfikator oferty.
     * @param session Sesja użytkownika.
     * @return Pakiet danych szczegółów oferty.
     * @throws SQLException Błąd SQL podczas pobierania danych z bazy.
     * @throws IOException  Błąd wejścia/wyjścia podczas przetwarzania zdjęć.
     */
    public VehiclePacket getOfferDetails(int offerId, User session) throws SQLException, IOException {
        String query = "SELECT * FROM `auta` WHERE `id_auta` = ?";
        try (PreparedStatement statement = dbh.conn.prepareStatement(query)) {
            statement.setInt(1, offerId);

            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return mapResultSetToVehiclePacket(result, session);
            } else {
                return null; // Brak oferty o podanym identyfikatorze.
            }
        }
    }

    /**
     * Mapuje dane z wyniku zapytania SQL na obiekt pakietu danych o pojeździe.
     *
     * @param result  Wynik zapytania SQL.
     * @param session Sesja użytkownika.
     * @return Pakiet danych o pojeździe.
     * @throws SQLException Błąd SQL podczas mapowania danych.
     * @throws IOException  Błąd wejścia/wyjścia podczas przetwarzania zdjęć.
     */
    private VehiclePacket mapResultSetToVehiclePacket(ResultSet result, User session) throws SQLException, IOException {
        VehiclePacket response = new VehiclePacket();
        response.operation = NetData.Operation.OfferDetails;

        mapVehicleDetails(result, response, session);
        mapVehicleImages(result, response);

        return response;
    }

    /**
     * Mapuje szczegóły pojazdu z wyniku zapytania SQL na obiekt pakietu danych o pojeździe.
     *
     * @param result   Wynik zapytania SQL.
     * @param response Pakiet danych o pojeździe.
     * @param session  Sesja użytkownika.
     * @throws SQLException Błąd SQL podczas mapowania danych.
     */
    private void mapVehicleDetails(ResultSet result, VehiclePacket response, User session) throws SQLException {
        response.brand = result.getString("marka");
        response.model = result.getString("model");
        response.engine = result.getString("silnik");
        response.year = result.getInt("rok_prod");
        response.price = result.getFloat("cenaZaDzien");
        response.description = result.getString("opis");
        response.databaseId = result.getInt("id_auta");
        response.engineCap = result.getInt("pojemnosc");
        response.canBeDeleted = session.isSignedIn && !session.username.isEmpty() && session.canDeleteOffers;
    }

    /**
     * Mapuje zdjęcia pojazdu z wyniku zapytania SQL na obiekt pakietu danych o pojeździe.
     *
     * @param result   Wynik zapytania SQL.
     * @param response Pakiet danych o pojeździe.
     * @throws SQLException Błąd SQL podczas mapowania danych.
     * @throws IOException  Błąd wejścia/wyjścia podczas przetwarzania zdjęć.
     */
    private void mapVehicleImages(ResultSet result, VehiclePacket response) throws SQLException, IOException {
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
    }
    /**
     * Pobiera listę ofert spełniających kryteria filtrowania.
     *
     * @param requestPacket Pakiet żądania filtrowania.
     * @param session       Sesja użytkownika.
     * @return Lista ofert spełniających kryteria filtrowania.
     */
    public List<VehiclePacket> getFilteredOffers(FilteredOffersRequestPacket requestPacket, User session) {
        List<VehiclePacket> offers = new ArrayList<>();

        try {
            String query = buildQuery(requestPacket);
            try {
                PreparedStatement preparedStatement = dbh.conn.prepareStatement(query);
                setParameters(preparedStatement, requestPacket, session);
                ResultSet result = preparedStatement.executeQuery();
                while (result.next()) {
                    VehiclePacket response = extractOfferFromResultSet(result);
                    offers.add(response);
                }
            } catch (SQLException | IOException e) {
                logger.log(Level.SEVERE, "Error retrieving filtered offers", e);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error in getFilteredOffers", e);
            throw new RuntimeException(e);
        }
        return offers;
    }
    /**
     * Buduje zapytanie SQL na podstawie kryteriów filtrowania ofert.
     *
     * @param requestPacket Pakiet żądania filtrowania.
     * @return Zbudowane zapytanie SQL.
     */
    private String buildQuery(FilteredOffersRequestPacket requestPacket) {
        StringBuilder queryBuilder = new StringBuilder("SELECT " +
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
                "    ) ");

        appendBrandCondition(queryBuilder, requestPacket);
        appendYearCondition(queryBuilder, requestPacket);
        appendEngineCapCondition(queryBuilder, requestPacket);
        appendPriceCondition(queryBuilder, requestPacket);

        queryBuilder.append("ORDER BY a.`cenaZaDzien`").append(requestPacket.priceDESC ? "DESC" : "ASC").append(";");
        return queryBuilder.toString();
    }

    /**
     * Dodaje warunek dotyczący marki do zapytania SQL.
     *
     * @param queryBuilder Builder zapytania SQL.
     * @param requestPacket Pakiet żądania filtrowania.
     */
    private void appendBrandCondition(StringBuilder queryBuilder, FilteredOffersRequestPacket requestPacket) {
        if (requestPacket.brand != null && !requestPacket.brand.isEmpty() && !requestPacket.brand.equals("KAŻDA")) {
            queryBuilder.append("AND a.`marka` = ? ");
        }
    }
    /**
     * Dodaje warunek dotyczący roku produkcji do zapytania SQL.
     *
     * @param queryBuilder Builder zapytania SQL.
     * @param requestPacket Pakiet żądania filtrowania.
     */
    private void appendYearCondition(StringBuilder queryBuilder, FilteredOffersRequestPacket requestPacket) {
        if (requestPacket.yearMin != -1) {
            queryBuilder.append("AND a.`rok_prod` >= ? ");
        }

        if (requestPacket.yearMax != -1) {
            queryBuilder.append("AND a.`rok_prod` <= ? ");
        }
    }
    /**
     * Dodaje warunek dotyczący pojemności silnika do zapytania SQL.
     *
     * @param queryBuilder Builder zapytania SQL.
     * @param requestPacket Pakiet żądania filtrowania.
     */
    private void appendEngineCapCondition(StringBuilder queryBuilder, FilteredOffersRequestPacket requestPacket) {
        if (requestPacket.engineCapMin != -1) {
            queryBuilder.append("AND a.`pojemnosc` >= ? ");
        }

        if (requestPacket.engineCapMax != -1) {
            queryBuilder.append("AND a.`pojemnosc` <= ? ");
        }
    }
    /**
     * Dodaje warunek dotyczący ceny do zapytania SQL.
     *
     * @param queryBuilder Builder zapytania SQL.
     * @param requestPacket Pakiet żądania filtrowania.
     */
    private void appendPriceCondition(StringBuilder queryBuilder, FilteredOffersRequestPacket requestPacket) {
        if (requestPacket.priceMin != -1) {
            queryBuilder.append("AND a.`cenaZaDzien` >= ? ");
        }

        if (requestPacket.priceMax != -1) {
            queryBuilder.append("AND a.`cenaZaDzien` <= ? ");
        }
    }
    /**
     * Ustawia parametry zapytania SQL na podstawie kryteriów filtrowania.
     *
     * @param preparedStatement Gotowe zapytanie SQL.
     * @param requestPacket     Pakiet żądania filtrowania.
     * @param session           Sesja użytkownika.
     * @throws SQLException Błąd SQL podczas ustawiania parametrów.
     */
    private void setParameters(PreparedStatement preparedStatement, FilteredOffersRequestPacket requestPacket, User session) throws SQLException {
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
    }
    /**
     * Pobiera zbiór marek pojazdów spełniających kryteria filtrowania.
     *
     * @param requestPacket Pakiet z żądaniem filtrowania ofert.
     * @param session Obiekt użytkownika z bieżącą sesją.
     * @return Zbiór marek pojazdów spełniających kryteria filtrowania.
     */
    public HashSet<String> getFilteredBrands(FilteredOffersRequestPacket requestPacket, User session) {
        HashSet<String> filteredBrands = new HashSet<>();
        try {
            String brandQuery = buildBrandQuery();
            PreparedStatement brandStatement = dbh.conn.prepareStatement(brandQuery);
            setBrandParameters(brandStatement, session);
            ResultSet brandResult = brandStatement.executeQuery();

            while (brandResult.next()) {
                filteredBrands.add(brandResult.getString("marka"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return filteredBrands;
    }
    /**
     * Metoda budująca zapytanie dotyczące marek pojazdów.
     *
     * @return Zbudowane zapytanie w formie ciągu znaków.
     */
    private String buildBrandQuery() {
        return "SELECT DISTINCT a.`marka` FROM `auta` a " +
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
    }

    /**
     * Metoda ustawiająca parametry zapytania dotyczącego marek pojazdów.
     *
     * @param brandStatement Obiekt PreparedStatement do ustawienia parametrów.
     * @param session        Obiekt reprezentujący sesję użytkownika.
     * @throws SQLException Jeśli wystąpi błąd SQL.
     */
    private void setBrandParameters(PreparedStatement brandStatement, User session) throws SQLException {
        try {
            brandStatement.setString(1, session.username);
            brandStatement.setString(2, session.username);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Błąd podczas ustawiania parametrów zapytania marek", e);
            throw e;
        }
    }
    /**
     * Metoda ekstrahująca dane oferty z wyniku zapytania do bazy danych.
     *
     * @param result Wynik zapytania do bazy danych.
     * @return Obiekt reprezentujący ofertę pojazdu.
     * @throws SQLException Jeśli wystąpi błąd SQL.
     * @throws IOException  Jeśli wystąpi błąd podczas operacji wejścia/wyjścia.
     */
    private VehiclePacket extractOfferFromResultSet(ResultSet result) throws SQLException, IOException {
        VehiclePacket response = new VehiclePacket();
        int id = result.getInt("id_auta");
        String marka = result.getString("marka");
        String model = result.getString("model");
        String silnik = result.getString("silnik");
        int prod = result.getInt("rok_prod");
        float cena = result.getFloat("cenaZaDzien");
        int poj = result.getInt("pojemnosc");
        response.brand = marka;
        response.model = model;
        response.engine = silnik;
        response.year = prod;
        response.price = cena;
        response.databaseId = id;
        if (!result.wasNull())
            response.engineCap = poj;
        int idWypo = result.getInt("id_wypozyczenia");
        if (!result.wasNull()) {
            Date dataWypo = result.getDate("data_wypozyczenia");
            if (result.wasNull()) {
                response.daysLeft = -1;
            } else {
                int daysLeft = result.getInt("dni_pozostale");
                if (!result.wasNull()) {
                    response.isRented = true;
                    response.daysLeft = daysLeft;
                }
            }
        }
        try {
            String zdjecie = result.getString("zdjecie");
            if (zdjecie != null && !zdjecie.isEmpty()) {
                byte[] img = Utilities.loadImageAsBytes(zdjecie, false);
                response.thumbnail = img;
            }
        } catch (Exception ex) {
            System.err.println(ex.getLocalizedMessage());
        }
        return response;
    }
}

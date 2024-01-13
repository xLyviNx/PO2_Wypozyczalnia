package src.DatabaseRepositories;

import org.projektpo2.*;
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

public class OfferRepository {
    private final DatabaseHandler dbh;

    public OfferRepository(DatabaseHandler dbh) {
        this.dbh = dbh;
    }

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
        }

        return false;
    }

    private void deleteImages(String thumbnail, String[] photos) {
        String folderPath = ServerMain.imagePath;
        if (!thumbnail.isEmpty())
            deleteImageIfExists(folderPath + thumbnail);
        for (String photo : photos) {
            if (photo.isEmpty())
                continue;
            deleteImageIfExists(folderPath + photo);
        }
    }

    private void deleteImageIfExists(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
            System.out.println("File deleted successfully: " + filePath);
        } catch (IOException e) {
            System.err.println("Unable to delete the image " + filePath + ": " + e.getMessage());
        }
    }

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
            e.printStackTrace();
        }

        return false;
    }

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

    private void saveImages(byte[] image, String imagePath, String username) {
        System.out.println(image);
        if (image != null && image.length>0 && imagePath!=null)
        {
            saveImage(image, "user/" + username + "/" + imagePath, username);
        }
    }

    private void saveAdditionalImages(ArrayList<String> imagePaths, ArrayList<byte[]> images, String username) {
        if (imagePaths != null && images.size() == imagePaths.size()) {
            for (int i = 0; i < imagePaths.size(); i++) {
                saveImages(images.get(i), imagePaths.get(i), username);
            }
        }
    }

    private void saveImage(byte[] image, String imagePath, String username) {
        try {
            Files.createDirectories(Paths.get(ServerMain.imagePath, "user", username));
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(image));
            ImageIO.write(bufferedImage, "jpg", new File(ServerMain.imagePath + File.separator + imagePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean fileExists(String filePath) {
        URL resourceUrl = Server.class.getResource("/img/" + filePath);
        return Utilities.fileExists(resourceUrl);
    }

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

    private VehiclePacket mapResultSetToVehiclePacket(ResultSet result, User session) throws SQLException, IOException {
        VehiclePacket response = new VehiclePacket();
        response.operation = NetData.Operation.OfferDetails;

        mapVehicleDetails(result, response, session);
        mapVehicleImages(result, response);

        return response;
    }

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
                e.printStackTrace();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return offers;
    }

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

    private void appendBrandCondition(StringBuilder queryBuilder, FilteredOffersRequestPacket requestPacket) {
        if (requestPacket.brand != null && !requestPacket.brand.isEmpty() && !requestPacket.brand.equals("KAŻDA")) {
            queryBuilder.append("AND a.`marka` = ? ");
        }
    }

    private void appendYearCondition(StringBuilder queryBuilder, FilteredOffersRequestPacket requestPacket) {
        if (requestPacket.yearMin != -1) {
            queryBuilder.append("AND a.`rok_prod` >= ? ");
        }

        if (requestPacket.yearMax != -1) {
            queryBuilder.append("AND a.`rok_prod` <= ? ");
        }
    }

    private void appendEngineCapCondition(StringBuilder queryBuilder, FilteredOffersRequestPacket requestPacket) {
        if (requestPacket.engineCapMin != -1) {
            queryBuilder.append("AND a.`pojemnosc` >= ? ");
        }

        if (requestPacket.engineCapMax != -1) {
            queryBuilder.append("AND a.`pojemnosc` <= ? ");
        }
    }

    private void appendPriceCondition(StringBuilder queryBuilder, FilteredOffersRequestPacket requestPacket) {
        if (requestPacket.priceMin != -1) {
            queryBuilder.append("AND a.`cenaZaDzien` >= ? ");
        }

        if (requestPacket.priceMax != -1) {
            queryBuilder.append("AND a.`cenaZaDzien` <= ? ");
        }
    }

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

    private void setBrandParameters(PreparedStatement brandStatement, User session) throws SQLException {
        brandStatement.setString(1, session.username);
        brandStatement.setString(2, session.username);
    }
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

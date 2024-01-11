package src.DatabaseRepositories;

import src.DatabaseHandler;
import src.packets.ReservationElement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConfirmationRepository {
    private final DatabaseHandler dbh;

    public ConfirmationRepository(DatabaseHandler dbh) {
        this.dbh = dbh;
    }

    public List<ReservationElement> getUnconfirmedReservations() throws SQLException {
        String query = "SELECT wyp.id_wypozyczenia, wyp.days, car.marka, car.model, car.rok_prod, car.id_auta, " +
                "car.cenaZaDzien, uzy.login, uzy.imie, uzy.nazwisko, uzy.numer_telefonu " +
                "FROM wypozyczenie wyp " +
                "INNER JOIN auta car ON wyp.auta_id_auta = car.id_auta " +
                "INNER JOIN uzytkownicy uzy ON wyp.uzytkownicy_id_uzytkownika = uzy.id_uzytkownika " +
                "WHERE wyp.data_wypozyczenia IS NULL;";

        List<ReservationElement> reservations = new ArrayList<>();
        ResultSet results = dbh.executeQuery(query);

        while (results != null && results.next()) {
            ReservationElement reservation = mapResultSetToReservation(results);
            reservations.add(reservation);
        }

        return reservations;
    }

    private ReservationElement mapResultSetToReservation(ResultSet resultSet) throws SQLException {
        ReservationElement reservation = new ReservationElement();
        reservation.reserveId = resultSet.getInt("id_wypozyczenia");
        reservation.reserveDays = resultSet.getInt("days");
        reservation.brand = resultSet.getString("marka");
        reservation.model = resultSet.getString("model");
        reservation.productionYear = resultSet.getInt("rok_prod");
        reservation.carId = resultSet.getInt("id_auta");
        reservation.dailyPrice = resultSet.getFloat("cenaZaDzien");
        reservation.login = resultSet.getString("login");
        reservation.firstName = resultSet.getString("imie");
        reservation.lastName = resultSet.getString("nazwisko");
        reservation.phoneNumber = resultSet.getInt("numer_telefonu");

        return reservation;
    }
}

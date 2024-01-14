package org.projektpo2.DatabaseRepositories;

import org.projektpo2.DatabaseHandler;
import org.projektpo2.packets.ReservationElement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasa odpowiedzialna za operacje związane z potwierdzaniem rezerwacji w bazie danych.
 */
public class ConfirmationRepository {
    private final DatabaseHandler dbh;
    private static final Logger logger = Logger.getLogger(ConfirmationRepository.class.getName());

    /**
     * Konstruktor inicjalizujący obiekt klasy.
     *
     * @param dbh Obiekt obsługujący połączenie z bazą danych.
     */
    public ConfirmationRepository(DatabaseHandler dbh) {
        this.dbh = dbh;
    }

    /**
     * Pobiera listę niepotwierdzonych rezerwacji z bazy danych.
     *
     * @return Lista niepotwierdzonych rezerwacji.
     * @throws SQLException Błąd SQL podczas wykonywania zapytania.
     */
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
            try {
                ReservationElement reservation = mapResultSetToReservation(results);
                reservations.add(reservation);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error mapping ResultSet to ReservationElement", e);
            }
        }

        return reservations;
    }

    /**
     * Mapuje wynik zapytania SQL na obiekt rezerwacji.
     *
     * @param resultSet Wynik zapytania SQL.
     * @return Obiekt rezerwacji.
     * @throws SQLException Błąd SQL podczas mapowania wyniku.
     */
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

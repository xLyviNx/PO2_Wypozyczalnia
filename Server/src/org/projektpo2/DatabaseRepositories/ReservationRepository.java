package org.projektpo2.DatabaseRepositories;

import org.projektpo2.DatabaseHandler;
import org.projektpo2.Utilities;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasa obsługująca operacje na rezerwacjach pojazdów w bazie danych.
 */
public class ReservationRepository {
    private static final Logger logger = Utilities.getLogger(ReservationRepository.class);
    private final DatabaseHandler dbh;

    /**
     * Konstruktor klasy ReservationRepository.
     *
     * @param dbh Obiekt obsługujący połączenie z bazą danych.
     */
    public ReservationRepository(DatabaseHandler dbh) {
        this.dbh = dbh;
    }

    /**
     * Potwierdza rezerwację o podanym identyfikatorze.
     *
     * @param reservationId Identyfikator rezerwacji do potwierdzenia.
     * @return True, jeśli potwierdzenie przebiegło pomyślnie, w przeciwnym razie false.
     */
    public boolean confirmReservation(int reservationId) {
        String query = "UPDATE wypozyczenie SET data_wypozyczenia = NOW() WHERE id_wypozyczenia = ?";
        try {
            return dbh.executeUpdate(query, preparedStatement -> preparedStatement.setInt(1, reservationId));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Błąd podczas potwierdzania rezerwacji", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Usuwa/anuluje rezerwację o podanym identyfikatorze.
     *
     * @param reservationId Identyfikator rezerwacji do usunięcia.
     * @return True, jeśli usunięcie przebiegło pomyślnie, w przeciwnym razie false.
     */
    public boolean deleteReservation(int reservationId) {
        String query = "DELETE FROM wypozyczenie WHERE id_wypozyczenia = ?";
        try {
            return dbh.executeUpdate(query, preparedStatement -> preparedStatement.setInt(1, reservationId));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Błąd podczas usuwania rezerwacji", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Tworzy nową rezerwację dla użytkownika o podanym loginie i pojazdu o podanym identyfikatorze.
     *
     * @param username  Login użytkownika dokonującego rezerwacji.
     * @param vehicleId Identyfikator pojazdu, który ma być zarezerwowany.
     * @param days      Liczba dni rezerwacji.
     * @throws SQLException W przypadku błędu podczas komunikacji z bazą danych.
     */
    public void makeReservation(String username, int vehicleId, int days) throws SQLException {
        String query = "INSERT INTO wypozyczenie (`uzytkownicy_id_uzytkownika`, `auta_id_auta`, `days`)" +
                " SELECT id_uzytkownika, ?, ? FROM uzytkownicy" +
                " WHERE login = ?";

        try (PreparedStatement insertStatement = dbh.conn.prepareStatement(query)) {
            insertStatement.setInt(1, vehicleId);
            insertStatement.setInt(2, days);
            insertStatement.setString(3, username);

            int res = insertStatement.executeUpdate();
            if (res <= 0) {
                throw new SQLException("Nie udało się zarezerwować pojazdu. Spróbuj ponownie później.");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Błąd podczas tworzenia rezerwacji", e);
            throw e;
        }
    }

    /**
     * Sprawdza, czy istnieje rezerwacja dla pojazdu o podanym identyfikatorze.
     *
     * @param id Identyfikator pojazdu.
     * @return True, jeśli istnieje rezerwacja, w przeciwnym razie false.
     */
    public boolean reservationExists(int id) {
        String query = "SELECT id_wypozyczenia FROM wypozyczenie WHERE auta_id_auta = ?";
        try (PreparedStatement selectStatement = dbh.conn.prepareStatement(query)) {
            selectStatement.setInt(1, id);
            ResultSet resultSet = selectStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Błąd podczas sprawdzania istnienia rezerwacji", e);
            return true;
        }
    }
}

package src.DatabaseRepositories;

import org.projektpo2.DatabaseHandler;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReservationRepository {
    private final DatabaseHandler dbh;

    public ReservationRepository(DatabaseHandler dbh) {
        this.dbh = dbh;
    }


    public boolean confirmReservation(int reservationId) {
        String query = "UPDATE wypozyczenie SET data_wypozyczenia = NOW() WHERE id_wypozyczenia = ?";
        try {
            return dbh.executeUpdate(query, preparedStatement -> preparedStatement.setInt(1, reservationId));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deleteReservation(int reservationId) {
        String query = "DELETE FROM wypozyczenie WHERE id_wypozyczenia = ?";
        try {
            return dbh.executeUpdate(query, preparedStatement -> preparedStatement.setInt(1, reservationId));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
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
        }
    }

    public boolean reservationExists(int id) {
        String query = "SELECT id_wypozyczenia FROM wypozyczenie WHERE auta_id_auta = ?";
        try (PreparedStatement selectStatement = dbh.conn.prepareStatement(query)) {
            selectStatement.setInt(1, id);
            ResultSet resultSet = selectStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

}

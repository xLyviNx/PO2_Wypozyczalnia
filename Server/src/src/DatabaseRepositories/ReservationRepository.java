package src.DatabaseRepositories;

import src.DatabaseHandler;

import java.sql.PreparedStatement;
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
}

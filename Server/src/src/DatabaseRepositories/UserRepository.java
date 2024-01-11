package src.DatabaseRepositories;

import src.DatabaseHandler;
import src.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepository {
    private final DatabaseHandler dbh;

    public UserRepository(DatabaseHandler dbh) {
        this.dbh = dbh;
    }

    public void fetchUserPermissions(User session) {
        if (!session.isSignedIn) {
            return;
        }

        try {
            if (!checkDBConnection()) {
                return;
            }

            String query = "SELECT typ.`dodajogloszenia`, typ.`wypozyczauto`, typ.`usunogloszenie`, typ.`manageReservations` FROM typy_uzytkownikow typ INNER JOIN uzytkownicy uz ON typ.id_typu = uz.typy_uzytkownikow_id_typu WHERE uz.login = ?";
            setUserPermissions(session, query);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private boolean checkDBConnection() {
        return dbh != null && dbh.conn != null;
    }

    private void setUserPermissions(User session, String query) throws SQLException {
        try (PreparedStatement selectStatement = dbh.conn.prepareStatement(query)) {
            selectStatement.setString(1, session.username);
            ResultSet rsS = selectStatement.executeQuery();
            while (rsS.next()) {
                session.canReserve = rsS.getBoolean("wypozyczauto");
                session.canAddOffers = rsS.getBoolean("dodajogloszenia");
                session.canDeleteOffers = rsS.getBoolean("usunogloszenie");
                session.canManageReservations = rsS.getBoolean("manageReservations");
                break;
            }
        }
    }
    public boolean isUserExists(String login, int phoneNumber) throws SQLException {
        String existsQuery = "SELECT `id_uzytkownika` FROM `uzytkownicy` WHERE `login` = ? OR numer_telefonu = ?";
        try (PreparedStatement existsStatement = dbh.conn.prepareStatement(existsQuery)) {
            existsStatement.setString(1, login);
            existsStatement.setInt(2, phoneNumber);

            ResultSet existing = existsStatement.executeQuery();
            return existing.next();
        }
    }

    public boolean registerUser(String login, String password, String imie, String nazwisko, int phoneNumber) throws SQLException {
        String registerQuery = "INSERT INTO uzytkownicy(`login`,`password`,`imie`,`nazwisko`,`data_utworzenia`,`numer_telefonu`,`typy_uzytkownikow_id_typu`) VALUES (?, ?, ?, ?, NOW(), ?, 1)";
        try (PreparedStatement registerStatement = dbh.conn.prepareStatement(registerQuery)) {
            registerStatement.setString(1, login);
            registerStatement.setString(2, password);
            registerStatement.setString(3, imie);
            registerStatement.setString(4, nazwisko);
            registerStatement.setInt(5, phoneNumber);

            int registerResult = registerStatement.executeUpdate();
            return registerResult > 0;
        }
    }

    public boolean loginUser(String login, String password) throws SQLException {
        String existsQuery = "SELECT `id_uzytkownika` FROM `uzytkownicy` WHERE BINARY `login` = ? AND BINARY password = ?";
        try (PreparedStatement existsStatement = dbh.conn.prepareStatement(existsQuery)) {
            existsStatement.setString(1, login);
            existsStatement.setString(2, password);

            ResultSet existing = existsStatement.executeQuery();
            return existing.next();
        }
    }
}

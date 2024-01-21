package org.projektpo2.DatabaseRepositories;

import org.projektpo2.DatabaseHandler;
import org.projektpo2.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Klasa UserRepository odpowiada za interakcję z bazą danych związaną z użytkownikami.
 */
public class UserRepository {
    private static final Logger logger = Logger.getLogger(UserRepository.class.getName());

    private final DatabaseHandler dbh;

    /**
     * Konstruktor klasy UserRepository.
     *
     * @param dbh Obiekt DatabaseHandler, który obsługuje połączenie z bazą danych.
     */
    public UserRepository(DatabaseHandler dbh) {
        this.dbh = dbh;
    }

    /**
     * Pobiera uprawnienia użytkownika z bazy danych.
     *
     * @param session Obiekt User reprezentujący użytkownika.
     */
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
            logger.severe("Błąd podczas pobierania uprawnień użytkownika: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Sprawdza, czy istnieje połączenie z bazą danych.
     *
     * @return true, jeśli istnieje połączenie z bazą danych; false w przeciwnym razie.
     */
    private boolean checkDBConnection() {
        return dbh != null && dbh.conn != null;
    }

    /**
     * Ustawia uprawnienia użytkownika na podstawie wyników zapytania SQL.
     *
     * @param session Obiekt User reprezentujący zalogowanego użytkownika.
     * @param query   Zapytanie SQL do pobrania uprawnień.
     * @throws SQLException Jeśli wystąpi błąd podczas komunikacji z bazą danych.
     */
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

    /**
     * Sprawdza, czy istnieje użytkownik o podanym loginie lub numerze telefonu.
     *
     * @param login        Login użytkownika do sprawdzenia.
     * @param phoneNumber Numer telefonu użytkownika do sprawdzenia.
     * @return true, jeśli użytkownik istnieje; false w przeciwnym razie.
     * @throws SQLException Jeśli wystąpi błąd podczas komunikacji z bazą danych.
     */
    public boolean isUserExists(String login, int phoneNumber) throws SQLException {
        String existsQuery = "SELECT `id_uzytkownika` FROM `uzytkownicy` WHERE `login` = ? OR numer_telefonu = ?";
        try (PreparedStatement existsStatement = dbh.conn.prepareStatement(existsQuery)) {
            existsStatement.setString(1, login);
            existsStatement.setInt(2, phoneNumber);

            ResultSet existing = existsStatement.executeQuery();
            return existing.next();
        }
    }

    /**
     * Rejestruje nowego użytkownika w bazie danych.
     *
     * @param login       Login nowego użytkownika.
     * @param password    Hasło nowego użytkownika.
     * @param imie        Imię nowego użytkownika.
     * @param nazwisko    Nazwisko nowego użytkownika.
     * @param phoneNumber Numer telefonu nowego użytkownika.
     * @return true, jeśli rejestracja powiodła się; false w przeciwnym razie.
     * @throws SQLException Jeśli wystąpi błąd podczas komunikacji z bazą danych.
     */
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

    /**
     * Loguje użytkownika do systemu.
     *
     * @param login    Login użytkownika.
     * @param password Hasło użytkownika.
     * @return true, jeśli logowanie powiodło się; false w przeciwnym razie.
     * @throws SQLException Jeśli wystąpi błąd podczas komunikacji z bazą danych.
     */
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

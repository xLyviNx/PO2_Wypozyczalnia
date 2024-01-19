package org.projektpo2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasa obsługująca połączenie z bazą danych.
 */
public class DatabaseHandler implements AutoCloseable {
    private static final Logger logger = Utilities.getLogger(DatabaseHandler.class);

    private static String URL = "";
    private static String USER = "";
    private static String PASSWORD = "";

    /**
     * Tworzy URL na podstawie parametrów.
     *
     * @param ip     Adres IP serwera bazy danych.
     * @param port   Numer portu serwera bazy danych.
     * @param dbname Nazwa bazy danych.
     * @return Gotowy URL.
     */
    public static String makeURL(String ip, String port, String dbname) {
        return "jdbc:mysql://" + ip + ":" + port + "/" + dbname;
    }

    /**
     * Ustawia dane uwierzytelniające do połączenia z bazą danych.
     *
     * @param ip       Adres IP serwera bazy danych.
     * @param port     Numer portu serwera bazy danych.
     * @param dbname   Nazwa bazy danych.
     * @param username Nazwa użytkownika.
     * @param password Hasło użytkownika.
     */
    public static void setCredentials(String ip, String port, String dbname, String username, String password) {
        URL = makeURL(ip, port, dbname);
        USER = username;
        PASSWORD = password;
    }

    public Connection conn;
    private Statement statement;

    /**
     * Konstruktor inicjalizujący połączenie z bazą danych.
     */
    public DatabaseHandler() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            statement = conn.createStatement();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Błąd połączenia z bazą danych", e);
            throw e;
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wykonuje zapytanie do bazy danych i zwraca wynik.
     *
     * @param query Zapytanie SQL.
     * @return Wynik zapytania w postaci ResultSet.
     * @throws SQLException Wyjątek SQL.
     */
    public ResultSet executeQuery(String query) throws SQLException {
        return statement.executeQuery(query);
    }

    /**
     * Wykonuje operację aktualizacji bazy danych.
     *
     * @param query                  Zapytanie SQL.
     * @param preparedStatementSetter Interfejs ustawiający parametry zapytania.
     * @return True, jeśli operacja zakończyła się powodzeniem, w przeciwnym razie false.
     * @throws SQLException Wyjątek SQL.
     */
    public boolean executeUpdate(String query, PreparedStatementSetter preparedStatementSetter) throws SQLException {
        try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatementSetter.setValues(preparedStatement);
            int result = preparedStatement.executeUpdate();
            return result > 0;
        }
    }

    /**
     * Zamyka połączenie z bazą danych.
     */
    @Override
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Błąd zamknięcia połączenia z bazą danych", e);
            throw new RuntimeException(e);
        }
    }
}

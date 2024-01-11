package src;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHandler implements AutoCloseable {
    private static String URL = "";
    private static String USER = "";
    private static String PASSWORD = "";

    public static String makeURL(String ip, String port, String dbname) {
        return "jdbc:mysql://" + ip + ":" + port + "/" + dbname;
    }

    public static void setCredentials(String ip, String port, String dbname, String username, String password) {
        URL = makeURL(ip, port, dbname);
        USER = username;
        PASSWORD = password;
    }

    public Connection conn;
    private Statement statement;

    public DatabaseHandler() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            statement = conn.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ResultSet executeQuery(String query) throws SQLException {
        return statement.executeQuery(query);
    }

    public boolean executeUpdate(String query, PreparedStatementSetter preparedStatementSetter) throws SQLException {
        try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatementSetter.setValues(preparedStatement);
            int result = preparedStatement.executeUpdate();
            return result > 0;
        }
    }

    @Override
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

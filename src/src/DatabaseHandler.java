package src;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHandler {
    public static String URL = "";
    public static String USER = "";
    public static String PASSWORD = "";
    public Connection conn;
    private Statement statement;
    public static String makeURL(String ip, String port, String dbname)
    {
        return "jdbc:mysql://" + ip + ":" + port + "/"+dbname;
    }
    public static void setCredentials(String ip, String port, String dbname, String username, String password)
    {
        URL=makeURL(ip,port,dbname);
        USER=username;
        PASSWORD = password;
        System.out.println(URL);
        System.out.println(USER);
        System.out.println(PASSWORD);
    }
    public DatabaseHandler() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            statement = conn.createStatement();
            //System.out.println("CONN: " + conn.isClosed());
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public ResultSet executeQuery(String query) {
        ResultSet resultSet = null;
        try {
            resultSet = statement.executeQuery(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultSet;
    }

    public int executeUpdate(String query) {
        int rowsAffected = 0;
        try {
            rowsAffected = statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rowsAffected;
    }

    public void close() {
        try {
            if (statement != null) {
                statement.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

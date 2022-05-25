package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class JdbcApp implements AuthService {

    private static final Logger LOG = LogManager.getLogger(JdbcApp.class);
    private static Connection connection;
    private static Statement stmt;

    public JdbcApp() {
    }

    public static void connect() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:clientschat.db");
            connection.setAutoCommit(true);
            stmt = connection.createStatement();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void disconnect() {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        connect();
        LOG.info("Сервис аутентификации запущен");
    }

    public String getLoginPassClientsChat(String login, String pass) {
        try (ResultSet rs = stmt.executeQuery("SELECT * FROM clients;")) {
            while (rs.next()) {
                if (rs.getString("login").equals(login) && rs.getString("pass").equals(pass)) {
                    return rs.getString("nick");
                }
            }
        } catch (SQLException throwable) {
            throwable.printStackTrace();
        }
        return null;
    }

    public static void changeNick(String oldNick, String newNick) throws SQLException {
        String qwerty = "UPDATE clients SET nick = '" + newNick + "' WHERE nick = '" + oldNick + "'";
        stmt.executeUpdate(qwerty);
    }

    public void stop() {
        disconnect();
        LOG.info("Сервис аутентификации остановлен");
    }

    public static synchronized boolean isNickClientsChat(String name) {
        try (ResultSet rs = stmt.executeQuery("SELECT * FROM clients;")) {
            while (rs.next()) {
                if (rs.getString("nick").equals(name)) {
                    return true;
                }
            }
        } catch (SQLException throwable) {
            throwable.printStackTrace();
        }
        return false;
    }

    protected static void readEx() {
        try (ResultSet rs = stmt.executeQuery("SELECT * FROM clients;")) {
            while (rs.next()) {
                LOG.info(rs.getString("login") + " " + rs.getString("pass") + " " + rs.getString("nick"));
            }
        } catch (SQLException throwable) {
            throwable.printStackTrace();
        }
    }
}

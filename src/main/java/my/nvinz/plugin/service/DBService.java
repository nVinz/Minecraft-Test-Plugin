package my.nvinz.plugin.service;

import java.sql.*;
import java.time.LocalDate;
import java.util.Objects;

public class DBService {

    private String userName;
    private String userPassword;
    private String url;

    private Connection createConnection() {
        try {
            return DriverManager.getConnection(url, userName, userPassword);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Супер долго
    public void saveKill(String table, String playerName, String ocelotName) {
        // В отдельном потоке чтобы не грузить основной, но все равно заметно
        Runnable execution = () -> {
            try(Connection connection = createConnection()) {
                Objects.requireNonNull(connection, "Unable to create DB connection: " + url);
                PreparedStatement statement = connection.prepareStatement("INSERT INTO " + table + " (player_name, ocelot_name, date) VALUES (?, ?, ?);");
                statement.setString(1, playerName);
                statement.setString(2, ocelotName);
                statement.setDate(3, Date.valueOf(LocalDate.now()));
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };
        Thread executionThread = new Thread(execution);
        executionThread.start();
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

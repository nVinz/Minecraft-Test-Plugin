package plugin.utils;

import java.sql.*;
import java.time.LocalDate;
import java.util.Calendar;

public class DBUtils {

    private String userName;
    private String userPassword;
    private String url;

    private Connection createConnection() {
        try {
            Connection connection = DriverManager.getConnection(url, userName, userPassword);
            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Супер долго
    public void saveKill(String table, String playerName, String ocelotName) {
        Connection connection = createConnection();
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO " + table + " (player_name, ocelot_name, date) VALUES (?, ?, ?);");
            statement.setString(1, playerName);
            statement.setString(2, ocelotName);
            statement.setDate(3, Date.valueOf(LocalDate.now()));
            statement.execute();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

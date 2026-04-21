package prog3.exam.datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import io.github.cdimascio.dotenv.Dotenv;

public class DataSourceConfig {
    private final Dotenv dotenv = Dotenv.load();

    public Connection getConnection() {
        try {
            String url = dotenv.get("JDBC_URL");
            String user = dotenv.get("JDBC_USER");
            String password = dotenv.get("JDBC_PASSWORD");

            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

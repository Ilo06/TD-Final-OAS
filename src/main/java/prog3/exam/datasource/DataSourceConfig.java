package prog3.exam.datasource;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Configuration
public class DataSourceConfig {

    private final String url;
    private final String user;
    private final String password;

    public DataSourceConfig() {
        Dotenv dotenv = Dotenv.load();
        this.url = dotenv.get("JDBC_URL");
        this.user = dotenv.get("JDBC_USER");
        this.password = dotenv.get("JDBC_PASSWORD");
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open DB connection: " + e.getMessage(), e);
        }
    }

    public void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to close DB connection: " + e.getMessage(), e);
            }
        }
    }
}
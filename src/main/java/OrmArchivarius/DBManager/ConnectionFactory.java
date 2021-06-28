package OrmArchivarius.DBManager;

import Client.Application;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectionFactory {

    public static Connection getConnection(String dbName) throws SQLException {
        Connection connection;
        try {
            Properties props = new Properties();
            try (InputStream in = Application.class.getClassLoader().getResourceAsStream("database.properties")) {
                props.load(in);
            }
            String dbHost = props.getProperty("dbHost");
            String dbUrl = dbHost + "/" + dbName;
            String dbUser = props.getProperty("dbUser");
            String dbPassword = props.getProperty("dbPassword");
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            connection.setAutoCommit(false);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException(e.getMessage(), e);
        }
        return connection;
    }

}

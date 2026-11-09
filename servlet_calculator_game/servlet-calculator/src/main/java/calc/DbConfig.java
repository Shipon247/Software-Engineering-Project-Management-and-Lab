package calc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * MySQL connection settings for the whole project (calculator history + Math Whiz scores).
 *
 * >>> Change PASSWORD below to the password you set for MySQL Workbench / MySQL Server. <<<
 * (You can also set environment variables DB_HOST, DB_PORT, DB_USER, DB_PASSWORD instead.)
 */
public final class DbConfig {

    public static final String DATABASE = "math_whiz_db";

    public static final String HOST     = env("DB_HOST", "localhost");
    public static final String PORT     = env("DB_PORT", "3306");
    public static final String USER     = env("DB_USER", "root");
    public static final String PASSWORD = env("DB_PASSWORD", "Shipon28@");   // <-- put YOUR MySQL password here

    // createDatabaseIfNotExist=true: the database math_whiz_db is created automatically if missing
    public static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
            + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
            + "&useUnicode=true&characterEncoding=UTF-8";

    private DbConfig() { }

    /** Opens a new connection to the MySQL database. */
    public static Connection connect() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found (check pom.xml / reload Maven)", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** For log messages / the page, never includes the password. */
    public static String describe() {
        return "MySQL " + HOST + ":" + PORT + "/" + DATABASE + " (user " + USER + ")";
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isEmpty() ? fallback : v;
    }
}

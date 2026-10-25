package id.vapezoo.categories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

public final class Database {
  private Database() {}

  @FunctionalInterface
  public interface Connections {
    Connection open() throws SQLException;
  }

  public static Connections environment(Map<String, String> values) {
    String url = required(values, "DB_URL");
    String user = required(values, "DB_USER");
    String password = required(values, "DB_PASSWORD");
    if (!url.startsWith("jdbc:mysql://")) {
      throw new IllegalArgumentException("DB_URL harus berupa URL JDBC MySQL.");
    }
    return () -> {
      Connection connection = DriverManager.getConnection(url, user, password);
      try {
        connection.setReadOnly(true);
        return connection;
      } catch (SQLException failure) {
        try {
          connection.close();
        } catch (SQLException cleanup) {
          failure.addSuppressed(cleanup);
        }
        throw failure;
      }
    };
  }

  private static String required(Map<String, String> values, String key) {
    String value = values.get(key);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Environment " + key + " belum diisi.");
    }
    return value;
  }
}

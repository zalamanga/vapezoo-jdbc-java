package id.vapezoo.categories;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryReaderTest {
  private Connection anchor;
  private String url;
  private CategoryRepository repository;
  private final AtomicInteger closed = new AtomicInteger();

  @BeforeEach
  void createIsolatedSchema() throws Exception {
    url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL";
    anchor = DriverManager.getConnection(url, "sa", "");
    String schema = Files.readString(Path.of("database/001-schema.sql"));
    schema = schema.substring(0, schema.indexOf("INSERT INTO"))
        .replace("CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci", "");
    try (var statement = anchor.createStatement()) {
      statement.execute(schema);
      statement.executeUpdate("INSERT INTO category(category_name,category_description) VALUES "
          + "('Perangkat','Kategori contoh'),('Aksesori',NULL),('Literal % _','Karakter pencarian')");
    }
    repository = new CategoryRepository(this::trackedConnection);
  }

  @AfterEach
  void closeFixture() throws Exception {
    anchor.close();
  }

  private Connection trackedConnection() throws SQLException {
    Connection connection = DriverManager.getConnection(url, "sa", "");
    return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
        new Class<?>[] {Connection.class}, (proxy, method, arguments) -> {
          if (method.getName().equals("close")) closed.incrementAndGet();
          try {
            return method.invoke(connection, arguments);
          } catch (InvocationTargetException failure) {
            throw failure.getCause();
          }
        });
  }

  @Test
  void listsInIdOrderAndHandlesNullableDescription() throws Exception {
    var rows = repository.search("");
    assertEquals(3, rows.size());
    assertEquals("Perangkat", rows.get(0).name());
    assertNull(rows.get(1).description());
    assertEquals(1, closed.get());
  }

  @Test
  void searchIsCaseInsensitiveAndUsesDescription() throws Exception {
    assertEquals("Perangkat", repository.search("  CONTOH ").get(0).name());
    assertEquals("Aksesori", repository.search("AKSES").get(0).name());
    assertEquals(2, closed.get());
  }

  @Test
  void percentUnderscoreAndSqlSyntaxAreLiteralSearchTerms() throws Exception {
    assertEquals(1, repository.search("%").size());
    assertEquals(1, repository.search("_").size());
    assertTrue(repository.search("' OR 1=1 --").isEmpty());
  }

  @Test
  void findsExistingIdAndReportsAbsentIdWithoutLeavingConnectionsOpen() throws Exception {
    assertEquals("Aksesori", repository.find(2).orElseThrow().name());
    assertTrue(repository.find(999).isEmpty());
    assertThrows(IllegalArgumentException.class, () -> repository.find(0));
    assertEquals(2, closed.get());
  }

  @Test
  void rejectsOversizedOrControlCharacterSearchBeforeOpeningConnection() {
    assertThrows(IllegalArgumentException.class, () -> repository.search("a".repeat(101)));
    assertThrows(IllegalArgumentException.class, () -> repository.search("term\u001b[31m"));
    assertEquals(0, closed.get());
  }

  @Test
  void closesConnectionWhenQueryFails() throws Exception {
    try (var statement = anchor.createStatement()) {
      statement.execute("DROP TABLE category");
    }
    assertThrows(SQLException.class, () -> repository.search(""));
    assertEquals(1, closed.get());
  }

  @Test
  void invalidChoicesIdsAndEmptyResultsKeepMenuUsable() throws Exception {
    String result = session("abc\n3\n-1\n3\nabc\n3\n999\n2\nunknown\n0\n", repository);
    assertTrue(result.contains("Pilihan tidak tersedia"));
    assertTrue(result.contains("ID harus berupa bilangan bulat positif"));
    assertTrue(result.contains("ID tidak ditemukan"));
    assertTrue(result.contains("Tidak ada kategori yang cocok"));
    assertTrue(result.endsWith("Selesai.\n") || result.endsWith("Selesai.\r\n"));
    assertEquals(3, repository.search("").size());
  }

  @Test
  void eofDuringPromptExitsWithoutAnUnintendedQuery() throws Exception {
    assertTrue(session("2\n", repository).contains("Input berakhir. Selesai."));
    assertEquals(0, closed.get());
  }

  @Test
  void firstDatabaseFailureCanBeRetriedWithoutPrintingSecretDetails() throws Exception {
    AtomicBoolean first = new AtomicBoolean(true);
    var flaky = new CategoryRepository(() -> {
      if (first.getAndSet(false)) throw new SQLException("never-print-this-password", "08001");
      return trackedConnection();
    });
    String result = session("1\n1\n0\n", flaky);
    assertTrue(result.contains("Kode SQL: 08001"));
    assertTrue(result.contains("ID 1 | Perangkat"));
    assertFalse(result.contains("never-print-this"));
    assertEquals(1, closed.get());
  }

  @Test
  void configurationRejectsMissingValuesAndOtherDatabaseTypesWithoutConnecting() {
    assertTrue(assertThrows(IllegalArgumentException.class,
        () -> Database.environment(Map.of())).getMessage().contains("DB_URL"));
    assertThrows(IllegalArgumentException.class, () -> Database.environment(
        Map.of("DB_URL", "jdbc:h2:mem:test", "DB_USER", "reader", "DB_PASSWORD", "example")));
  }

  private String session(String input, CategoryRepository source) throws Exception {
    StringWriter output = new StringWriter();
    new ConsoleApp(new StringReader(input), output, source).run();
    return output.toString();
  }
}

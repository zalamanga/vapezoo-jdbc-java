package id.vapezoo.categories;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public final class Main {
  private Main() {}

  public static void main(String[] args) {
    try {
      var repository = new CategoryRepository(Database.environment(System.getenv()));
      new ConsoleApp(new InputStreamReader(System.in, StandardCharsets.UTF_8),
          new OutputStreamWriter(System.out, StandardCharsets.UTF_8), repository).run();
    } catch (IllegalArgumentException invalid) {
      System.err.println(invalid.getMessage());
      System.exit(2);
    } catch (IOException unavailable) {
      System.err.println("Input/output tidak tersedia.");
      System.exit(3);
    }
  }
}

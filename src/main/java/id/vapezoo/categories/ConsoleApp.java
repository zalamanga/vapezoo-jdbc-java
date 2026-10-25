package id.vapezoo.categories;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.SQLException;
import java.util.List;

public final class ConsoleApp {
  private final BufferedReader input;
  private final PrintWriter output;
  private final CategoryRepository repository;

  public ConsoleApp(Reader input, Writer output, CategoryRepository repository) {
    this.input = new BufferedReader(input);
    this.output = new PrintWriter(output, true);
    this.repository = repository;
  }

  public void run() throws IOException {
    output.println("VAPEZOO JDBC | Pembaca kategori demo");
    output.println("Data hanya dibaca. Program ini tidak mengelola produk atau penjualan.");
    while (true) {
      output.println("\n1 Daftar kategori | 2 Cari nama/keterangan | 3 Buka ID | 0 Keluar");
      String choice = read("Pilihan: ");
      if (choice == null || choice.equals("0")) {
        output.println("Selesai.");
        return;
      }
      try {
        switch (choice) {
          case "1" -> print(repository.search(""));
          case "2" -> print(repository.search(required("Kata pencarian: ")));
          case "3" -> {
            long id;
            try {
              id = Long.parseLong(required("ID kategori: "));
            } catch (NumberFormatException invalid) {
              throw new IllegalArgumentException("ID harus berupa bilangan bulat positif.");
            }
            var found = repository.find(id);
            if (found.isPresent()) print(List.of(found.get()));
            else output.println("ID tidak ditemukan.");
          }
          default -> output.println("Pilihan tidak tersedia. Gunakan 0 sampai 3.");
        }
      } catch (EOFException ended) {
        output.println("\nInput berakhir. Selesai.");
        return;
      } catch (IllegalArgumentException invalid) {
        output.println("Input ditolak: " + invalid.getMessage());
      } catch (SQLException unavailable) {
        String state = unavailable.getSQLState();
        if (state == null || !state.matches("[A-Z0-9]{5}")) state = "tidak tersedia";
        output.println("Data belum dapat dimuat. Periksa koneksi/schema lalu coba lagi. Kode SQL: " + state);
      }
    }
  }

  private void print(List<Category> rows) {
    if (rows.isEmpty()) {
      output.println("Tidak ada kategori yang cocok.");
      return;
    }
    for (Category category : rows) {
      output.println("\nID " + category.id() + " | " + safe(category.name()));
      output.println("Keterangan: " + safe(category.description()));
    }
  }

  private String safe(String value) {
    return value == null ? "-" : value.replaceAll("\\p{Cntrl}", " ");
  }

  private String read(String prompt) throws IOException {
    output.print(prompt);
    output.flush();
    String value = input.readLine();
    return value == null ? null : value.strip();
  }

  private String required(String prompt) throws IOException {
    String value = read(prompt);
    if (value == null) throw new EOFException();
    return value;
  }
}

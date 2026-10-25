package id.vapezoo.categories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CategoryRepository {
  private final Database.Connections connections;

  public CategoryRepository(Database.Connections connections) {
    this.connections = connections;
  }

  public List<Category> search(String keyword) throws SQLException {
    String term = keyword.strip().toLowerCase(java.util.Locale.ROOT);
    if (term.length() > 100 || term.chars().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException("Kata pencarian maksimal 100 karakter tanpa karakter kontrol.");
    }
    try (var connection = connections.open();
        var query = connection.prepareStatement(
            "SELECT category_id, category_name, category_description FROM category "
                + "WHERE LOCATE(?,LOWER(category_name)) > 0 "
                + "OR LOCATE(?,LOWER(COALESCE(category_description,''))) > 0 ORDER BY category_id")) {
      query.setString(1, term);
      query.setString(2, term);
      try (ResultSet rows = query.executeQuery()) {
        List<Category> found = new ArrayList<>();
        while (rows.next()) found.add(category(rows));
        return found;
      }
    }
  }

  public Optional<Category> find(long id) throws SQLException {
    if (id <= 0) throw new IllegalArgumentException("ID harus berupa bilangan bulat positif.");
    try (var connection = connections.open();
        var query = connection.prepareStatement(
            "SELECT category_id, category_name, category_description FROM category WHERE category_id = ?")) {
      query.setLong(1, id);
      try (ResultSet rows = query.executeQuery()) {
        return rows.next() ? Optional.of(category(rows)) : Optional.empty();
      }
    }
  }

  private Category category(ResultSet row) throws SQLException {
    return new Category(row.getLong("category_id"), row.getString("category_name"),
        row.getString("category_description"));
  }
}

CREATE TABLE category (
  category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  category_name VARCHAR(100) NOT NULL UNIQUE,
  category_description VARCHAR(500)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO category (category_name, category_description) VALUES
  ('Perangkat', 'Kategori contoh untuk mencoba pembacaan JDBC.'),
  ('Aksesori', 'Data sintetis untuk pencarian nama dan keterangan.'),
  ('Lain-lain', 'Baris contoh; tidak mewakili katalog usaha aktif.');

REVOKE ALL PRIVILEGES ON vapezoo_categories.* FROM 'category_reader'@'%';
GRANT SELECT ON vapezoo_categories.* TO 'category_reader'@'%';

package com.example.demo.admin;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AdminRepository {
    private static final int PER_HALAMAN = 2;
    private static final String PENGGUNA = " FROM users WHERE strpos(lower(username), lower(?)) > 0"
            + " OR strpos(lower(full_name), lower(?)) > 0";
    private static final String KATEGORI = " FROM categories WHERE strpos(lower(name), lower(?)) > 0";
    private static final Logger LOG = LoggerFactory.getLogger(AdminRepository.class);
    private final JdbcTemplate jdbc;
    private final Path folderFoto;
    private final Path folderPenanda;

    public AdminRepository(JdbcTemplate jdbc, @Value("${app.upload-dir:uploads}") String lokasi) {
        this.jdbc = jdbc;
        folderFoto = Path.of(lokasi).toAbsolutePath().normalize();
        folderPenanda = folderFoto.resolveSibling("." + folderFoto.getFileName() + "-hapus");
    }

    public boolean adalahAdmin(int idusr) {
        String sql = "SELECT EXISTS (SELECT 1 FROM users WHERE idusr = ? AND role = 'ADMIN')";
        return Boolean.TRUE.equals(jdbc.queryForObject(sql, Boolean.class, idusr));
    }

    public int halamanPengguna(String cari) {
        return jumlahHalaman(PENGGUNA, cari, cari);
    }

    public int halamanKategori(String cari) {
        return jumlahHalaman(KATEGORI, cari);
    }

    private int jumlahHalaman(String sumber, Object... kataKunci) {
        int jumlah = jdbc.queryForObject("SELECT COUNT(*)" + sumber, Integer.class, kataKunci);
        return jumlah == 0 ? 1 : 1 + (jumlah - 1) / PER_HALAMAN;
    }

    public List<Map<String, Object>> daftarPengguna(String cari, String urut, String arah, int halaman) {
        // ORDER BY hanya boleh memakai dua kolom yang tersedia di form.
        String kolom = "full_name".equals(urut) ? "full_name" : "username";
        String sql = "SELECT idusr, username, full_name, role" + PENGGUNA
                + " ORDER BY lower(" + kolom + ") " + arahSql(arah) + ", idusr LIMIT ? OFFSET ?";
        return jdbc.queryForList(sql, cari, cari, PER_HALAMAN, (halaman - 1) * PER_HALAMAN);
    }

    public List<Map<String, Object>> daftarKategori(String cari, String arah, int halaman) {
        String sql = "SELECT idcat, name" + KATEGORI + " ORDER BY lower(name) " + arahSql(arah)
                + ", idcat LIMIT ? OFFSET ?";
        return jdbc.queryForList(sql, cari, PER_HALAMAN, (halaman - 1) * PER_HALAMAN);
    }

    private String arahSql(String arah) {
        return "desc".equalsIgnoreCase(arah) ? "DESC" : "ASC";
    }

    @Transactional
    public boolean tambahKategori(String nama) {
        // Contoh : Jika dua Admin yang bersamaan menambah “Pedas” dan “pedas” tetap menghasilkan satu kategori.
        jdbc.execute("LOCK TABLE categories IN SHARE ROW EXCLUSIVE MODE");
        String sql = """
                INSERT INTO categories (name)
                SELECT ? WHERE NOT EXISTS (SELECT 1 FROM categories WHERE lower(trim(name)) = lower(?))
                """;
        return jdbc.update(sql, nama, nama) == 1;
    }

    public boolean hapusKategori(int idcat) {
        // Foreign key melepas kategori dari resep saat kategori dihapus.
        return jdbc.update("DELETE FROM categories WHERE idcat = ?", idcat) == 1;
    }

    public List<Map<String, Object>> statistikKategori() {
        // LEFT JOIN ikut menampilkan kategori yang jumlah resepnya masih nol.
        return jdbc.queryForList("""
                SELECT c.name, COUNT(rc.idrec) AS jumlah
                FROM categories c LEFT JOIN recipes_categories rc ON rc.idcat = c.idcat
                GROUP BY c.idcat, c.name ORDER BY jumlah DESC, lower(c.name), c.idcat
                """);
    }

    @Transactional(rollbackFor = IOException.class)
    public boolean hapusAkun(int idusr) throws IOException {
        String akun = "SELECT idusr FROM users WHERE idusr = ? AND role = 'USER' FOR UPDATE";
        if (jdbc.queryForList(akun, Integer.class, idusr).isEmpty()) {
            return false;
        }
        // Catat nama foto selagi baris resep masih ada. CASCADE akan menghapus resepnya.
        String sql = "SELECT photo_path FROM recipes WHERE idusr = ? AND photo_path IS NOT NULL FOR UPDATE";
        for (String path : jdbc.queryForList(sql, String.class, idusr)) {
            if (!path.isBlank()) {
                tandaiFoto(path);
            }
        }
        jdbc.update("DELETE FROM users WHERE idusr = ?", idusr);
        return true;
    }

    private void tandaiFoto(String path) throws IOException {
        String[] bagian = path.split("/", -1);
        if (bagian.length != 2 || !bagian[0].equals("uploads")) {
            throw new IOException("Path foto harus berupa uploads/nama-file.");
        }
        periksaFolder();
        String nama = bagian[1];
        if (nama.isBlank() || nama.equals(".") || nama.equals("..") || nama.contains("\\") || nama.contains(":")) {
            throw new IOException("Nama foto tidak valid.");
        }
        if (adaFoto(nama)) {
            Files.createDirectories(folderPenanda);
            // Simpan nama file agar penghapusan bisa dilanjutkan setelah commit.
            Files.writeString(folderPenanda.resolve(nama), "", CREATE, WRITE, NOFOLLOW_LINKS);
        }
    }

    private void periksaFolder() throws IOException {
        if (Files.isSymbolicLink(folderFoto) || Files.isSymbolicLink(folderPenanda)) {
            throw new IOException("Periksa lokasi folder foto.");
        }
    }

    private boolean adaFoto(String nama) throws IOException {
        Path foto = folderFoto.resolve(nama);
        if (Files.notExists(foto, NOFOLLOW_LINKS)) {
            return false; // Data dummy bisa berisi path saja.
        }
        if (!Files.isRegularFile(foto, NOFOLLOW_LINKS)) {
            throw new IOException("Path foto harus mengarah ke file biasa.");
        }
        return true;
    }

    public boolean bersihkanFoto() {
        if (Files.notExists(folderPenanda, NOFOLLOW_LINKS)) {
            return true;
        }
        try {
            periksaFolder();
            try (DirectoryStream<Path> daftar = Files.newDirectoryStream(folderPenanda)) {
                for (Path penanda : daftar) {
                    String nama = penanda.getFileName().toString();
                    String sql = "SELECT EXISTS (SELECT 1 FROM recipes WHERE photo_path = ?)";
                    // Kadang dua resep memakai file yang sama.
                    if (!Boolean.TRUE.equals(jdbc.queryForObject(sql, Boolean.class, "uploads/" + nama))) {
                        if (adaFoto(nama)) {
                            Files.deleteIfExists(folderFoto.resolve(nama));
                        }
                        Files.deleteIfExists(penanda);
                    }
                }
            }
            return true;
        } catch (IOException | DataAccessException e) {
            LOG.warn("Sebagian foto masih menunggu dihapus.", e);
            return false;
        }
    }

    // Kalau aplikasi berhenti sesudah commit, lanjutkan dari penanda yang tersisa.
    @EventListener(ApplicationReadyEvent.class)
    public void lanjutkanHapusFoto() {
        bersihkanFoto();
    }
}

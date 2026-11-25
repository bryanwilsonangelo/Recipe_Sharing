package com.example.demo.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {
    private final AdminRepository repository;

    public AdminCategoryController(AdminRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String daftar(@RequestParam(defaultValue = "") String cari,
                         @RequestParam(defaultValue = "asc") String arah,
                         @RequestParam(defaultValue = "1") int halaman, Model model) {
        cari = cari.strip();
        arah = "desc".equalsIgnoreCase(arah) ? "desc" : "asc";
        int totalHalaman = repository.halamanKategori(cari);
        halaman = Math.max(1, Math.min(halaman, totalHalaman));

        model.addAttribute("kategori", repository.daftarKategori(cari, arah, halaman));
        // Grafik selalu memakai semua kategori, meski daftar sedang difilter.
        model.addAttribute("statistik", repository.statistikKategori());
        model.addAttribute("cari", cari);
        model.addAttribute("arah", arah);
        model.addAttribute("halaman", halaman);
        model.addAttribute("totalHalaman", totalHalaman);
        return "admin/categories";
    }

    @PostMapping("/tambah")
    public String tambah(@RequestParam(defaultValue = "") String nama, RedirectAttributes pesan) {
        // Rapikan spasi sebelum memeriksa panjang dan mencari duplikat.
        nama = nama.strip();
        if (nama.isEmpty() || nama.codePointCount(0, nama.length()) > 50) {
            pesan.addFlashAttribute("gagal", "Nama kategori harus berisi 1 sampai 50 karakter.");
        } else if (!repository.tambahKategori(nama)) {
            pesan.addFlashAttribute("gagal", "Nama kategori itu sudah digunakan.");
        } else {
            pesan.addFlashAttribute("sukses", "Kategori " + nama + " ditambahkan.");
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{idcat}/hapus")
    public String hapus(@PathVariable int idcat, RedirectAttributes pesan) {
        if (repository.hapusKategori(idcat)) {
            pesan.addFlashAttribute("sukses", "Kategori dihapus. Semua resep tetap tersimpan.");
        } else {
            pesan.addFlashAttribute("gagal", "Kategori tidak ditemukan.");
        }
        return "redirect:/admin/categories";
    }
}

package com.example.demo.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.UUID;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
    private final AdminRepository repository;

    public AdminUserController(AdminRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String daftar(@RequestParam(defaultValue = "") String cari,
                         @RequestParam(defaultValue = "username") String urut,
                         @RequestParam(defaultValue = "asc") String arah,
                         @RequestParam(defaultValue = "1") int halaman, Model model) {
        cari = cari.strip();
        urut = "full_name".equals(urut) ? "full_name" : "username";
        arah = "desc".equalsIgnoreCase(arah) ? "desc" : "asc";
        int totalHalaman = repository.halamanPengguna(cari);
        // Setelah pencarian, halaman yang lama bisa berada di luar hasil.
        halaman = Math.max(1, Math.min(halaman, totalHalaman));

        model.addAttribute("pengguna", repository.daftarPengguna(cari, urut, arah, halaman));
        model.addAttribute("cari", cari);
        model.addAttribute("urut", urut);
        model.addAttribute("arah", arah);
        model.addAttribute("halaman", halaman);
        model.addAttribute("totalHalaman", totalHalaman);
        return "admin/users";
    }

    @PostMapping("/{idusr}/hapus")
    public String hapus(@PathVariable int idusr, RedirectAttributes pesan) {
        // Hapus file fisik hanya setelah transaksi akun berhasil.
        try {
            if (!repository.hapusAkun(idusr)) {
                pesan.addFlashAttribute("gagal", "Akun tidak ditemukan atau merupakan akun Admin.");
            } else if (repository.bersihkanFoto()) {
                pesan.addFlashAttribute("sukses", "Akun pengguna beserta resep dan fotonya sudah dihapus.");
            } else {
                pesan.addFlashAttribute("gagal", "Akun dan resep terhapus. Periksa izin folder foto. "
                        + "Aplikasi akan mencoba membersihkan sisa file saat dijalankan kembali.");
            }
        } catch (IOException e) {
            pesan.addFlashAttribute("gagal", "Akun masih tersimpan. Periksa path foto dan izin folder upload.");
        }
        return "redirect:/admin/users";
    }
}

// Penjaga ini dipakai oleh halaman pengguna dan kategori.
@Configuration(proxyBeanMethods = false)
class PenjagaHalamanAdmin implements WebMvcConfigurer, HandlerInterceptor {
    private final AdminRepository repository;

    PenjagaHalamanAdmin(AdminRepository repository) {
        this.repository = repository;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/admin/**");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        HttpSession session = request.getSession(false);
        Object pengguna = session == null ? null : session.getAttribute("loggedInUser");
        if (!(pengguna instanceof Integer idusr)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        if (!repository.adalahAdmin(idusr)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        // Token memastikan form dikirim dari sesi Admin yang membukanya.
        String token;
        synchronized (session) {
            token = (String) session.getAttribute("tokenFormAdmin");
            if (token == null) {
                token = UUID.randomUUID().toString();
                session.setAttribute("tokenFormAdmin", token);
            }
        }
        if ("POST".equals(request.getMethod()) && !token.equals(request.getParameter("token"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }
}

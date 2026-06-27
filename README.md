<div align="center">

# TickTrack

**_Sistem Manajemen Helpdesk & Tiket Skala Enterprise._**

Aplikasi Android native untuk pelaporan dan penanganan masalah secara *real-time*.
Dikembangkan untuk menghadirkan pengalaman pengguna kelas atas dalam manajemen kendala operasional, lengkap dengan dasbor analitik dan keamanan standar industri.
<br/>**Arnest Suhendra** (2350081054) • **Bagas Ibnu Abdillah** (2350081074) • **Putra Michael Sitohang** (2350081087)

![version](https://img.shields.io/badge/version-1.0.0-4CAF50?style=flat-square)
![platform](https://img.shields.io/badge/platform-Android-3DDC84?style=flat-square)
![min SDK](https://img.shields.io/badge/minSdk-24-E8F5E9?style=flat-square&labelColor=2E7D32)
![language](https://img.shields.io/badge/language-Java%208-B07219?style=flat-square)
![database](https://img.shields.io/badge/database-MySQL%20%28JDBC%29-4479A1?style=flat-square)

</div>

---

## 📖 Tentang

TickTrack adalah aplikasi *helpdesk* berbasis Android yang memungkinkan kolaborasi tanpa hambatan antara pengguna (*Users*) dan administrator (*Admins*). Pengguna dapat dengan mudah membuat tiket pelaporan masalah, berinteraksi layaknya obrolan pesan (*chat-style*) dengan admin, serta memonitor status penyelesaian. Bagi administrator, TickTrack menyediakan *control-center* penuh mulai dari pelacakan analitik hingga pengelolaan akun secara komprehensif.

Aplikasi ini dibangun menggunakan arsitektur **Native Android (Java)** dan terhubung langsung secara asinkron ke server **MySQL** melalui JDBC, memastikan latensi data yang sangat minim.

---

## 🌟 Fitur Utama (v1.0.0 Production Ready)

Aplikasi ini memiliki 10 modul ekosistem inti yang berjalan dengan sinkronisasi penuh:

1. **🔒 Autentikasi (Login/Register)**
   Sistem login aman dengan enkripsi password menggunakan algoritma hashing **BCrypt**.
   
2. **🏠 Dashboard Berbasis Peran**
   Tampilan *dashboard* dinamis yang beradaptasi dengan peran pengguna. Admin melihat metrik statistik seluruh sistem, sementara User memantau tiket pribadinya.
   
3. **🎫 Manajemen Tiket (Ticket List)**
   Kemampuan untuk melakukan filter, urutkan (sort), dan mencari tiket. Memanfaatkan UI bersih ala *Material Design* lengkap dengan animasi *Shimmer Loading*.
   
4. **💬 Interaksi Detail Tiket (Chat & Timeline)**
   Ruang diskusi interaktif (*real-time chat*) di dalam setiap tiket untuk mempermudah komunikasi penyelesaian antara pelapor dan tim teknisi.
   
5. **🗂️ Manajemen Pengguna (Admin Only)**
   Fitur administratif untuk melihat daftar akun, mencari profil, dan menonaktifkan pengguna (*Deactivate Account*) yang melanggar ketentuan.
   
6. **🏷️ Manajemen Kategori (Admin Only)**
   Sistem operasi CRUD *(Create, Read, Update, Delete)* untuk entitas kategori tiket, dilengkapi dengan manajemen kode warna (Hex Color) dan proteksi penghapusan relasional.
   
7. **🔔 Pusat Notifikasi Berbasis Event**
   Pengiriman notifikasi otomatis *(event-driven)* saat ada tiket baru, penolakan, penutupan, maupun pesan baru. Cukup klik untuk melompat langsung ke halaman terkait.
   
8. **📜 Riwayat Aktivitas & Jejak Audit**
   Audit aktivitas terperinci yang mencatat waktu, aksi, dan aktor (Siapa melakukan Apa) untuk transparansi SLA *(Service Level Agreement)* yang profesional.
   
9. **📊 Laporan & Analitik (Reports)**
   Halaman dasbor layar penuh eksklusif yang memuat 6 diagram statistik interaktif ditenagai oleh **MPAndroidChart** (Tren Harian/Bulanan, Komposisi Kategori/Status, Top Users).
   
10. **⚙️ Pengaturan Profil (Settings & Upload)**
    Fasilitas manajemen akun di mana pengguna dapat mengedit data diri, mengganti kata sandi, dan memotong *(crop)* lalu mengunggah foto profil menggunakan pustaka **UCrop**.

---

## 🛠️ Tech Stack

- **Bahasa**: Java 8
- **IDE**: Android Studio
- **Min SDK / Target / Compile**: 24 / 36 / 36
- **Database Architecture**: Direct JDBC Connector (MySQL) dengan `ExecutorService` untuk multithreading di luar UI.
- **Keamanan**: `jBcrypt` (Hashing) & IDOR Protection di level *query*.
- **Library Terkemuka**: 
  - `MPAndroidChart` (v3.1.0) untuk rendering grafik tingkat lanjut.
  - `UCrop` untuk utilitas manipulasi rasio gambar profil.
  - `Facebook Shimmer` untuk skeleton loading *feed*.
  - `Lottie` (6.1.0) untuk animasi interaktif status kosong (Empty States).
  - Material Design 3 (M3) UI Components.

---

## 🚀 Cara Build & Instalasi

### 1. Prasyarat
- Android Studio versi terbaru
- JDK 17 / JDK 8 (Dikonfigurasi melalui Android Studio)
- Server MySQL (XAMPP/Cloud) yang sudah menjalankan skema *database* TickTrack.

### 2. Setup Repositori
```bash
git clone https://github.com/ptramichsthg/ticktrack_app.git
cd ticktrack_app
```
Buka *project* melalui Android Studio, pilih **Trust project**, lalu tunggu Gradle melakukan sinkronisasi dependensi.

### 3. Konfigurasi Database
Karena aplikasi ini terhubung ke MySQL melalui JDBC, Anda harus menyesuaikan konfigurasi pada kelas `DatabaseConnection.java` yang berada di direktori `com.example.ticktrack.db`:
- Sesuaikan IP `HOST` server (Misal: `10.0.2.2` untuk localhost emulator).
- Masukkan `USERNAME` dan `PASSWORD` database MySQL Anda.
- Sesuaikan `DATABASE_NAME`.

### 4. Build APK & Jalankan
Untuk mengekspor versi *Debug*:
```bash
./gradlew assembleDebug
```
File APK siap instal akan ter-generate di folder `app/build/outputs/apk/debug/app-debug.apk`. 
Atau, cukup tekan ikon ▶️ **Run** (Shift+F10) di Android Studio untuk meluncurkan langsung ke perangkat/emulator Anda.

---

<div align="center">

*TickTrack — Percepat Resolusi, Tingkatkan Efisiensi.*

</div>

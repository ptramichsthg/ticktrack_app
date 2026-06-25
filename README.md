<div align="center">

# TickTrack

**_Sistem Manajemen Helpdesk & Tiket yang Terintegrasi._**

Aplikasi Android native untuk pelaporan dan penanganan masalah secara real-time.
Tugas besar mata kuliah **Mobile Programming**. Dikembangkan oleh kelompok yang beranggotakan 3 orang:
<br/>**Arnest Suhendra** (2350081054) • **Bagas Ibnu Abdillah** (2350081074) • **Putra Michael Sitohang** (2350081087)

![version](https://img.shields.io/badge/version-1.0.0-4CAF50?style=flat-square)
![platform](https://img.shields.io/badge/platform-Android-3DDC84?style=flat-square)
![min SDK](https://img.shields.io/badge/minSdk-24-E8F5E9?style=flat-square&labelColor=2E7D32)
![language](https://img.shields.io/badge/language-Java%208-B07219?style=flat-square)
![database](https://img.shields.io/badge/database-MySQL%20%28JDBC%29-4479A1?style=flat-square)

</div>

---

## Tentang

TickTrack adalah aplikasi helpdesk berbasis Android yang memungkinkan kolaborasi tanpa hambatan antara pengguna dan administrator. Pengguna dapat dengan mudah membuat tiket pelaporan masalah, berinteraksi layaknya obrolan pesan (chat-style) dengan admin, serta memonitor status penyelesaian. Aplikasi ini dibangun dengan fokus pada efisiensi, performa, dan antarmuka yang bersih serta profesional.

Tiga nilai inti yang mengarahkan setiap keputusan desain dan kode:

- **Efisien** — pengelolaan tiket yang cepat dan terstruktur
- **Transparan** — riwayat respons dan status tiket dapat dipantau secara real-time
- **Intuitif** — antarmuka pengguna yang modern dengan animasi halus dan visualisasi data

---

## Fitur

Aplikasi ini mencakup lebih dari 10 modul utama untuk memastikan kelancaran alur operasional helpdesk. Berikut rinciannya:

| Kode | Fitur | Catatan |
|---|---|---|
| F-01 | Registrasi Akun | Pendaftaran pengguna baru dengan enkripsi password menggunakan library `jBcrypt` |
| F-02 | Autentikasi Login | Keamanan akses berlapis dengan validasi sesi dan deteksi peran otomatis (User/Admin) |
| F-03 | Dashboard Utama | Ringkasan statistik (Total, Selesai, Tertunda) dengan grafik visual interaktif `MPAndroidChart` |
| F-04 | Pelaporan (Buat Tiket) | Form input untuk melaporkan insiden/kendala dengan parameter Kategori, Prioritas, dan Deskripsi |
| F-05 | Riwayat & Daftar Tiket | Timeline `RecyclerView` responsif dengan fitur tarik-untuk-menyegarkan (*Swipe-to-Refresh*) |
| F-06 | Detail Tiket Bergaya Chat | UI interaktif menyerupai ruang obrolan (chat-room) untuk membaca alur penyelesaian masalah |
| F-07 | Balasan Tiket (Replies) | Kolom interaktif pada detail tiket yang memungkinkan percakapan antara Pelapor dan Admin |
| F-08 | Manajemen Profil | Halaman khusus yang menampilkan ringkasan informasi personal dan peran pengguna saat ini |
| F-09 | Edit Data Diri | Formulir pembaruan identitas yang langsung tersinkronisasi (real-time) dengan database MySQL |
| F-10 | Keamanan Kata Sandi | Fasilitas ganti kata sandi berlapis, mensyaratkan validasi password lama yang ter-hash |
| F-11 | Empty States & Animasi | Penanganan skenario "tanpa data" yang ramah (Lottie) serta skeleton loading (Shimmer) |
| F-12 | Role-Based Access (RBAC) | Tampilan dan izin akses fungsi otomatis menyesuaikan entitas: Pengguna Biasa vs Admin |

---

## Tech stack

- **Bahasa**: Java 8
- **IDE**: Android Studio
- **Min SDK / Target / Compile**: 24 / 36 / 36
- **Konektivitas Database**: Native JDBC Connector (MySQL) untuk koneksi database langsung
- **Keamanan**: `jBcrypt` untuk pencocokan dan hashing password standar industri
- **UI & Animasi**: 
  - Material Design Components
  - ViewBinding untuk interaksi UI yang aman
  - `Lottie` (6.1.0) untuk animasi vektor ringan
  - `Facebook Shimmer` untuk efek skeleton loading
  - `Glide` (4.16.0) untuk manajemen pemuatan gambar
- **Visualisasi Data**: `MPAndroidChart` (v3.1.0) untuk grafik dashboard
- **Lainnya**: SwipeRefreshLayout untuk fungsi pull-to-refresh

---

## Cara build

### Prasyarat

- Android Studio versi terbaru
- JDK 17 / JDK 8 (Dikonfigurasi melalui Android Studio)
- Android SDK Platform 36
- Database MySQL yang sudah berjalan dengan skema TickTrack

### Setup

```bash
git clone https://github.com/USERNAME_ANDA/TickTrack-Android.git
cd TickTrack-Android
```

Buka folder di Android Studio, **Trust project**, dan biarkan Gradle melakukan sinkronisasi otomatis.

### Konfigurasi Database

Karena aplikasi ini menggunakan JDBC secara langsung, pastikan konfigurasi koneksi database Anda sudah sesuai pada `DbConnection.java` atau file konfigurasi terkait di package `com.example.ticktrack.db`. Sesuaikan:
- IP/Host Server (Misal: `10.0.2.2` untuk localhost dari emulator Android)
- Username
- Password
- Nama Database

### Build APK debug

```bash
./gradlew assembleDebug
```

APK akan berada di `app/build/outputs/apk/debug/app-debug.apk`.

### Run di emulator / perangkat

Di Android Studio: Tekan `Shift + F10`, atau pilih device target di toolbar lalu klik tombol **Run**.

---

## Struktur kode

```text
app/src/main/
├── java/com/example/ticktrack/
│   ├── activities/                 # Activity untuk Login, Register, Edit Profile, dll
│   ├── adapters/                   # RecyclerView Adapter (Tiket, Balasan Chat)
│   ├── db/                         # Kelas konfigurasi JDBC dan eksekusi query
│   ├── fragments/                  # Fragment utama (Dashboard, List Tiket, Profile)
│   ├── listeners/                  # Interface callback (click listener, dll)
│   ├── models/                     # POJO/Model data (User, Ticket, Reply)
│   ├── session/                    # Pengelola Shared Preferences / Sesi User
│   └── utils/                      # Helper (Format tanggal, utilitas gambar, hashing)
└── res/                            
    ├── layout/                     # UI XML (activity, fragment, item_list, empty_state)
    ├── drawable/                   # Ikon dan background resource
    ├── menu/                       # Konfigurasi Bottom Navigation
    └── values/                     # Warna, strings, dan tema (themes.xml)
```

---

<div align="center">

_TickTrack — Solusi Helpdesk dalam Genggaman._

</div>

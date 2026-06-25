package com.example.ticktrack.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ticktrack.databinding.ActivityLoginBinding;
import com.example.ticktrack.db.DatabaseConnection;
import com.example.ticktrack.session.SessionManager;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private SessionManager session;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        if (session.isDarkMode()) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        if (session.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (email.isEmpty()) { binding.tilEmail.setError("Email wajib diisi"); return; }
        binding.tilEmail.setError(null);

        if (password.isEmpty() || password.length() < 6) { binding.tilPassword.setError("Minimal 6 karakter"); return; }
        binding.tilPassword.setError(null);

        binding.errorCard.setVisibility(View.GONE);
        setLoading(true);

        // DEV BYPASS: Dummy Login
        if (password.equals("password")) {
            if (email.equals("admin@example.com")) {
                setLoading(false);
                session.saveLoginSession("dummy", 999, "Admin Dummy", "admin@example.com", "admin");
                Toast.makeText(this, "Login DUMMY (Admin Mode)", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return;
            } else if (email.equals("user@example.com")) {
                setLoading(false);
                session.saveLoginSession("dummy", 888, "User Dummy", "user@example.com", "user");
                Toast.makeText(this, "Login DUMMY (User Mode)", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return;
            }
        }

        executorService.execute(() -> {
            try {
                Connection connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    String query = "SELECT * FROM users WHERE email = ?";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setString(1, email);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        String hashedPassword = rs.getString("password");
                        boolean isPasswordMatch = false;

                        if (hashedPassword.startsWith("$2y$")) {
                            hashedPassword = hashedPassword.replaceFirst("\\$2y\\$", "\\$2a\\$");
                        }

                        if (BCrypt.checkpw(password, hashedPassword)) {
                            isPasswordMatch = true;
                        }

                        if (isPasswordMatch) {
                            int userId = rs.getInt("id");
                            String name = rs.getString("name");
                            String role = rs.getString("role");
                            
                            mainHandler.post(() -> {
                                setLoading(false);
                                session.saveLoginSession("", userId, name, email, role);
                                Toast.makeText(this, "Selamat datang, " + name, Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            });
                        } else {
                            mainHandler.post(() -> {
                                setLoading(false);
                                binding.errorCard.setVisibility(View.VISIBLE);
                                binding.tvError.setText("Email atau password salah");
                            });
                        }
                    } else {
                        mainHandler.post(() -> {
                            setLoading(false);
                            binding.errorCard.setVisibility(View.VISIBLE);
                            binding.tvError.setText("Email tidak ditemukan");
                        });
                    }
                    connection.close();
                } else {
                    mainHandler.post(() -> {
                        setLoading(false);
                        binding.errorCard.setVisibility(View.VISIBLE);
                        binding.tvError.setText("Koneksi database gagal");
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    binding.errorCard.setVisibility(View.VISIBLE);
                    binding.tvError.setText("Error: " + e.getMessage());
                });
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.btnLogin.setEnabled(!isLoading);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setText(isLoading ? "Memproses..." : "Masuk");
    }
}

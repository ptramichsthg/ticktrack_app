package com.example.ticktrack.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ticktrack.databinding.ActivityRegisterBinding;
import com.example.ticktrack.db.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.tvLoginLink.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (name.isEmpty()) { binding.tilName.setError("Nama wajib diisi"); return; }
        binding.tilName.setError(null);

        if (email.isEmpty()) { binding.tilEmail.setError("Email wajib diisi"); return; }
        binding.tilEmail.setError(null);

        if (password.isEmpty() || password.length() < 6) { binding.tilPassword.setError("Minimal 6 karakter"); return; }
        binding.tilPassword.setError(null);

        if (!password.equals(confirmPassword)) { binding.tilConfirmPassword.setError("Password tidak sama"); return; }
        binding.tilConfirmPassword.setError(null);

        binding.errorCard.setVisibility(View.GONE);
        setLoading(true);

        executorService.execute(() -> {
            try {
                Connection connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    String query = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, 'user')";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setString(1, name);
                    stmt.setString(2, email);
                    // Hash with BCrypt to match CodeIgniter 4
                    String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
                    // Replace to 2y to match CI4
                    hashed = hashed.replaceFirst("\\$2a\\$", "\\$2y\\$");
                    stmt.setString(3, hashed);

                    int result = stmt.executeUpdate();
                    if (result > 0) {
                        mainHandler.post(() -> {
                            setLoading(false);
                            Toast.makeText(this, "Pendaftaran berhasil, silakan login", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    } else {
                        mainHandler.post(() -> {
                            setLoading(false);
                            binding.errorCard.setVisibility(View.VISIBLE);
                            binding.tvError.setText("Gagal mendaftar");
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
        binding.btnRegister.setEnabled(!isLoading);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setText(isLoading ? "Memproses..." : "Daftar");
    }
}

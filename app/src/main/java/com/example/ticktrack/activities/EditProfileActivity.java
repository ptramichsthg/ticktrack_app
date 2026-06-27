package com.example.ticktrack.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ticktrack.databinding.ActivityEditProfileBinding;
import com.example.ticktrack.db.DatabaseConnection;
import com.example.ticktrack.session.SessionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditProfileActivity extends AppCompatActivity {
    private ActivityEditProfileBinding binding;
    private SessionManager session;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Load existing data
        binding.etName.setText(session.getName());
        binding.etEmail.setText(session.getEmail());
        binding.etPhone.setText(session.getPhone());

        binding.btnSave.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();

        if (name.isEmpty()) { binding.tilName.setError("Nama wajib diisi"); return; }
        binding.tilName.setError(null);
        if (email.isEmpty()) { binding.tilEmail.setError("Email wajib diisi"); return; }
        binding.tilEmail.setError(null);

        setLoading(true);

        executorService.execute(() -> {
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                conn = DatabaseConnection.getConnection();
                if (conn != null) {
                    String query = "UPDATE users SET name = ?, email = ?, phone = ? WHERE id = ?";
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, name);
                    stmt.setString(2, email);
                    stmt.setString(3, phone);
                    stmt.setInt(4, session.getUserId());

                    int result = stmt.executeUpdate();
                    if (result > 0) {
                        mainHandler.post(() -> {
                            session.updateProfile(name, email, phone);
                            Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
                            setLoading(false);
                            finish();
                        });
                    } else {
                        mainHandler.post(() -> {
                            setLoading(false);
                            Toast.makeText(this, "Gagal memperbarui profil", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    mainHandler.post(() -> {
                        setLoading(false);
                        Toast.makeText(this, "Koneksi database gagal", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } finally {
                try { if(stmt != null) { stmt.close(); } } catch(Exception ignored){}
                try { if(conn != null) { conn.close(); } } catch(Exception ignored){}
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.btnSave.setEnabled(!isLoading);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnSave.setText(isLoading ? "Menyimpan..." : "Simpan Perubahan");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

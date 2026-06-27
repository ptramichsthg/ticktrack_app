package com.example.ticktrack.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ticktrack.databinding.ActivityChangePasswordBinding;
import com.example.ticktrack.db.DatabaseConnection;
import com.example.ticktrack.session.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChangePasswordActivity extends AppCompatActivity {
    private ActivityChangePasswordBinding binding;
    private SessionManager session;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.btnSave.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String oldPassword = binding.etOldPassword.getText().toString().trim();
        String newPassword = binding.etNewPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (oldPassword.isEmpty()) { binding.tilOldPassword.setError("Password lama wajib diisi"); return; }
        binding.tilOldPassword.setError(null);

        if (newPassword.isEmpty() || newPassword.length() < 6) { binding.tilNewPassword.setError("Minimal 6 karakter"); return; }
        binding.tilNewPassword.setError(null);

        if (!newPassword.equals(confirmPassword)) { binding.tilConfirmPassword.setError("Konfirmasi password tidak cocok"); return; }
        binding.tilConfirmPassword.setError(null);

        setLoading(true);

        executorService.execute(() -> {
            Connection conn = null;
            PreparedStatement selectStmt = null;
            PreparedStatement updateStmt = null;
            ResultSet rs = null;
            try {
                conn = DatabaseConnection.getConnection();
                if (conn != null) {
                    // Cek password lama
                    String query = "SELECT password FROM users WHERE id = ?";
                    selectStmt = conn.prepareStatement(query);
                    selectStmt.setInt(1, session.getUserId());
                    rs = selectStmt.executeQuery();

                    if (rs.next()) {
                        String hashedPassword = rs.getString("password");
                        if (hashedPassword.startsWith("$2y$")) {
                            hashedPassword = hashedPassword.replaceFirst("\\$2y\\$", "\\$2a\\$");
                        }

                        if (BCrypt.checkpw(oldPassword, hashedPassword)) {
                            // Update password baru
                            String updateQuery = "UPDATE users SET password = ? WHERE id = ?";
                            updateStmt = conn.prepareStatement(updateQuery);
                            String newHashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                            newHashed = newHashed.replaceFirst("\\$2a\\$", "\\$2y\\$");
                            updateStmt.setString(1, newHashed);
                            updateStmt.setInt(2, session.getUserId());

                            int result = updateStmt.executeUpdate();
                            if (result > 0) {
                                mainHandler.post(() -> {
                                    setLoading(false);
                                    new MaterialAlertDialogBuilder(this)
                                        .setTitle("Berhasil")
                                        .setMessage("Password berhasil diubah.")
                                        .setPositiveButton("OK", (dialog, which) -> finish())
                                        .setCancelable(false)
                                        .show();
                                });
                            } else {
                                mainHandler.post(() -> {
                                    setLoading(false);
                                    Toast.makeText(this, "Gagal mengubah password", Toast.LENGTH_SHORT).show();
                                });
                            }
                        } else {
                            mainHandler.post(() -> {
                                setLoading(false);
                                binding.tilOldPassword.setError("Password lama salah");
                            });
                        }
                    } else {
                        mainHandler.post(() -> {
                            setLoading(false);
                            Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show();
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
                try { if(rs != null) { rs.close(); } } catch(Exception ignored){}
                try { if(selectStmt != null) { selectStmt.close(); } } catch(Exception ignored){}
                try { if(updateStmt != null) { updateStmt.close(); } } catch(Exception ignored){}
                try { if(conn != null) { conn.close(); } } catch(Exception ignored){}
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.btnSave.setEnabled(!isLoading);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnSave.setText(isLoading ? "Menyimpan..." : "Ubah Password");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

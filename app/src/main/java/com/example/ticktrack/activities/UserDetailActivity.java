package com.example.ticktrack.activities;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ticktrack.databinding.ActivityUserDetailBinding;
import com.example.ticktrack.db.DatabaseConnection;
import com.example.ticktrack.session.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserDetailActivity extends AppCompatActivity {
    private ActivityUserDetailBinding binding;
    private SessionManager session;
    private ExecutorService executorService;
    private Handler mainHandler;
    private int targetUserId;
    private boolean isUserActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        if (!"admin".equals(session.getRole())) {
            finish();
            return;
        }

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        targetUserId = getIntent().getIntExtra("user_id", -1);
        if (targetUserId == -1) {
            finish();
            return;
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        binding.btnSave.setOnClickListener(v -> saveUserData());
        binding.btnToggleStatus.setOnClickListener(v -> toggleUserStatus());

        loadUserDetail();
    }

    private void loadUserDetail() {
        binding.progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    String query = "SELECT u.*, " +
                            "(SELECT COUNT(*) FROM tickets t WHERE t.user_id = u.id) as total_tickets " +
                            "FROM users u WHERE u.id = ?";
                    stmt = connection.prepareStatement(query);
                    stmt.setInt(1, targetUserId);
                    rs = stmt.executeQuery();

                    if (rs.next()) {
                        String name = rs.getString("name");
                        String email = rs.getString("email");
                        int totalTickets = rs.getInt("total_tickets");
                        try { isUserActive = rs.getBoolean("is_active"); } catch (Exception ignored) {}

                        mainHandler.post(() -> {
                            binding.etName.setText(name);
                            binding.etEmail.setText(email);
                            binding.tvTotalTickets.setText(String.valueOf(totalTickets));
                            updateStatusUI();
                            binding.progressBar.setVisibility(View.GONE);
                        });
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } finally {
                try { if(rs != null) rs.close(); } catch(Exception ignored){}
                try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                try { if(connection != null) connection.close(); } catch(Exception ignored){}
            }
        });
    }

    private void updateStatusUI() {
        if (isUserActive) {
            binding.tvStatus.setText("AKTIF");
            binding.cvStatusBadge.setCardBackgroundColor(Color.parseColor("#10B981")); // Green
            binding.btnToggleStatus.setText("Nonaktifkan User");
            binding.btnToggleStatus.setTextColor(Color.parseColor("#EF4444")); // Red text
            binding.btnToggleStatus.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#EF4444")));
        } else {
            binding.tvStatus.setText("NONAKTIF");
            binding.cvStatusBadge.setCardBackgroundColor(Color.parseColor("#EF4444")); // Red
            binding.btnToggleStatus.setText("Aktifkan kembali User");
            binding.btnToggleStatus.setTextColor(Color.parseColor("#10B981")); // Green text
            binding.btnToggleStatus.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#10B981")));
        }
    }

    private void saveUserData() {
        String newName = binding.etName.getText().toString().trim();
        String newEmail = binding.etEmail.getText().toString().trim();

        if (newName.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Nama dan Email tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSave.setEnabled(false);
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    stmt = connection.prepareStatement("UPDATE users SET name = ?, email = ? WHERE id = ?");
                    stmt.setString(1, newName);
                    stmt.setString(2, newEmail);
                    stmt.setInt(3, targetUserId);
                    
                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        mainHandler.post(() -> Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show());
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, "Gagal memperbarui: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } finally {
                try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                try { if(connection != null) connection.close(); } catch(Exception ignored){}
                mainHandler.post(() -> binding.btnSave.setEnabled(true));
            }
        });
    }

    private void toggleUserStatus() {
        String actionTitle = isUserActive ? "Nonaktifkan Akun" : "Aktifkan Akun";
        String actionMsg = isUserActive ? 
                "User ini tidak akan bisa login ke aplikasi lagi. Anda yakin?" : 
                "User ini akan diizinkan kembali mengakses aplikasi. Anda yakin?";

        new MaterialAlertDialogBuilder(this)
            .setTitle(actionTitle)
            .setMessage(actionMsg)
            .setPositiveButton("Ya", (dialog, which) -> {
                binding.btnToggleStatus.setEnabled(false);
                executorService.execute(() -> {
                    Connection connection = null;
                    PreparedStatement stmt = null;
                    try {
                        connection = DatabaseConnection.getConnection();
                        if (connection != null) {
                            stmt = connection.prepareStatement("UPDATE users SET is_active = ? WHERE id = ?");
                            stmt.setBoolean(1, !isUserActive);
                            stmt.setInt(2, targetUserId);
                            
                            int rows = stmt.executeUpdate();
                            if (rows > 0) {
                                isUserActive = !isUserActive;
                                mainHandler.post(() -> {
                                    Toast.makeText(this, "Status pengguna berhasil diubah", Toast.LENGTH_SHORT).show();
                                    updateStatusUI();
                                });
                            }
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> Toast.makeText(this, "Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } finally {
                        try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                        try { if(connection != null) connection.close(); } catch(Exception ignored){}
                        mainHandler.post(() -> binding.btnToggleStatus.setEnabled(true));
                    }
                });
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

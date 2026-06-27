package com.example.ticktrack.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ticktrack.databinding.ActivityCreateTicketBinding;
import com.example.ticktrack.db.DatabaseConnection;
import com.example.ticktrack.session.SessionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;

public class CreateTicketActivity extends AppCompatActivity {
    private ActivityCreateTicketBinding binding;
    private SessionManager session;
    private ExecutorService executorService;
    private Handler mainHandler;
    private List<Integer> categoryIds = new ArrayList<>();
    private List<String> categoryNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateTicketBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        session = new SessionManager(this);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        String[] priorities = {"low", "medium", "high", "urgent"};
        String[] priorityLabels = {"Rendah", "Sedang", "Tinggi", "Mendesak"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, priorityLabels);
        binding.spinnerPriority.setAdapter(priorityAdapter);

        loadCategories();

        binding.btnSubmit.setOnClickListener(v -> submitTicket(priorities));
    }

    private void loadCategories() {
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    String query = "SELECT id, name FROM categories ORDER BY name ASC";
                    stmt = connection.prepareStatement(query);
                    rs = stmt.executeQuery();
                    
                    categoryIds.clear();
                    categoryNames.clear();
                    
                    while (rs.next()) {
                        categoryIds.add(rs.getInt("id"));
                        categoryNames.add(rs.getString("name"));
                    }
                    
                    mainHandler.post(() -> {
                        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categoryNames);
                        binding.spinnerCategory.setAdapter(categoryAdapter);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { if (rs != null) rs.close(); } catch (Exception ignored) {}
                try { if (stmt != null) stmt.close(); } catch (Exception ignored) {}
                try { if (connection != null) connection.close(); } catch (Exception ignored) {}
            }
        });
    }

    private void submitTicket(String[] priorities) {
        String title = binding.etTitle.getText().toString().trim();
        String desc = binding.etDescription.getText().toString().trim();
        int priorityIndex = binding.spinnerPriority.getSelectedItemPosition();

        if (title.isEmpty() || title.length() < 5) { binding.tilTitle.setError("Judul minimal 5 karakter"); return; }
        binding.tilTitle.setError(null);

        if (desc.isEmpty() || desc.length() < 10) { binding.tilDescription.setError("Deskripsi minimal 10 karakter"); return; }
        binding.tilDescription.setError(null);

        if (categoryIds.isEmpty()) {
            Toast.makeText(this, "Kategori belum dimuat, silakan tunggu", Toast.LENGTH_SHORT).show();
            return;
        }

        String priority = priorities[priorityIndex];
        int categoryIndex = binding.spinnerCategory.getSelectedItemPosition();
        int categoryId = categoryIds.get(categoryIndex);

        setLoading(true);

        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement checkStmt = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    // Generate TK code
                    String checkQuery = "SELECT MAX(id) as max_id FROM tickets";
                    checkStmt = connection.prepareStatement(checkQuery);
                    rs = checkStmt.executeQuery();
                    int nextId = 1;
                    if (rs.next()) {
                        nextId = rs.getInt("max_id") + 1;
                    }
                    String code = String.format("TK-%06d", nextId);

                    String query = "INSERT INTO tickets (code, user_id, category_id, title, description, priority, status) VALUES (?, ?, ?, ?, ?, ?, 'open')";
                    stmt = connection.prepareStatement(query);
                    stmt.setString(1, code);
                    stmt.setInt(2, session.getUserId());
                    stmt.setInt(3, categoryId);
                    stmt.setString(4, title);
                    stmt.setString(5, desc);
                    stmt.setString(6, priority);

                    int result = stmt.executeUpdate();
                    if (result > 0) {
                        mainHandler.post(() -> {
                            setLoading(false);
                            Toast.makeText(this, "Tiket berhasil dibuat!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    } else {
                        mainHandler.post(() -> {
                            setLoading(false);
                            Toast.makeText(this, "Gagal membuat tiket", Toast.LENGTH_SHORT).show();
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
                try { if(rs != null) rs.close(); } catch(Exception ignored){}
                try { if(checkStmt != null) checkStmt.close(); } catch(Exception ignored){}
                try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                try { if(connection != null) connection.close(); } catch(Exception ignored){}
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.btnSubmit.setEnabled(!isLoading);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

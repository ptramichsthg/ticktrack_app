package com.example.ticktrack.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ticktrack.R;
import com.example.ticktrack.adapters.CategoryAdapter;
import com.example.ticktrack.databinding.ActivityCategoryManagementBinding;
import com.example.ticktrack.db.DatabaseConnection;
import com.example.ticktrack.models.CategoryModel;
import com.example.ticktrack.session.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.widget.ArrayAdapter;

public class CategoryManagementActivity extends AppCompatActivity {
    private ActivityCategoryManagementBinding binding;
    private SessionManager session;
    private ExecutorService executorService;
    private Handler mainHandler;
    private CategoryAdapter adapter;
    private List<CategoryModel> categories = new ArrayList<>();

    // Standard colors for categories
    private final String[] colorNames = {"Biru (Blue)", "Merah (Red)", "Hijau (Green)", "Kuning (Yellow)", "Ungu (Purple)"};
    private final String[] colorHex = {"#3B82F6", "#EF4444", "#10B981", "#F59E0B", "#8B5CF6"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        if (!"admin".equals(session.getRole())) {
            finish();
            return;
        }

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new CategoryAdapter(new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onEditClick(CategoryModel category) {
                showCategoryDialog(category);
            }
            @Override
            public void onDeleteClick(CategoryModel category) {
                confirmDeleteCategory(category);
            }
        });
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCategories.setAdapter(adapter);

        binding.fabAddCategory.setOnClickListener(v -> showCategoryDialog(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategories();
    }

    private void loadCategories() {
        binding.progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            List<CategoryModel> loaded = new ArrayList<>();
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    String query = "SELECT c.*, " +
                            "(SELECT COUNT(*) FROM tickets t WHERE t.category_id = c.id) as total_tickets " +
                            "FROM categories c ORDER BY c.name ASC";
                    stmt = connection.prepareStatement(query);
                    rs = stmt.executeQuery();

                    while (rs.next()) {
                        String color = "#3B82F6"; // default
                        try {
                            String dbColor = rs.getString("color");
                            if (dbColor != null) color = dbColor;
                        } catch (Exception ignored) {}
                        
                        loaded.add(new CategoryModel(
                                rs.getInt("id"),
                                rs.getString("name"),
                                color,
                                rs.getInt("total_tickets")
                        ));
                    }
                    
                    mainHandler.post(() -> {
                        categories = loaded;
                        adapter.setCategories(categories);
                        binding.progressBar.setVisibility(View.GONE);
                    });
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

    private void showCategoryDialog(CategoryModel category) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category, null);
        EditText etName = dialogView.findViewById(R.id.etCategoryName);
        Spinner spinnerColor = dialogView.findViewById(R.id.spinnerColor);
        
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, colorNames);
        spinnerColor.setAdapter(colorAdapter);

        if (category != null) {
            etName.setText(category.getName());
            for (int i = 0; i < colorHex.length; i++) {
                if (colorHex[i].equalsIgnoreCase(category.getColor())) {
                    spinnerColor.setSelection(i);
                    break;
                }
            }
        }

        String title = category == null ? "Tambah Kategori Baru" : "Edit Kategori";
        new MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Simpan", (dialog, which) -> {
                String name = etName.getText().toString().trim();
                String color = colorHex[spinnerColor.getSelectedItemPosition()];
                if (name.isEmpty()) {
                    Toast.makeText(this, "Nama kategori tidak boleh kosong", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveCategory(category == null ? -1 : category.getId(), name, color);
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void saveCategory(int id, String name, String color) {
        binding.progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    if (id == -1) {
                        stmt = connection.prepareStatement("INSERT INTO categories (name, color) VALUES (?, ?)");
                        stmt.setString(1, name);
                        stmt.setString(2, color);
                    } else {
                        stmt = connection.prepareStatement("UPDATE categories SET name = ?, color = ? WHERE id = ?");
                        stmt.setString(1, name);
                        stmt.setString(2, color);
                        stmt.setInt(3, id);
                    }
                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        mainHandler.post(() -> {
                            Toast.makeText(this, "Kategori berhasil disimpan", Toast.LENGTH_SHORT).show();
                            loadCategories();
                        });
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } finally {
                try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                try { if(connection != null) connection.close(); } catch(Exception ignored){}
            }
        });
    }

    private void confirmDeleteCategory(CategoryModel category) {
        if (category.getTotalTickets() > 0) {
            new MaterialAlertDialogBuilder(this)
                .setTitle("Tidak Dapat Dihapus")
                .setMessage("Kategori ini sedang digunakan oleh " + category.getTotalTickets() + " tiket. Anda tidak bisa menghapusnya untuk menjaga keutuhan data.")
                .setPositiveButton("Mengerti", null)
                .show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle("Hapus Kategori")
            .setMessage("Yakin ingin menghapus kategori '" + category.getName() + "'?")
            .setPositiveButton("Hapus", (dialog, which) -> {
                binding.progressBar.setVisibility(View.VISIBLE);
                executorService.execute(() -> {
                    Connection connection = null;
                    PreparedStatement stmt = null;
                    try {
                        connection = DatabaseConnection.getConnection();
                        if (connection != null) {
                            stmt = connection.prepareStatement("DELETE FROM categories WHERE id = ?");
                            stmt.setInt(1, category.getId());
                            int rows = stmt.executeUpdate();
                            if (rows > 0) {
                                mainHandler.post(() -> {
                                    Toast.makeText(this, "Kategori berhasil dihapus", Toast.LENGTH_SHORT).show();
                                    loadCategories();
                                });
                            }
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "Gagal menghapus: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    } finally {
                        try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                        try { if(connection != null) connection.close(); } catch(Exception ignored){}
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

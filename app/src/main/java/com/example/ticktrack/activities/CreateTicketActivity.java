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
import android.content.Intent;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import android.database.Cursor;

public class CreateTicketActivity extends AppCompatActivity {
    private ActivityCreateTicketBinding binding;
    private SessionManager session;
    private ExecutorService executorService;
    private Handler mainHandler;
    private List<Integer> categoryIds = new ArrayList<>();
    private List<String> categoryNames = new ArrayList<>();
    
    private byte[] attachmentBytes = null;
    private String attachmentName = null;
    private String attachmentType = null;
    
    private ActivityResultLauncher<String> filePickerLauncher;

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

        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                processSelectedFile(uri);
            }
        });

        binding.btnAttach.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
        binding.btnSubmit.setOnClickListener(v -> submitTicket(priorities));
    }

    private void processSelectedFile(Uri uri) {
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                
                String fileName = cursor.getString(nameIndex);
                long fileSize = cursor.getLong(sizeIndex);
                
                if (fileSize > 2 * 1024 * 1024) { // 2 MB Limit
                    Toast.makeText(this, "Ukuran file maksimal 2MB", Toast.LENGTH_SHORT).show();
                    cursor.close();
                    return;
                }
                
                InputStream inputStream = getContentResolver().openInputStream(uri);
                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                int len = 0;
                while ((len = inputStream.read(buffer)) != -1) {
                    byteBuffer.write(buffer, 0, len);
                }
                
                attachmentBytes = byteBuffer.toByteArray();
                attachmentName = fileName;
                attachmentType = getContentResolver().getType(uri);
                if (attachmentType == null) {
                    attachmentType = "application/octet-stream";
                }
                
                binding.tvAttachmentName.setText(fileName);
                
                cursor.close();
                inputStream.close();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Gagal memproses file", Toast.LENGTH_SHORT).show();
        }
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
                    stmt = connection.prepareStatement(query, java.sql.Statement.RETURN_GENERATED_KEYS);
                    stmt.setString(1, code);
                    stmt.setInt(2, session.getUserId());
                    stmt.setInt(3, categoryId);
                    stmt.setString(4, title);
                    stmt.setString(5, desc);
                    stmt.setString(6, priority);

                    int result = stmt.executeUpdate();
                    if (result > 0) {
                        ResultSet keys = stmt.getGeneratedKeys();
                        if (keys.next()) {
                            int newTicketId = keys.getInt(1);
                            
                            // Insert Attachment if present
                            if (attachmentBytes != null) {
                                try (PreparedStatement attachStmt = connection.prepareStatement("INSERT INTO attachments (ticket_id, file_name, file_type, file_size, file_data) VALUES (?, ?, ?, ?, ?)")) {
                                    attachStmt.setInt(1, newTicketId);
                                    attachStmt.setString(2, attachmentName);
                                    attachStmt.setString(3, attachmentType);
                                    attachStmt.setInt(4, attachmentBytes.length);
                                    attachStmt.setBytes(5, attachmentBytes);
                                    attachStmt.executeUpdate();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            
                            try (PreparedStatement actStmt = connection.prepareStatement("INSERT INTO activities (ticket_id, user_id, action_type, description) VALUES (?, ?, ?, ?)")) {
                                actStmt.setInt(1, newTicketId);
                                actStmt.setInt(2, session.getUserId());
                                actStmt.setString(3, "TICKET_CREATED");
                                String roleStr = session.getRole().equals("admin") ? "Admin" : "User";
                                actStmt.setString(4, roleStr + " " + session.getName() + " membuat Ticket");
                                actStmt.executeUpdate();
                            } catch (Exception ignored) {}
                            
                            // Send notification to all admins
                            try (PreparedStatement notifStmt = connection.prepareStatement(
                                "INSERT INTO notifications (user_id, title, message, ticket_id) " +
                                "SELECT id, 'Tiket Baru Dibuat', ?, ? FROM users WHERE role = 'admin'")) {
                                notifStmt.setString(1, "Tiket " + code + " telah dibuat oleh " + session.getName());
                                notifStmt.setInt(2, newTicketId);
                                notifStmt.executeUpdate();
                            } catch (Exception ignored) {}
                        }
                        
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

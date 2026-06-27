package com.example.ticktrack.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ticktrack.adapters.ActivityHistoryAdapter;
import com.example.ticktrack.adapters.TicketReplyAdapter;
import com.example.ticktrack.databinding.ActivityTicketDetailBinding;
import com.example.ticktrack.db.DatabaseConnection;
import com.example.ticktrack.models.ActivityModel;
import com.example.ticktrack.models.TicketReply;
import com.example.ticktrack.session.SessionManager;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TicketDetailActivity extends AppCompatActivity {
    private ActivityTicketDetailBinding binding;
    private SessionManager session;
    private ExecutorService executorService;
    private Handler mainHandler;
    private TicketReplyAdapter replyAdapter;
    private int ticketId;
    private int ticketOwnerId = -1;
    private String ticketCodeGlobal = "";

    private final String[] statusValues = {"open", "in_progress", "resolved", "rejected"};
    private final String[] statusLabels = {"Open", "In Progress", "Resolved", "Rejected"};

    private final String[] priorityValues = {"low", "medium", "high", "critical"};
    private final String[] priorityLabels = {"Rendah", "Sedang", "Tinggi", "Mendesak"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTicketDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        session = new SessionManager(this);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        ticketId = getIntent().getIntExtra("ticket_id", -1);
        String ticketCode = getIntent().getStringExtra("ticket_code");
        
        binding.toolbar.setTitle("Tiket #" + ticketCode);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        replyAdapter = new TicketReplyAdapter();
        binding.rvReplies.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReplies.setAdapter(replyAdapter);

        if (session.getRole().equals("admin")) {
            binding.llAdminControl.setVisibility(View.VISIBLE);
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statusLabels);
            binding.spinnerStatus.setAdapter(spinnerAdapter);

            ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, priorityLabels);
            binding.spinnerPriority.setAdapter(priorityAdapter);

            binding.btnUpdateStatus.setOnClickListener(v -> updateTicketStatus());
        }

        binding.btnSelfClose.setOnClickListener(v -> selfCloseTicket());
        binding.btnSendReply.setOnClickListener(v -> sendReply());

        if (ticketId != -1) {
            loadTicketDetail();
            loadTimeline();
            loadReplies();
        }
    }

    private void loadTicketDetail() {
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    String query = "SELECT t.*, u.name as user_name, c.name as category_name " +
                                 "FROM tickets t " +
                                 "JOIN users u ON t.user_id = u.id " +
                                 "JOIN categories c ON t.category_id = c.id " +
                                 "WHERE t.id = ?";
                    stmt = connection.prepareStatement(query);
                    stmt.setInt(1, ticketId);
                    rs = stmt.executeQuery();

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                    if (rs.next()) {
                        String code = rs.getString("code");
                        ticketCodeGlobal = code != null ? code : "";
                        String title = rs.getString("title");
                        String desc = rs.getString("description");
                        String author = rs.getString("user_name");
                        String status = rs.getString("status");
                        String category = rs.getString("category_name");
                        String priority = rs.getString("priority");
                        
                        String createdAt = "";
                        if (rs.getTimestamp("created_at") != null) {
                            createdAt = dateFormat.format(rs.getTimestamp("created_at"));
                        }
                        String finalCreatedAt = createdAt;
                        
                        // IDOR Protection
                        ticketOwnerId = rs.getInt("user_id");
                        if (!session.getRole().equals("admin") && ticketOwnerId != session.getUserId()) {
                            mainHandler.post(() -> {
                                Toast.makeText(this, "Anda tidak memiliki akses ke tiket ini", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                            return;
                        }

                        mainHandler.post(() -> {
                            binding.tvTicketTitle.setText(title);
                            binding.tvTicketDescription.setText(desc);
                            binding.tvTicketAuthor.setText("Dibuat oleh: " + author + " • " + finalCreatedAt);
                            binding.tvTicketCategory.setText(category);
                            binding.tvTicketStatus.setText(status.toUpperCase());
                            
                            if (session.getRole().equals("admin")) {
                                for (int i = 0; i < statusValues.length; i++) {
                                    if (statusValues[i].equals(status)) {
                                        binding.spinnerStatus.setSelection(i);
                                        break;
                                    }
                                }
                                for (int i = 0; i < priorityValues.length; i++) {
                                    if (priorityValues[i].equals(priority)) {
                                        binding.spinnerPriority.setSelection(i);
                                        break;
                                    }
                                }
                            } else {
                                if (status.equals("open") || status.equals("in_progress")) {
                                    binding.llUserControl.setVisibility(View.VISIBLE);
                                } else {
                                    binding.llUserControl.setVisibility(View.GONE);
                                }
                            }
                            binding.progressBar.setVisibility(View.GONE);
                            
                            loadAttachmentInfo();
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

    private void loadReplies() {
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    String query = "SELECT r.*, u.name as user_name, u.role as user_role " +
                                 "FROM ticket_replies r " +
                                 "JOIN users u ON r.user_id = u.id " +
                                 "WHERE r.ticket_id = ? " +
                                 "ORDER BY r.created_at ASC";
                    stmt = connection.prepareStatement(query);
                    stmt.setInt(1, ticketId);
                    rs = stmt.executeQuery();

                    List<TicketReply> replies = new ArrayList<>();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                    while (rs.next()) {
                        TicketReply reply = new TicketReply();
                        reply.setId(rs.getInt("id"));
                        reply.setMessage(rs.getString("message"));
                        reply.setUserName(rs.getString("user_name"));
                        reply.setUserRole(rs.getString("user_role"));
                        if (rs.getTimestamp("created_at") != null) {
                            reply.setCreatedAt(dateFormat.format(rs.getTimestamp("created_at")));
                        }
                        replies.add(reply);
                    }

                    mainHandler.post(() -> {
                        replyAdapter.setReplies(replies);
                        if (!replies.isEmpty()) {
                            binding.rvReplies.scrollToPosition(replies.size() - 1);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { if(rs != null) rs.close(); } catch(Exception ignored){}
                try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                try { if(connection != null) connection.close(); } catch(Exception ignored){}
            }
        });
    }

    private void loadTimeline() {
        binding.rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            List<ActivityModel> activities = new ArrayList<>();
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    String query = "SELECT * FROM activities WHERE ticket_id = ? ORDER BY created_at ASC";
                    stmt = connection.prepareStatement(query);
                    stmt.setInt(1, ticketId);
                    rs = stmt.executeQuery();
                    
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                    
                    while (rs.next()) {
                        String createdAt = rs.getString("created_at");
                        try {
                            if (createdAt != null) createdAt = outputFormat.format(inputFormat.parse(createdAt));
                        } catch (Exception e) {}
                        
                        activities.add(new ActivityModel(
                            rs.getInt("id"),
                            rs.getString("action_type"),
                            rs.getString("description"),
                            createdAt
                        ));
                    }
                    
                    mainHandler.post(() -> {
                        ActivityHistoryAdapter adapter = new ActivityHistoryAdapter(activities);
                        binding.rvTimeline.setAdapter(adapter);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { if(rs != null) rs.close(); } catch(Exception ignored){}
                try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                try { if(connection != null) connection.close(); } catch(Exception ignored){}
            }
        });
    }

    private void selfCloseTicket() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Tandai Selesai")
            .setMessage("Apakah Anda yakin masalah ini sudah teratasi? Tiket akan ditutup.")
            .setPositiveButton("Ya, Selesai", (dialog, which) -> {
                binding.btnSelfClose.setEnabled(false);
                executorService.execute(() -> {
                    Connection connection = null;
                    PreparedStatement stmt = null;
                    try {
                        connection = DatabaseConnection.getConnection();
                        if (connection != null) {
                            String query = "UPDATE tickets SET status = 'resolved' WHERE id = ?";
                            stmt = connection.prepareStatement(query);
                            stmt.setInt(1, ticketId);
                            int result = stmt.executeUpdate();
                            if (result > 0) {
                                try (PreparedStatement actStmt = connection.prepareStatement("INSERT INTO activities (ticket_id, user_id, action_type, description) VALUES (?, ?, ?, ?)")) {
                                    actStmt.setInt(1, ticketId);
                                    actStmt.setInt(2, session.getUserId());
                                    actStmt.setString(3, "STATUS_UPDATED");
                                    String roleStr = session.getRole().equals("admin") ? "Admin" : "User";
                                    actStmt.setString(4, roleStr + " " + session.getName() + " Resolve Ticket");
                                    actStmt.executeUpdate();
                                } catch (Exception ignored) {}
                                
                                // Notify admins
                                try (PreparedStatement notifStmt = connection.prepareStatement(
                                    "INSERT INTO notifications (user_id, title, message, ticket_id) " +
                                    "SELECT id, 'Tiket Ditutup User', ?, ? FROM users WHERE role = 'admin'")) {
                                    notifStmt.setString(1, "Tiket " + ticketCodeGlobal + " telah ditutup oleh " + session.getName());
                                    notifStmt.setInt(2, ticketId);
                                    notifStmt.executeUpdate();
                                } catch (Exception ignored) {}
                                
                                mainHandler.post(() -> {
                                    Toast.makeText(this, "Tiket berhasil ditutup", Toast.LENGTH_SHORT).show();
                                    binding.llUserControl.setVisibility(View.GONE);
                                    binding.tvTicketStatus.setText("RESOLVED");
                                });
                            }
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } finally {
                        try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                        try { if(connection != null) connection.close(); } catch(Exception ignored){}
                        mainHandler.post(() -> binding.btnSelfClose.setEnabled(true));
                    }
                });
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void loadAttachmentInfo() {
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    stmt = connection.prepareStatement("SELECT id, file_name, file_size, file_type FROM attachments WHERE ticket_id = ?");
                    stmt.setInt(1, ticketId);
                    rs = stmt.executeQuery();
                    if (rs.next()) {
                        int attachId = rs.getInt("id");
                        String fileName = rs.getString("file_name");
                        int fileSize = rs.getInt("file_size");
                        String fileType = rs.getString("file_type");
                        
                        String sizeStr = String.format(Locale.getDefault(), "%.2f MB", fileSize / (1024.0 * 1024.0));
                        
                        mainHandler.post(() -> {
                            binding.llAttachment.setVisibility(View.VISIBLE);
                            binding.tvAttachmentNameDetail.setText(fileName);
                            binding.tvAttachmentSize.setText(sizeStr);
                            
                            if (fileType.startsWith("image/")) {
                                binding.ivAttachmentIcon.setImageResource(android.R.drawable.ic_menu_gallery);
                            } else if (fileType.equals("application/pdf")) {
                                binding.ivAttachmentIcon.setImageResource(android.R.drawable.ic_menu_agenda);
                            }
                            
                            binding.cvAttachment.setOnClickListener(v -> downloadAndOpenAttachment(attachId, fileName, fileType));
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { if(rs != null) rs.close(); } catch(Exception ignored){}
                try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                try { if(connection != null) connection.close(); } catch(Exception ignored){}
            }
        });
    }
    
    private void downloadAndOpenAttachment(int attachId, String fileName, String fileType) {
        Toast.makeText(this, "Mengunduh file...", Toast.LENGTH_SHORT).show();
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    stmt = connection.prepareStatement("SELECT file_data FROM attachments WHERE id = ?");
                    stmt.setInt(1, attachId);
                    rs = stmt.executeQuery();
                    if (rs.next()) {
                        byte[] fileData = rs.getBytes("file_data");
                        if (fileData != null) {
                            File cacheDir = getCacheDir();
                            File tempFile = new File(cacheDir, fileName);
                            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                                fos.write(fileData);
                            }
                            
                            Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempFile);
                            
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(fileUri, fileType);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            
                            mainHandler.post(() -> {
                                try {
                                    startActivity(intent);
                                } catch (Exception e) {
                                    Toast.makeText(this, "Tidak ada aplikasi untuk membuka file ini", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, "Gagal mengunduh file", Toast.LENGTH_SHORT).show());
            } finally {
                try { if(rs != null) rs.close(); } catch(Exception ignored){}
                try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                try { if(connection != null) connection.close(); } catch(Exception ignored){}
            }
        });
    }

    private void updateTicketStatus() {
        int selectedStatusIndex = binding.spinnerStatus.getSelectedItemPosition();
        String newStatus = statusValues[selectedStatusIndex];
        
        int selectedPriorityIndex = binding.spinnerPriority.getSelectedItemPosition();
        String newPriority = priorityValues[selectedPriorityIndex];
        
        binding.btnUpdateStatus.setEnabled(false);
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    String query = "UPDATE tickets SET status = ?, priority = ? WHERE id = ?";
                    stmt = connection.prepareStatement(query);
                    stmt.setString(1, newStatus);
                    stmt.setString(2, newPriority);
                    stmt.setInt(3, ticketId);
                    
                    int result = stmt.executeUpdate();
                    if (result > 0) {
                        try (PreparedStatement actStmt = connection.prepareStatement("INSERT INTO activities (ticket_id, user_id, action_type, description) VALUES (?, ?, ?, ?)")) {
                            actStmt.setInt(1, ticketId);
                            actStmt.setInt(2, session.getUserId());
                            actStmt.setString(3, "TICKET_UPDATED");
                            String actionText = "Status berubah menjadi " + newStatus.toUpperCase();
                            if (newStatus.equals("resolved")) actionText = "Admin Resolve Ticket";
                            if (newStatus.equals("rejected")) actionText = "Admin Reject Ticket";
                            actStmt.setString(4, actionText);
                            actStmt.executeUpdate();
                        } catch (Exception ignored) {}
                        
                        // Notify user if ticket is rejected
                        if (newStatus.equals("rejected") && ticketOwnerId != -1) {
                            try (PreparedStatement notifStmt = connection.prepareStatement(
                                "INSERT INTO notifications (user_id, title, message, ticket_id) VALUES (?, 'Tiket Ditolak', ?, ?)")) {
                                notifStmt.setInt(1, ticketOwnerId);
                                notifStmt.setString(2, "Tiket Anda (" + ticketCodeGlobal + ") telah ditolak oleh Admin.");
                                notifStmt.setInt(3, ticketId);
                                notifStmt.executeUpdate();
                            } catch (Exception ignored) {}
                        }
                        
                        mainHandler.post(() -> {
                            Toast.makeText(this, "Perubahan berhasil disimpan", Toast.LENGTH_SHORT).show();
                            binding.tvTicketStatus.setText(newStatus.toUpperCase());
                        });
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } finally {
                try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                try { if(connection != null) connection.close(); } catch(Exception ignored){}
                mainHandler.post(() -> binding.btnUpdateStatus.setEnabled(true));
            }
        });
    }

    private void sendReply() {
        String message = binding.etReplyMessage.getText().toString().trim();
        if (message.isEmpty() || ticketId == -1) return;

        binding.btnSendReply.setEnabled(false);
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    String query = "INSERT INTO ticket_replies (ticket_id, user_id, message) VALUES (?, ?, ?)";
                    stmt = connection.prepareStatement(query);
                    stmt.setInt(1, ticketId);
                    stmt.setInt(2, session.getUserId());
                    stmt.setString(3, message);
                    
                    int result = stmt.executeUpdate();
                    if (result > 0) {
                        try (PreparedStatement actStmt = connection.prepareStatement("INSERT INTO activities (ticket_id, user_id, action_type, description) VALUES (?, ?, ?, ?)")) {
                            actStmt.setInt(1, ticketId);
                            actStmt.setInt(2, session.getUserId());
                            actStmt.setString(3, "REPLY_ADDED");
                            String roleStr = session.getRole().equals("admin") ? "Admin" : "User";
                            actStmt.setString(4, roleStr + " membalas Ticket");
                            actStmt.executeUpdate();
                        } catch (Exception ignored) {}
                        
                        // Notify logic
                        if (session.getRole().equals("admin")) {
                            if (ticketOwnerId != -1) {
                                try (PreparedStatement notifStmt = connection.prepareStatement(
                                    "INSERT INTO notifications (user_id, title, message, ticket_id) VALUES (?, 'Balasan Baru', ?, ?)")) {
                                    notifStmt.setInt(1, ticketOwnerId);
                                    notifStmt.setString(2, "Admin membalas tiket Anda (" + ticketCodeGlobal + ").");
                                    notifStmt.setInt(3, ticketId);
                                    notifStmt.executeUpdate();
                                } catch (Exception ignored) {}
                            }
                        } else {
                            try (PreparedStatement notifStmt = connection.prepareStatement(
                                "INSERT INTO notifications (user_id, title, message, ticket_id) " +
                                "SELECT id, 'Balasan Baru', ?, ? FROM users WHERE role = 'admin'")) {
                                notifStmt.setString(1, "User membalas tiket " + ticketCodeGlobal + ".");
                                notifStmt.setInt(2, ticketId);
                                notifStmt.executeUpdate();
                            } catch (Exception ignored) {}
                        }
                        
                        mainHandler.post(() -> {
                            binding.etReplyMessage.setText("");
                            loadReplies(); // Refresh list
                        });
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } finally {
                try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                try { if(connection != null) connection.close(); } catch(Exception ignored){}
                mainHandler.post(() -> binding.btnSendReply.setEnabled(true));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

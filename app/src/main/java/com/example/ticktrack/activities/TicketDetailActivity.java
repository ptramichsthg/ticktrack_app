package com.example.ticktrack.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ticktrack.adapters.TicketReplyAdapter;
import com.example.ticktrack.databinding.ActivityTicketDetailBinding;
import com.example.ticktrack.db.DatabaseConnection;
import com.example.ticktrack.models.TicketReply;
import com.example.ticktrack.session.SessionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    private final String[] statusValues = {"open", "in_progress", "resolved"};
    private final String[] statusLabels = {"Open", "In Progress", "Resolved"};

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

            binding.btnUpdateStatus.setOnClickListener(v -> updateTicketStatus());
        }

        binding.btnSendReply.setOnClickListener(v -> sendReply());

        if (ticketId != -1) {
            loadTicketDetail();
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
                        String title = rs.getString("title");
                        String desc = rs.getString("description");
                        String author = rs.getString("user_name");
                        String status = rs.getString("status");
                        String category = rs.getString("category_name");
                        
                        String createdAt = "";
                        if (rs.getTimestamp("created_at") != null) {
                            createdAt = dateFormat.format(rs.getTimestamp("created_at"));
                        }
                        String finalCreatedAt = createdAt;
                        
                        // IDOR Protection
                        int ticketOwnerId = rs.getInt("user_id");
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
                            }
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

    private void updateTicketStatus() {
        int selectedIndex = binding.spinnerStatus.getSelectedItemPosition();
        String newStatus = statusValues[selectedIndex];
        
        binding.btnUpdateStatus.setEnabled(false);
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    String query = "UPDATE tickets SET status = ? WHERE id = ?";
                    stmt = connection.prepareStatement(query);
                    stmt.setString(1, newStatus);
                    stmt.setInt(2, ticketId);
                    
                    int result = stmt.executeUpdate();
                    if (result > 0) {
                        mainHandler.post(() -> {
                            Toast.makeText(this, "Status berhasil diperbarui", Toast.LENGTH_SHORT).show();
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
}

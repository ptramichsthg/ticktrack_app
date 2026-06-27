package com.example.ticktrack.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.ticktrack.R;
import com.example.ticktrack.adapters.NotificationAdapter;
import com.example.ticktrack.db.DatabaseConnection;
import com.example.ticktrack.models.NotificationModel;
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

public class NotificationFragment extends Fragment {
    private SessionManager session;
    private ExecutorService executorService;
    private Handler mainHandler;
    private RecyclerView rvNotifications;
    private SwipeRefreshLayout swipeRefresh;
    private List<NotificationModel> notificationList;
    private NotificationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);
        
        session = new SessionManager(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        rvNotifications = view.findViewById(R.id.rvNotifications);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        
        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        view.findViewById(R.id.tvMarkAllRead).setOnClickListener(v -> markAllAsRead());
        
        swipeRefresh.setOnRefreshListener(this::loadNotifications);
        
        loadNotifications();
        return view;
    }

    private void loadNotifications() {
        swipeRefresh.setRefreshing(true);
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            List<NotificationModel> list = new ArrayList<>();
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    String query = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
                    stmt = connection.prepareStatement(query);
                    stmt.setInt(1, session.getUserId());
                    rs = stmt.executeQuery();

                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

                    while (rs.next()) {
                        String createdAt = rs.getString("created_at");
                        try {
                            if (createdAt != null) createdAt = outputFormat.format(inputFormat.parse(createdAt));
                        } catch (Exception e) {}

                        int ticketId = 0;
                        try { ticketId = rs.getInt("ticket_id"); } catch (Exception ignored) {}
                        
                        list.add(new NotificationModel(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("title"),
                            rs.getString("message"),
                            rs.getBoolean("is_read"),
                            createdAt,
                            ticketId
                        ));
                    }
                    mainHandler.post(() -> {
                        notificationList = list;
                        adapter = new NotificationAdapter(notificationList, (notification, position) -> markAsRead(notification, position));
                        rvNotifications.setAdapter(adapter);
                        swipeRefresh.setRefreshing(false);
                        
                        View emptyState = getView() != null ? getView().findViewById(R.id.layoutEmptyState) : null;
                        if (list.isEmpty()) {
                            rvNotifications.setVisibility(View.GONE);
                            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                        } else {
                            rvNotifications.setVisibility(View.VISIBLE);
                            if (emptyState != null) emptyState.setVisibility(View.GONE);
                        }
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } finally {
                try { if(rs != null) rs.close(); } catch(Exception ignored){}
                try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                try { if(connection != null) connection.close(); } catch(Exception ignored){}
            }
        });
    }

    private void markAsRead(NotificationModel notification, int position) {
        if (!notification.isRead()) {
            executorService.execute(() -> {
                Connection connection = null;
                PreparedStatement stmt = null;
                try {
                    connection = DatabaseConnection.getConnection();
                    if (connection != null) {
                        stmt = connection.prepareStatement("UPDATE notifications SET is_read = 1 WHERE id = ?");
                        stmt.setInt(1, notification.getId());
                        if (stmt.executeUpdate() > 0) {
                            mainHandler.post(() -> {
                                notification.setRead(true);
                                adapter.notifyItemChanged(position);
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                    try { if(connection != null) connection.close(); } catch(Exception ignored){}
                }
            });
        }
        
        // Navigate to ticket detail if ticket_id is valid
        if (notification.getTicketId() > 0) {
            android.content.Intent intent = new android.content.Intent(requireContext(), com.example.ticktrack.activities.TicketDetailActivity.class);
            intent.putExtra("ticket_id", notification.getTicketId());
            startActivity(intent);
        }
    }
    
    private void markAllAsRead() {
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    stmt = connection.prepareStatement("UPDATE notifications SET is_read = 1 WHERE user_id = ?");
                    stmt.setInt(1, session.getUserId());
                    if (stmt.executeUpdate() > 0) {
                        mainHandler.post(this::loadNotifications);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                try { if(connection != null) connection.close(); } catch(Exception ignored){}
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

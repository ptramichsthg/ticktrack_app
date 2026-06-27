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
import com.example.ticktrack.databinding.FragmentDashboardBinding;
import com.example.ticktrack.db.DatabaseConnection;
import com.example.ticktrack.session.SessionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardFragment extends Fragment {
    private FragmentDashboardBinding binding;
    private SessionManager session;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        session = new SessionManager(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        setupHeader();
        loadStats();

        binding.swipeRefresh.setOnRefreshListener(this::loadStats);

        return binding.getRoot();
    }

    private void setupHeader() {
        binding.tvUserName.setText(session.getName() != null ? session.getName() : "Guest");
        binding.tvRole.setText(session.getRole().equals("admin") ? "Administrator" : "Pengguna");
    }

    private void loadStats() {
        binding.swipeRefresh.setRefreshing(true);
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    String query;
                    
                    if (session.getRole().equals("admin")) {
                        query = "SELECT COUNT(*) as total, " +
                                "SUM(CASE WHEN status = 'open' THEN 1 ELSE 0 END) as open_count, " +
                                "SUM(CASE WHEN status = 'in_progress' THEN 1 ELSE 0 END) as in_progress_count, " +
                                "SUM(CASE WHEN status = 'resolved' THEN 1 ELSE 0 END) as resolved_count " +
                                "FROM tickets";
                        stmt = connection.prepareStatement(query);
                    } else {
                        query = "SELECT COUNT(*) as total, " +
                                "SUM(CASE WHEN status = 'open' THEN 1 ELSE 0 END) as open_count, " +
                                "SUM(CASE WHEN status = 'in_progress' THEN 1 ELSE 0 END) as in_progress_count, " +
                                "SUM(CASE WHEN status = 'resolved' THEN 1 ELSE 0 END) as resolved_count " +
                                "FROM tickets WHERE user_id = ?";
                        stmt = connection.prepareStatement(query);
                        stmt.setInt(1, session.getUserId());
                    }

                    rs = stmt.executeQuery();
                    if (rs.next()) {
                        int total = rs.getInt("total");
                        int open = rs.getInt("open_count");
                        int inProgress = rs.getInt("in_progress_count");
                        int resolved = rs.getInt("resolved_count");

                        mainHandler.post(() -> {
                            if (binding != null) {
                                binding.tvStatTotal.setText(String.valueOf(total));
                                binding.tvStatOpen.setText(String.valueOf(open));
                                binding.tvStatProgress.setText(String.valueOf(inProgress));
                                binding.tvStatResolved.setText(String.valueOf(resolved));
                                binding.swipeRefresh.setRefreshing(false);
                            }
                        });
                    }
                } else {
                    mainHandler.post(() -> {
                        if (binding != null) binding.swipeRefresh.setRefreshing(false);
                        Toast.makeText(getContext(), "Koneksi database gagal", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (binding != null) binding.swipeRefresh.setRefreshing(false);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } finally {
                try { if(rs != null) { rs.close(); } } catch(Exception ignored){}
                try { if(stmt != null) { stmt.close(); } } catch(Exception ignored){}
                try { if(connection != null) { connection.close(); } } catch(Exception ignored){}
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null) {
            executorService.shutdown();
        }
        binding = null;
    }
}

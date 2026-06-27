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
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ticktrack.adapters.ActivityHistoryAdapter;
import com.example.ticktrack.models.ActivityModel;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import android.graphics.Color;

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

        binding.rvRecentActivity.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentActivity.setNestedScrollingEnabled(false);

        setupHeader();
        loadStats();
        loadRecentActivity();

        binding.swipeRefresh.setOnRefreshListener(() -> {
            loadStats();
            loadRecentActivity();
        });
        
        binding.btnShortcutTickets.setOnClickListener(v -> {
            if (getActivity() != null) {
                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = getActivity().findViewById(com.example.ticktrack.R.id.bottomNav);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(com.example.ticktrack.R.id.nav_tickets);
                }
            }
        });

        return binding.getRoot();
    }

    private void setupHeader() {
        binding.tvUserName.setText(session.getName() != null ? session.getName() : "Guest");
        binding.tvRole.setText(session.getRole().equals("admin") ? "Administrator" : "Pengguna");
        
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    stmt = connection.prepareStatement("SELECT avatar FROM users WHERE id = ?");
                    stmt.setInt(1, session.getUserId());
                    rs = stmt.executeQuery();
                    if (rs.next()) {
                        byte[] bytes = rs.getBytes("avatar");
                        if (bytes != null && bytes.length > 0) {
                            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            mainHandler.post(() -> {
                                binding.ivDashboardAvatar.setImageBitmap(bitmap);
                            });
                        }
                    }
                }
            } catch (Exception ignored) {
            } finally {
                try { if(rs != null) rs.close(); } catch(Exception ignored){}
                try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                try { if(connection != null) connection.close(); } catch(Exception ignored){}
            }
        });
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
                                "SUM(CASE WHEN status = 'resolved' THEN 1 ELSE 0 END) as resolved_count, " +
                                "SUM(CASE WHEN status = 'rejected' THEN 1 ELSE 0 END) as rejected_count " +
                                "FROM tickets";
                        stmt = connection.prepareStatement(query);
                    } else {
                        query = "SELECT COUNT(*) as total, " +
                                "SUM(CASE WHEN status = 'open' THEN 1 ELSE 0 END) as open_count, " +
                                "SUM(CASE WHEN status = 'in_progress' THEN 1 ELSE 0 END) as in_progress_count, " +
                                "SUM(CASE WHEN status = 'resolved' THEN 1 ELSE 0 END) as resolved_count, " +
                                "SUM(CASE WHEN status = 'rejected' THEN 1 ELSE 0 END) as rejected_count " +
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
                        int rejected = rs.getInt("rejected_count");

                        mainHandler.post(() -> {
                            if (binding != null) {
                                binding.tvStatTotal.setText(String.valueOf(total));
                                binding.tvStatOpen.setText(String.valueOf(open));
                                binding.tvStatProgress.setText(String.valueOf(inProgress));
                                binding.tvStatResolved.setText(String.valueOf(resolved));
                                binding.tvStatRejected.setText(String.valueOf(rejected));
                                if (session.getRole().equals("admin")) {
                                    binding.cvPieChart.setVisibility(View.VISIBLE);
                                    setupPieChart(open, inProgress, resolved, rejected);
                                } else {
                                    binding.cvPieChart.setVisibility(View.GONE);
                                }
                                
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

    private void setupPieChart(int open, int inProgress, int resolved, int rejected) {
        if (binding.pieChart == null) return;
        
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        
        if (open > 0) {
            entries.add(new PieEntry(open, "Buka"));
            colors.add(Color.parseColor("#1D4ED8"));
        }
        if (inProgress > 0) {
            entries.add(new PieEntry(inProgress, "Diproses"));
            colors.add(Color.parseColor("#B45309"));
        }
        if (resolved > 0) {
            entries.add(new PieEntry(resolved, "Selesai"));
            colors.add(Color.parseColor("#047857"));
        }
        if (rejected > 0) {
            entries.add(new PieEntry(rejected, "Ditolak"));
            colors.add(Color.parseColor("#EF4444"));
        }

        if (entries.isEmpty()) {
            binding.pieChart.clear();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);

        PieData data = new PieData(dataSet);
        binding.pieChart.setData(data);
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.getLegend().setEnabled(false);
        binding.pieChart.setDrawHoleEnabled(true);
        binding.pieChart.setHoleColor(Color.TRANSPARENT);
        binding.pieChart.setTransparentCircleRadius(0f);
        binding.pieChart.setCenterText("Status");
        binding.pieChart.setCenterTextSize(16f);
        binding.pieChart.animateY(1000);
        binding.pieChart.invalidate();
    }

    private void loadRecentActivity() {
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            List<ActivityModel> activities = new ArrayList<>();
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    String query;
                    if (session.getRole().equals("admin")) {
                        query = "SELECT * FROM activities ORDER BY created_at DESC LIMIT 5";
                        stmt = connection.prepareStatement(query);
                    } else {
                        query = "SELECT * FROM activities WHERE user_id = ? ORDER BY created_at DESC LIMIT 5";
                        stmt = connection.prepareStatement(query);
                        stmt.setInt(1, session.getUserId());
                    }
                    
                    rs = stmt.executeQuery();
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                    
                    while (rs.next()) {
                        String createdAt = rs.getString("created_at");
                        try {
                            if (createdAt != null) {
                                createdAt = outputFormat.format(inputFormat.parse(createdAt));
                            }
                        } catch (Exception e) {}
                        
                        activities.add(new ActivityModel(
                            rs.getInt("id"),
                            rs.getString("action_type"),
                            rs.getString("description"),
                            createdAt
                        ));
                    }
                    
                    mainHandler.post(() -> {
                        if (binding != null) {
                            ActivityHistoryAdapter adapter = new ActivityHistoryAdapter(activities);
                            binding.rvRecentActivity.setAdapter(adapter);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
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

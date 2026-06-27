package com.example.ticktrack.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ticktrack.databinding.ActivityAnalyticsBinding;
import com.example.ticktrack.db.DatabaseConnection;
import com.example.ticktrack.session.SessionManager;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalyticsActivity extends AppCompatActivity {
    private ActivityAnalyticsBinding binding;
    private SessionManager session;
    private ExecutorService executorService;
    private Handler mainHandler;

    private final int[] colors = {
        Color.parseColor("#3B82F6"), // Blue
        Color.parseColor("#10B981"), // Green
        Color.parseColor("#F59E0B"), // Yellow
        Color.parseColor("#EF4444"), // Red
        Color.parseColor("#8B5CF6")  // Purple
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAnalyticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        if (!"admin".equals(session.getRole())) {
            finish();
            return;
        }

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        loadAnalyticsData();
    }

    private void loadAnalyticsData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            Connection connection = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    loadStatusChart(connection);
                    loadCategoryChart(connection);
                    loadPriorityChart(connection);
                    loadDailyChart(connection);
                    loadMonthlyChart(connection);
                    loadTopUsersChart(connection);
                }
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } finally {
                try { if(connection != null) connection.close(); } catch(Exception ignored){}
                mainHandler.post(() -> binding.progressBar.setVisibility(View.GONE));
            }
        });
    }

    private void loadStatusChart(Connection connection) throws Exception {
        PreparedStatement stmt = connection.prepareStatement("SELECT status, COUNT(*) as count FROM tickets GROUP BY status");
        ResultSet rs = stmt.executeQuery();
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> entryColors = new ArrayList<>();
        
        while (rs.next()) {
            String status = rs.getString("status");
            int count = rs.getInt("count");
            if (count > 0) {
                entries.add(new PieEntry(count, status.toUpperCase()));
                if (status.equals("open")) entryColors.add(Color.parseColor("#3B82F6"));
                else if (status.equals("in_progress")) entryColors.add(Color.parseColor("#F59E0B"));
                else if (status.equals("resolved")) entryColors.add(Color.parseColor("#10B981"));
                else if (status.equals("rejected")) entryColors.add(Color.parseColor("#EF4444"));
                else entryColors.add(Color.parseColor("#94A3B8"));
            }
        }
        
        mainHandler.post(() -> {
            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(entryColors);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.WHITE);
            binding.chartStatus.setData(new PieData(dataSet));
            binding.chartStatus.getDescription().setEnabled(false);
            binding.chartStatus.setCenterText("Total\nStatus");
            binding.chartStatus.animateY(1000);
            binding.chartStatus.invalidate();
        });
        rs.close(); stmt.close();
    }

    private void loadCategoryChart(Connection connection) throws Exception {
        PreparedStatement stmt = connection.prepareStatement("SELECT c.name, COUNT(t.id) as count, c.color FROM tickets t JOIN categories c ON t.category_id = c.id GROUP BY c.name, c.color");
        ResultSet rs = stmt.executeQuery();
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> entryColors = new ArrayList<>();
        
        while (rs.next()) {
            int count = rs.getInt("count");
            if (count > 0) {
                entries.add(new PieEntry(count, rs.getString("name")));
                try {
                    entryColors.add(Color.parseColor(rs.getString("color")));
                } catch (Exception e) {
                    entryColors.add(colors[entries.size() % colors.length]);
                }
            }
        }
        
        mainHandler.post(() -> {
            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(entryColors);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.WHITE);
            binding.chartCategory.setData(new PieData(dataSet));
            binding.chartCategory.getDescription().setEnabled(false);
            binding.chartCategory.setCenterText("Kategori");
            binding.chartCategory.animateY(1000);
            binding.chartCategory.invalidate();
        });
        rs.close(); stmt.close();
    }

    private void loadPriorityChart(Connection connection) throws Exception {
        PreparedStatement stmt = connection.prepareStatement("SELECT priority, COUNT(*) as count FROM tickets GROUP BY priority");
        ResultSet rs = stmt.executeQuery();
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> entryColors = new ArrayList<>();
        
        while (rs.next()) {
            String priority = rs.getString("priority");
            int count = rs.getInt("count");
            if (count > 0) {
                entries.add(new PieEntry(count, priority.toUpperCase()));
                if (priority.equals("low")) entryColors.add(Color.parseColor("#3B82F6"));
                else if (priority.equals("medium")) entryColors.add(Color.parseColor("#10B981"));
                else if (priority.equals("high")) entryColors.add(Color.parseColor("#F59E0B"));
                else if (priority.equals("critical")) entryColors.add(Color.parseColor("#EF4444"));
                else entryColors.add(Color.parseColor("#94A3B8"));
            }
        }
        
        mainHandler.post(() -> {
            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(entryColors);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.WHITE);
            binding.chartPriority.setData(new PieData(dataSet));
            binding.chartPriority.getDescription().setEnabled(false);
            binding.chartPriority.setCenterText("Prioritas");
            binding.chartPriority.animateY(1000);
            binding.chartPriority.invalidate();
        });
        rs.close(); stmt.close();
    }

    private void loadDailyChart(Connection connection) throws Exception {
        ArrayList<String> allDates = new ArrayList<>();
        ArrayList<String> displayLabels = new ArrayList<>();
        java.text.SimpleDateFormat sdfDB = new java.text.SimpleDateFormat("yyyy-MM-dd");
        java.text.SimpleDateFormat sdfDisplay = new java.text.SimpleDateFormat("dd/MM");
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, -6);
        for(int i = 0; i < 7; i++) {
            allDates.add(sdfDB.format(cal.getTime()));
            displayLabels.add(sdfDisplay.format(cal.getTime()));
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }

        String query = "SELECT DATE_FORMAT(created_at, '%Y-%m-%d') as date, COUNT(*) as count " +
                       "FROM tickets " +
                       "WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
                       "GROUP BY date ORDER BY date ASC";
        PreparedStatement stmt = connection.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        while (rs.next()) {
            counts.put(rs.getString("date"), rs.getInt("count"));
        }
        
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            int count = counts.containsKey(allDates.get(i)) ? counts.get(allDates.get(i)) : 0;
            entries.add(new BarEntry(i, count));
        }
        
        mainHandler.post(() -> {
            BarDataSet dataSet = new BarDataSet(entries, "Total Tiket");
            dataSet.setColor(Color.parseColor("#3B82F6"));
            dataSet.setValueTextSize(10f);
            
            BarData data = new BarData(dataSet);
            data.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    if (value == 0) return "";
                    return String.valueOf((int) value);
                }
            });
            
            binding.chartDaily.setData(data);
            binding.chartDaily.getDescription().setEnabled(false);
            binding.chartDaily.getAxisLeft().setAxisMinimum(0f);
            
            XAxis xAxis = binding.chartDaily.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(displayLabels));
            xAxis.setGranularity(1f);
            
            binding.chartDaily.getAxisRight().setEnabled(false);
            binding.chartDaily.animateY(1000);
            binding.chartDaily.invalidate();
        });
        rs.close(); stmt.close();
    }

    private void loadMonthlyChart(Connection connection) throws Exception {
        String query = "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COUNT(*) as count " +
                       "FROM tickets " +
                       "WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 5 MONTH) " +
                       "GROUP BY month ORDER BY month ASC";
        PreparedStatement stmt = connection.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int i = 0;
        
        while (rs.next()) {
            String month = rs.getString("month");
            int count = rs.getInt("count");
            entries.add(new BarEntry(i, count));
            labels.add(month);
            i++;
        }
        
        mainHandler.post(() -> {
            BarDataSet dataSet = new BarDataSet(entries, "Total Tiket");
            dataSet.setColor(Color.parseColor("#10B981"));
            dataSet.setValueTextSize(10f);
            
            BarData data = new BarData(dataSet);
            data.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.valueOf((int) value);
                }
            });
            
            binding.chartMonthly.setData(data);
            binding.chartMonthly.getDescription().setEnabled(false);
            
            XAxis xAxis = binding.chartMonthly.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            xAxis.setGranularity(1f);
            
            binding.chartMonthly.getAxisRight().setEnabled(false);
            binding.chartMonthly.animateY(1000);
            binding.chartMonthly.invalidate();
        });
        rs.close(); stmt.close();
    }

    private void loadTopUsersChart(Connection connection) throws Exception {
        String query = "SELECT u.name, COUNT(t.id) as count " +
                       "FROM tickets t JOIN users u ON t.user_id = u.id " +
                       "GROUP BY u.name ORDER BY count DESC LIMIT 5";
        PreparedStatement stmt = connection.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int i = 0;
        
        // Reverse order so the highest is at the top in HorizontalBarChart
        ArrayList<String> tempNames = new ArrayList<>();
        ArrayList<Integer> tempCounts = new ArrayList<>();
        
        while (rs.next()) {
            tempNames.add(rs.getString("name"));
            tempCounts.add(rs.getInt("count"));
        }
        
        for (int j = tempNames.size() - 1; j >= 0; j--) {
            entries.add(new BarEntry(i, tempCounts.get(j)));
            labels.add(tempNames.get(j));
            i++;
        }
        
        mainHandler.post(() -> {
            BarDataSet dataSet = new BarDataSet(entries, "Total Tiket per User");
            dataSet.setColors(colors);
            dataSet.setValueTextSize(10f);
            
            BarData data = new BarData(dataSet);
            data.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.valueOf((int) value);
                }
            });
            
            binding.chartTopUsers.setData(data);
            binding.chartTopUsers.getDescription().setEnabled(false);
            binding.chartTopUsers.setExtraLeftOffset(50f);
            
            XAxis xAxis = binding.chartTopUsers.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            xAxis.setGranularity(1f);
            xAxis.setDrawGridLines(false);
            
            binding.chartTopUsers.getAxisRight().setEnabled(false);
            binding.chartTopUsers.animateY(1000);
            binding.chartTopUsers.invalidate();
        });
        rs.close(); stmt.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

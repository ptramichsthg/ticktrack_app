package com.example.ticktrack.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ticktrack.activities.CreateTicketActivity;
import com.example.ticktrack.activities.TicketDetailActivity;
import com.example.ticktrack.adapters.TicketAdapter;
import com.example.ticktrack.databinding.FragmentTicketListBinding;
import com.example.ticktrack.db.DatabaseConnection;
import com.example.ticktrack.listeners.OnTicketClickListener;
import com.example.ticktrack.models.Ticket;
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
import androidx.appcompat.widget.SearchView;

public class TicketListFragment extends Fragment implements OnTicketClickListener {
    private FragmentTicketListBinding binding;
    private SessionManager session;
    private TicketAdapter adapter;
    private ExecutorService executorService;
    private Handler mainHandler;
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTicketListBinding.inflate(inflater, container, false);
        session = new SessionManager(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        adapter = new TicketAdapter(this);
        binding.rvTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTickets.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(this::loadTickets);
        binding.fabCreateTicket.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CreateTicketActivity.class));
        });

        if (session.getRole().equals("admin")) {
            binding.fabCreateTicket.setVisibility(View.GONE);
        }

        binding.llFilterContainer.setVisibility(View.VISIBLE);
        String[] filterLabels = {"Semua Status", "Open", "In Progress", "Resolved"};
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, filterLabels);
        binding.spinnerFilter.setAdapter(filterAdapter);

        binding.spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadTickets();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchQuery = query;
                loadTickets();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchQuery = newText;
                loadTickets();
                return true;
            }
        });

        // Set click listener for empty state button
        View emptyStateView = binding.getRoot().findViewById(com.example.ticktrack.R.id.layoutEmptyState);
        if (emptyStateView != null) {
            View btnCreate = emptyStateView.findViewById(com.example.ticktrack.R.id.btnEmptyStateCreate);
            if (btnCreate != null) {
                btnCreate.setOnClickListener(v -> startActivity(new Intent(requireContext(), CreateTicketActivity.class)));
            }
        }

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTickets();
    }

    private void loadTickets() {
        binding.swipeRefresh.setRefreshing(true);
        
        int filterPos = binding.spinnerFilter != null ? binding.spinnerFilter.getSelectedItemPosition() : 0;
        String[] filterValues = {null, "open", "in_progress", "resolved"};
        String statusFilter = filterPos > 0 ? filterValues[filterPos] : null;

        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    StringBuilder queryBuilder = new StringBuilder(
                            "SELECT t.*, u.name as user_name, c.name as category_name " +
                            "FROM tickets t " +
                            "JOIN users u ON t.user_id = u.id " +
                            "JOIN categories c ON t.category_id = c.id "
                    );

                    boolean isAdmin = session.getRole().equals("admin");
                    boolean hasWhere = false;

                    if (!isAdmin) {
                        queryBuilder.append("WHERE t.user_id = ? ");
                        hasWhere = true;
                    }

                    if (statusFilter != null) {
                        if (!hasWhere) { queryBuilder.append("WHERE "); hasWhere = true; }
                        else { queryBuilder.append("AND "); }
                        queryBuilder.append("t.status = ? ");
                    }

                    if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                        if (!hasWhere) { queryBuilder.append("WHERE "); hasWhere = true; }
                        else { queryBuilder.append("AND "); }
                        queryBuilder.append("(t.code LIKE ? OR t.title LIKE ?) ");
                    }

                    queryBuilder.append("ORDER BY t.created_at DESC");

                    stmt = connection.prepareStatement(queryBuilder.toString());
                    int paramIndex = 1;

                    if (!isAdmin) {
                        stmt.setInt(paramIndex++, session.getUserId());
                    }
                    if (statusFilter != null) {
                        stmt.setString(paramIndex++, statusFilter);
                    }
                    if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                        String likeQuery = "%" + searchQuery.trim() + "%";
                        stmt.setString(paramIndex++, likeQuery);
                        stmt.setString(paramIndex++, likeQuery);
                    }

                    rs = stmt.executeQuery();
                    List<Ticket> tickets = new ArrayList<>();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                    while (rs.next()) {
                        Ticket t = new Ticket();
                        t.setId(rs.getInt("id"));
                        t.setCode(rs.getString("code"));
                        t.setTitle(rs.getString("title"));
                        t.setDescription(rs.getString("description"));
                        t.setStatus(rs.getString("status"));
                        t.setPriority(rs.getString("priority"));
                        t.setUserName(rs.getString("user_name"));
                        t.setCategoryName(rs.getString("category_name"));
                        if (rs.getTimestamp("created_at") != null) {
                            t.setCreatedAt(dateFormat.format(rs.getTimestamp("created_at")));
                        }
                        tickets.add(t);
                    }

                    mainHandler.post(() -> {
                        if (binding != null) {
                            adapter.setTickets(tickets);
                            binding.swipeRefresh.setRefreshing(false);
                            
                            View emptyStateView = binding.getRoot().findViewById(com.example.ticktrack.R.id.layoutEmptyState);
                            if (tickets.isEmpty()) {
                                binding.rvTickets.setVisibility(View.GONE);
                                if (emptyStateView != null) emptyStateView.setVisibility(View.VISIBLE);
                            } else {
                                binding.rvTickets.setVisibility(View.VISIBLE);
                                if (emptyStateView != null) emptyStateView.setVisibility(View.GONE);
                            }
                        }
                    });
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
    public void onTicketClick(Ticket ticket) {
        Intent intent = new Intent(requireContext(), TicketDetailActivity.class);
        intent.putExtra("ticket_id", ticket.getId());
        intent.putExtra("ticket_code", ticket.getCode());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

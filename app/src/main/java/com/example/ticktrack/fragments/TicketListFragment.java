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
    private List<Integer> categoryIds = new ArrayList<>();
    private boolean isSpinnersInitialized = false;
    
    // Pagination & Shimmer
    private int offset = 0;
    private static final int LIMIT = 10;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerContainer;

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
        
        shimmerContainer = binding.getRoot().findViewById(com.example.ticktrack.R.id.shimmerViewContainer);

        binding.swipeRefresh.setOnRefreshListener(() -> loadTickets(false));
        
        binding.rvTickets.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) { // Scrolling down
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null && !isLoading && !isLastPage) {
                        if (layoutManager.findLastCompletelyVisibleItemPosition() == adapter.getItemCount() - 1) {
                            loadTickets(true);
                        }
                    }
                }
            }
        });
        binding.fabCreateTicket.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CreateTicketActivity.class));
        });

        if (session.getRole().equals("admin")) {
            binding.fabCreateTicket.setVisibility(View.GONE);
        }

        setupFilters();

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchQuery = query;
                loadTickets(false);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchQuery = newText;
                loadTickets(false);
                return true;
            }
        });

        View emptyStateView = binding.getRoot().findViewById(com.example.ticktrack.R.id.layoutEmptyState);
        if (emptyStateView != null) {
            View btnCreate = emptyStateView.findViewById(com.example.ticktrack.R.id.btnEmptyStateCreate);
            if (btnCreate != null) {
                btnCreate.setOnClickListener(v -> startActivity(new Intent(requireContext(), CreateTicketActivity.class)));
            }
        }

        return binding.getRoot();
    }

    private void setupFilters() {
        String[] statusLabels = {"Semua Status", "Open", "In Progress", "Resolved", "Rejected"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, statusLabels);
        binding.spinnerStatus.setAdapter(statusAdapter);

        String[] priorityLabels = {"Semua Prioritas", "Low", "Medium", "High"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorityLabels);
        binding.spinnerPriority.setAdapter(priorityAdapter);

        String[] sortLabels = {"Terbaru", "Terlama", "Prioritas (High-Low)"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, sortLabels);
        binding.spinnerSort.setAdapter(sortAdapter);

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isSpinnersInitialized) loadTickets(false);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        binding.spinnerStatus.setOnItemSelectedListener(filterListener);
        binding.spinnerPriority.setOnItemSelectedListener(filterListener);
        binding.spinnerSort.setOnItemSelectedListener(filterListener);
        binding.spinnerCategory.setOnItemSelectedListener(filterListener);

        loadCategoriesForFilter();
    }

    private void loadCategoriesForFilter() {
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            List<String> categories = new ArrayList<>();
            categories.add("Semua Kategori");
            categoryIds.clear();
            categoryIds.add(-1);

            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    stmt = connection.prepareStatement("SELECT * FROM categories ORDER BY name ASC");
                    rs = stmt.executeQuery();
                    while (rs.next()) {
                        categoryIds.add(rs.getInt("id"));
                        categories.add(rs.getString("name"));
                    }
                    mainHandler.post(() -> {
                        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories);
                        binding.spinnerCategory.setAdapter(catAdapter);
                        isSpinnersInitialized = true;
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

    @Override
    public void onResume() {
        super.onResume();
        loadTickets(false);
    }

    private void loadTickets(boolean isLoadMore) {
        if (isLoading) return;
        isLoading = true;
        
        if (!isLoadMore) {
            offset = 0;
            isLastPage = false;
            if (shimmerContainer != null && adapter.getItemCount() == 0) {
                shimmerContainer.setVisibility(View.VISIBLE);
                shimmerContainer.startShimmer();
                binding.rvTickets.setVisibility(View.GONE);
                View emptyState = binding.getRoot().findViewById(com.example.ticktrack.R.id.layoutEmptyState);
                if (emptyState != null) emptyState.setVisibility(View.GONE);
            } else {
                binding.swipeRefresh.setRefreshing(true);
            }
        } else {
            binding.swipeRefresh.setRefreshing(true);
        }
        
        int statusPos = binding.spinnerStatus != null ? binding.spinnerStatus.getSelectedItemPosition() : 0;
        String[] statusValues = {null, "open", "in_progress", "resolved", "rejected"};
        String statusFilter = (statusPos > 0 && statusPos < statusValues.length) ? statusValues[statusPos] : null;

        int priorityPos = binding.spinnerPriority != null ? binding.spinnerPriority.getSelectedItemPosition() : 0;
        String[] priorityValues = {null, "Low", "Medium", "High"};
        String priorityFilter = (priorityPos > 0 && priorityPos < priorityValues.length) ? priorityValues[priorityPos] : null;

        int categoryPos = binding.spinnerCategory != null ? binding.spinnerCategory.getSelectedItemPosition() : 0;
        Integer categoryFilter = (categoryPos > 0 && categoryPos < categoryIds.size()) ? categoryIds.get(categoryPos) : null;

        int sortPos = binding.spinnerSort != null ? binding.spinnerSort.getSelectedItemPosition() : 0;

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
                    
                    if (priorityFilter != null) {
                        if (!hasWhere) { queryBuilder.append("WHERE "); hasWhere = true; }
                        else { queryBuilder.append("AND "); }
                        queryBuilder.append("t.priority = ? ");
                    }
                    
                    if (categoryFilter != null) {
                        if (!hasWhere) { queryBuilder.append("WHERE "); hasWhere = true; }
                        else { queryBuilder.append("AND "); }
                        queryBuilder.append("t.category_id = ? ");
                    }

                    if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                        if (!hasWhere) { queryBuilder.append("WHERE "); hasWhere = true; }
                        else { queryBuilder.append("AND "); }
                        queryBuilder.append("(t.code LIKE ? OR t.title LIKE ?) ");
                    }

                    if (sortPos == 1) {
                        queryBuilder.append("ORDER BY t.created_at ASC ");
                    } else if (sortPos == 2) {
                        queryBuilder.append("ORDER BY CASE t.priority WHEN 'High' THEN 1 WHEN 'Medium' THEN 2 WHEN 'Low' THEN 3 ELSE 4 END ASC, t.created_at DESC ");
                    } else {
                        queryBuilder.append("ORDER BY t.created_at DESC ");
                    }
                    
                    queryBuilder.append("LIMIT ? OFFSET ?");

                    stmt = connection.prepareStatement(queryBuilder.toString());
                    int paramIndex = 1;

                    if (!isAdmin) {
                        stmt.setInt(paramIndex++, session.getUserId());
                    }
                    if (statusFilter != null) {
                        stmt.setString(paramIndex++, statusFilter);
                    }
                    if (priorityFilter != null) {
                        stmt.setString(paramIndex++, priorityFilter);
                    }
                    if (categoryFilter != null) {
                        stmt.setInt(paramIndex++, categoryFilter);
                    }
                    if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                        String likeQuery = "%" + searchQuery.trim() + "%";
                        stmt.setString(paramIndex++, likeQuery);
                        stmt.setString(paramIndex++, likeQuery);
                    }
                    
                    stmt.setInt(paramIndex++, LIMIT);
                    stmt.setInt(paramIndex++, offset);

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
                            if (shimmerContainer != null) {
                                shimmerContainer.stopShimmer();
                                shimmerContainer.setVisibility(View.GONE);
                            }
                            
                            if (tickets.size() < LIMIT) {
                                isLastPage = true;
                            } else {
                                offset += LIMIT;
                            }
                            
                            if (!isLoadMore) {
                                adapter.setTickets(tickets);
                            } else {
                                adapter.addTickets(tickets);
                            }
                            
                            binding.swipeRefresh.setRefreshing(false);
                            isLoading = false;
                            
                            View emptyStateView = binding.getRoot().findViewById(com.example.ticktrack.R.id.layoutEmptyState);
                            if (adapter.getItemCount() == 0) {
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
                        if (binding != null) {
                            binding.swipeRefresh.setRefreshing(false);
                            isLoading = false;
                            if (shimmerContainer != null) { shimmerContainer.stopShimmer(); shimmerContainer.setVisibility(View.GONE); }
                            
                            com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), "Koneksi database gagal (Offline)", com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE)
                                .setAction("Coba Lagi", v -> loadTickets(false)).show();
                        }
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (binding != null) {
                        binding.swipeRefresh.setRefreshing(false);
                        isLoading = false;
                        if (shimmerContainer != null) { shimmerContainer.stopShimmer(); shimmerContainer.setVisibility(View.GONE); }
                        
                        com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), "Error Database (Offline/Timeout)", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                            .setAction("Retry", v -> loadTickets(false)).show();
                    }
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
        if (executorService != null) {
            executorService.shutdown();
        }
        binding = null;
    }
}

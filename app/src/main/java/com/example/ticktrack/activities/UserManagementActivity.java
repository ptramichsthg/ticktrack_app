package com.example.ticktrack.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ticktrack.adapters.UserAdapter;
import com.example.ticktrack.databinding.ActivityUserManagementBinding;
import com.example.ticktrack.db.DatabaseConnection;
import com.example.ticktrack.models.UserModel;
import com.example.ticktrack.session.SessionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserManagementActivity extends AppCompatActivity {
    private ActivityUserManagementBinding binding;
    private SessionManager session;
    private ExecutorService executorService;
    private Handler mainHandler;
    private UserAdapter userAdapter;
    private List<UserModel> allUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        if (!"admin".equals(session.getRole())) {
            Toast.makeText(this, "Akses ditolak", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        userAdapter = new UserAdapter(user -> {
            Intent intent = new Intent(this, UserDetailActivity.class);
            intent.putExtra("user_id", user.getId());
            startActivity(intent);
        });
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUsers.setAdapter(userAdapter);

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterUsers(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                filterUsers(newText);
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
    }

    private void loadUsers() {
        binding.progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            List<UserModel> loadedUsers = new ArrayList<>();
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    String query = "SELECT u.*, " +
                            "(SELECT COUNT(*) FROM tickets t WHERE t.user_id = u.id) as total_tickets " +
                            "FROM users u WHERE u.role != 'admin' ORDER BY u.id DESC";
                    stmt = connection.prepareStatement(query);
                    rs = stmt.executeQuery();

                    while (rs.next()) {
                        boolean isActive = true;
                        try { isActive = rs.getBoolean("is_active"); } catch (Exception ignored) {}
                        loadedUsers.add(new UserModel(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("email"),
                                rs.getString("role"),
                                isActive,
                                rs.getInt("total_tickets")
                        ));
                    }
                    
                    mainHandler.post(() -> {
                        allUsers = loadedUsers;
                        userAdapter.setUsers(allUsers);
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

    private void filterUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            userAdapter.setUsers(allUsers);
            return;
        }
        String lowerQuery = query.toLowerCase();
        List<UserModel> filtered = new ArrayList<>();
        for (UserModel user : allUsers) {
            if (user.getName().toLowerCase().contains(lowerQuery) || 
                user.getEmail().toLowerCase().contains(lowerQuery)) {
                filtered.add(user);
            }
        }
        userAdapter.setUsers(filtered);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

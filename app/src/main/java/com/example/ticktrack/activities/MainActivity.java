package com.example.ticktrack.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.ticktrack.R;
import com.example.ticktrack.databinding.ActivityMainBinding;
import com.example.ticktrack.fragments.DashboardFragment;
import com.example.ticktrack.fragments.TicketListFragment;
import com.example.ticktrack.fragments.ProfileFragment;
import com.example.ticktrack.session.SessionManager;
import com.example.ticktrack.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import android.os.Handler;
import android.os.Looper;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private SessionManager session;
    private ScheduledExecutorService poller;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        mainHandler = new Handler(Looper.getMainLooper());

        if (session.isDarkMode()) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Default fragment
        loadFragment(new DashboardFragment());
        
        startNotificationPoller();

        binding.bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                fragment = new DashboardFragment();
            } else if (itemId == R.id.nav_tickets) {
                fragment = new TicketListFragment();
            } else if (itemId == R.id.nav_notifications) {
                fragment = new com.example.ticktrack.fragments.NotificationFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void startNotificationPoller() {
        poller = Executors.newSingleThreadScheduledExecutor();
        poller.scheduleAtFixedRate(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                connection = DatabaseConnection.getConnection();
                if (connection != null) {
                    String query = "SELECT COUNT(*) as unread_count FROM notifications WHERE user_id = ? AND is_read = 0";
                    stmt = connection.prepareStatement(query);
                    stmt.setInt(1, session.getUserId());
                    rs = stmt.executeQuery();
                    if (rs.next()) {
                        int unreadCount = rs.getInt("unread_count");
                        mainHandler.post(() -> {
                            if (unreadCount > 0) {
                                binding.bottomNav.getOrCreateBadge(R.id.nav_notifications).setVisible(true);
                                binding.bottomNav.getOrCreateBadge(R.id.nav_notifications).setNumber(unreadCount);
                            } else {
                                binding.bottomNav.removeBadge(R.id.nav_notifications);
                            }
                        });
                    }
                }
            } catch (Exception ignored) {
            } finally {
                try { if(rs != null) rs.close(); } catch(Exception ignored){}
                try { if(stmt != null) stmt.close(); } catch(Exception ignored){}
                try { if(connection != null) connection.close(); } catch(Exception ignored){}
            }
        }, 0, 10, TimeUnit.SECONDS); // Poll every 10 seconds
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (poller != null) {
            poller.shutdown();
        }
    }
}

package com.example.ticktrack.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.ticktrack.activities.LoginActivity;
import com.example.ticktrack.activities.EditProfileActivity;
import com.example.ticktrack.activities.ChangePasswordActivity;
import com.example.ticktrack.databinding.FragmentProfileBinding;
import com.example.ticktrack.session.SessionManager;
import com.example.ticktrack.db.DatabaseConnection;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Handler;
import android.os.Looper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private SessionManager session;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        session = new SessionManager(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        loadProfileData();

        binding.btnLogout.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Keluar")
                    .setMessage("Apakah Anda yakin ingin keluar?")
                    .setPositiveButton("Ya, Keluar", (d, w) -> {
                        session.logout();
                        startActivity(new Intent(requireContext(), LoginActivity.class));
                        requireActivity().finishAffinity();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });

        // Add click listeners for the newly added menus
        binding.btnEditProfile.setOnClickListener(v -> 
            startActivity(new Intent(requireContext(), EditProfileActivity.class))
        );

        binding.btnChangePassword.setOnClickListener(v -> 
            startActivity(new Intent(requireContext(), ChangePasswordActivity.class))
        );

        if ("admin".equals(session.getRole())) {
            binding.llAdminSection.setVisibility(View.VISIBLE);
            binding.btnUserManagement.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), com.example.ticktrack.activities.UserManagementActivity.class));
            });
            binding.btnCategoryManagement.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), com.example.ticktrack.activities.CategoryManagementActivity.class));
            });
            binding.btnAnalytics.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), com.example.ticktrack.activities.AnalyticsActivity.class));
            });
        } else {
            binding.llAdminSection.setVisibility(View.GONE);
        }

        // Dark mode logic removed

        binding.switchNotification.setChecked(session.isNotificationEnabled());
        binding.switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            session.setNotificationEnabled(isChecked);
        });
        
        binding.btnAbout.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tentang TickTrack")
                .setMessage("TickTrack v1.0.0\n\nAplikasi Helpdesk inovatif yang dirancang untuk mempercepat resolusi masalah IT Anda. Dibangun dengan cinta menggunakan Android & MySQL.")
                .setPositiveButton("Tutup", null)
                .show();
        });
        
        binding.btnHelp.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Bantuan")
                .setMessage("Pusat Bantuan TickTrack.\n\nJika Anda mengalami kendala atau membutuhkan panduan penggunaan aplikasi, silakan kirim email ke support@ticktrack.com.")
                .setPositiveButton("Tutup", null)
                .show();
        });

        return binding.getRoot();
    }

    private void loadProfileData() {
        if (binding != null && session != null) {
            binding.tvName.setText(session.getName());
            binding.tvEmail.setText(session.getEmail());
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
                                    binding.ivProfileAvatar.setImageBitmap(bitmap);
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
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

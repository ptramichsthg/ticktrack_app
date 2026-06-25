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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private SessionManager session;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        session = new SessionManager(requireContext());

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

        // Set initial state
        binding.switchDarkMode.setChecked(session.isDarkMode());

        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            session.setDarkMode(isChecked);
            if (isChecked) {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        binding.switchNotification.setChecked(session.isNotificationEnabled());
        binding.switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            session.setNotificationEnabled(isChecked);
        });

        return binding.getRoot();
    }

    private void loadProfileData() {
        if (binding != null && session != null) {
            binding.tvName.setText(session.getName());
            binding.tvEmail.setText(session.getEmail());
            binding.tvRole.setText(session.getRole().equals("admin") ? "Administrator" : "Pengguna");
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
    }
}

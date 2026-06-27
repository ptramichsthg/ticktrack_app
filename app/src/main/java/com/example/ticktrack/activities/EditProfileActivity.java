package com.example.ticktrack.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ticktrack.databinding.ActivityEditProfileBinding;
import com.example.ticktrack.db.DatabaseConnection;
import com.example.ticktrack.session.SessionManager;
import com.yalantis.ucrop.UCrop;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditProfileActivity extends AppCompatActivity {
    private ActivityEditProfileBinding binding;
    private SessionManager session;
    private ExecutorService executorService;
    private Handler mainHandler;
    private byte[] avatarBytes = null;
    
    private ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> galleryLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Load existing data
        binding.etName.setText(session.getName());
        binding.etEmail.setText(session.getEmail());
        
        loadCurrentAvatar();

        binding.btnSave.setOnClickListener(v -> saveProfile());
        binding.fabEditAvatar.setOnClickListener(v -> {
            galleryLauncher.launch(new androidx.activity.result.PickVisualMediaRequest.Builder()
                .setMediaType(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
        });
        
        galleryLauncher = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                startCrop(uri);
            }
        });
        
        cropLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri resultUri = UCrop.getOutput(result.getData());
                if (resultUri != null) {
                    processCroppedImage(resultUri);
                }
            } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                Throwable cropError = UCrop.getError(result.getData());
                if (cropError != null) {
                    Toast.makeText(this, "Gagal memotong gambar: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void startCrop(Uri sourceUri) {
        String destinationFileName = "avatar_" + System.currentTimeMillis() + ".jpg";
        File destinationFile = new File(getCacheDir(), destinationFileName);
        
        UCrop uCrop = UCrop.of(sourceUri, Uri.fromFile(destinationFile));
        uCrop.withAspectRatio(1, 1);
        uCrop.withMaxResultSize(500, 500);
        
        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(70);
        uCrop.withOptions(options);
        
        cropLauncher.launch(uCrop.getIntent(this));
    }
    
    private void processCroppedImage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            binding.ivAvatar.setImageBitmap(bitmap);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            avatarBytes = baos.toByteArray();
            
        } catch (Exception e) {
            Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadCurrentAvatar() {
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
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            mainHandler.post(() -> {
                                binding.ivAvatar.setImageBitmap(bitmap);
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

    private void saveProfile() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();

        if (name.isEmpty()) { binding.tilName.setError("Nama wajib diisi"); return; }
        binding.tilName.setError(null);
        if (email.isEmpty()) { binding.tilEmail.setError("Email wajib diisi"); return; }
        binding.tilEmail.setError(null);

        setLoading(true);

        executorService.execute(() -> {
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                conn = DatabaseConnection.getConnection();
                if (conn != null) {
                    if (avatarBytes != null) {
                        String query = "UPDATE users SET name = ?, email = ?, avatar = ? WHERE id = ?";
                        stmt = conn.prepareStatement(query);
                        stmt.setString(1, name);
                        stmt.setString(2, email);
                        stmt.setBytes(3, avatarBytes);
                        stmt.setInt(4, session.getUserId());
                    } else {
                        String query = "UPDATE users SET name = ?, email = ? WHERE id = ?";
                        stmt = conn.prepareStatement(query);
                        stmt.setString(1, name);
                        stmt.setString(2, email);
                        stmt.setInt(3, session.getUserId());
                    }

                    int result = stmt.executeUpdate();
                    if (result > 0) {
                        mainHandler.post(() -> {
                            session.updateProfile(name, email);
                            Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
                            setLoading(false);
                            finish();
                        });
                    } else {
                        mainHandler.post(() -> {
                            setLoading(false);
                            Toast.makeText(this, "Gagal memperbarui profil", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    mainHandler.post(() -> {
                        setLoading(false);
                        Toast.makeText(this, "Koneksi database gagal", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } finally {
                try { if(stmt != null) { stmt.close(); } } catch(Exception ignored){}
                try { if(conn != null) { conn.close(); } } catch(Exception ignored){}
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.btnSave.setEnabled(!isLoading);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnSave.setText(isLoading ? "Menyimpan..." : "Simpan Perubahan");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

package com.example.testfp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    // --- Permission constants ---
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 300;

    // --- UI element variables ---
    private FloatingActionButton btnCamera;
    private BottomNavigationView bottomNavigation;
    private TextView tvInfo;
    private ImageView ivHeader;
    private ImageView ivUserImage; // ADDED: Variable for the user's photo

    private RoboFlowAPI roboFlowAPI;

    // --- Modern ActivityResultLaunchers ---
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Find views by their IDs ---
        btnCamera = findViewById(R.id.btn_camera);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        tvInfo = findViewById(R.id.tv_info);
        ivHeader = findViewById(R.id.iv_header);
        ivUserImage = findViewById(R.id.iv_user_image); // ADDED: Link to the new ImageView

        roboFlowAPI = new RoboFlowAPI(this);

        initializeLaunchers();
        checkAndRequestPermissions();

        btnCamera.setOnClickListener(v -> takePhoto());

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_upload) {
                openImagePicker();
                return true;
            } else if (itemId == R.id.nav_history) {
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
                return true;
            }
            return false;
        });
    }

    private void initializeLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bitmap imageBitmap = (Bitmap) result.getData().getExtras().get("data");
                        if (imageBitmap != null) {
                            // MODIFIED: Added logic to show the image
                            displayAndClassifyImage(imageBitmap);
                        }
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                            // MODIFIED: Added logic to show the image
                            displayAndClassifyImage(imageBitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // ADDED: A new helper method to avoid repeating code
    private void displayAndClassifyImage(Bitmap bitmap) {
        // Show the user's image
        ivHeader.setVisibility(View.GONE); // Hide the header to make space
        ivUserImage.setVisibility(View.VISIBLE);
        ivUserImage.setImageBitmap(bitmap);

        // Classify the image
        classifyImage(bitmap);
    }

    private void takePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(cameraIntent);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    private void openImagePicker() {
        galleryLauncher.launch("image/*");
    }

    private void classifyImage(Bitmap bitmap) {
        tvInfo.setText("Classifying...");

        roboFlowAPI.classifyImage(bitmap, new RoboFlowAPI.ClassificationCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    tvInfo.setText(result);
                    HistoryManager.getInstance().addHistoryItem(bitmap, result);
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    tvInfo.setText("Error: " + error);
                });
            }
        });
    }

    // --- Permission Handling ---
    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission is required to access images on older Android versions", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
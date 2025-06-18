package com.example.testfp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView; // --- ADDED: Import for ImageView
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 200;
    private static final int STORAGE_PERMISSION_CODE = 300;

    // --- Updated UI element variables ---
    private FloatingActionButton btnCamera;
    private BottomNavigationView bottomNavigation;
    private TextView tvInfo;
    private ImageView ivBackgroundSoto; // --- ADDED: Variable for the main background image

    private RoboFlowAPI roboFlowAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Find views by their NEW IDs from the updated XML ---
        btnCamera = findViewById(R.id.btn_camera);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        tvInfo = findViewById(R.id.tv_info);
        ivBackgroundSoto = findViewById(R.id.iv_background_soto); // --- ADDED: Link to the ImageView

        roboFlowAPI = new RoboFlowAPI(this);

        checkAndRequestPermissions();

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        bottomNavigation.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_upload) {
                    openImagePicker();
                    return true;
                } else if (itemId == R.id.nav_history) {
                    Toast.makeText(MainActivity.this, "History clicked!", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });
        bottomNavigation.setItemIconTintList(null);
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, STORAGE_PERMISSION_CODE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            }
        }
    }

    private void takePhoto() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), GALLERY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bitmap imageBitmap = null;
            if (requestCode == CAMERA_REQUEST_CODE && data != null) {
                imageBitmap = (Bitmap) data.getExtras().get("data");
            } else if (requestCode == GALLERY_REQUEST_CODE && data != null) {
                Uri selectedImageUri = data.getData();
                try {
                    imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }

            if (imageBitmap != null) {
                // --- ADDED: Set the user's image as the new background ---
                ivBackgroundSoto.setImageBitmap(imageBitmap);

                // We still classify the image and update the text as before
                classifyImage(imageBitmap);
            }
        }
    }

    private void classifyImage(Bitmap bitmap) {
        tvInfo.setText("Classifying...");

        roboFlowAPI.classifyImage(bitmap, new RoboFlowAPI.ClassificationCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    tvInfo.setText(result);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission is required to access images", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
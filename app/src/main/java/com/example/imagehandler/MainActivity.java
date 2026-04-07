package com.example.imagehandler;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.DocumentsContract;
import java.io.OutputStream;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_FOLDER_PICKER = 102;

    LinearLayout cameraBlock, createFolderBlock, openFolderBlock;
    TextView textViewCurrentFolder;

    private File defaultFolder;
    private Uri currentFolderUri = null;   // To store selected folder
    private String currentFolderName = "ImageHandler"; // Default name

    private static final String PREF_NAME = "AppPreferences";
    private static final String KEY_FOLDER_URI = "selected_folder_uri";
    private static final String KEY_FOLDER_NAME = "selected_folder_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraBlock = findViewById(R.id.cameraBlock);
        createFolderBlock = findViewById(R.id.createFolderBlock);
        openFolderBlock = findViewById(R.id.openFolderBlock);
        textViewCurrentFolder = findViewById(R.id.textView);

        defaultFolder = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "ImageHandler");

        loadLastSelectedFolder();

        // Camera Click
        cameraBlock.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                openCamera();
            } else {
                requestCameraPermission();
            }
        });

        // Choose Folder Click
        createFolderBlock.setOnClickListener(v -> openFolderPicker());

        // Open Folder Click - Opens currently selected folder
        openFolderBlock.setOnClickListener(v -> openCurrentFolder());
    }

    private void loadLastSelectedFolder() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String uriString = prefs.getString(KEY_FOLDER_URI, null);
        String name = prefs.getString(KEY_FOLDER_NAME, "ImageHandler");

        if (uriString != null) {
            currentFolderUri = Uri.parse(uriString);
            currentFolderName = name;
        }
        updateCurrentFolderText();
    }

    private void saveSelectedFolder(Uri uri, String name) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_FOLDER_URI, uri.toString())
                .putString(KEY_FOLDER_NAME, name)
                .apply();

        currentFolderUri = uri;
        currentFolderName = name;
        updateCurrentFolderText();
    }

    private void updateCurrentFolderText() {
        textViewCurrentFolder.setText("Current Folder: " + currentFolderName);
    }

    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_FOLDER_PICKER);
    }

    private void openCurrentFolder() {
        Intent intent = new Intent(MainActivity.this, GalleryActivity.class);

        if (currentFolderUri != null) {
            intent.putExtra("folderUri", currentFolderUri.toString());
            intent.putExtra("isDefaultFolder", false);
            intent.putExtra("folderName", currentFolderName);
        } else {
            intent.putExtra("isDefaultFolder", true);
        }
        startActivity(intent);
    }

    // ==================== Camera Code ====================
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA_PERMISSION);
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            if (photo != null) {
                saveImageToFolder(photo);
            }
        } else if (requestCode == REQUEST_FOLDER_PICKER && resultCode == RESULT_OK && data != null) {
            Uri treeUri = data.getData();
            if (treeUri != null) {
                getContentResolver().takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );
                // Get folder name
                String folderName = "Selected Folder";
                try {
                    folderName = treeUri.getLastPathSegment();
                    if (folderName != null && folderName.contains(":")) {
                        folderName = folderName.substring(folderName.lastIndexOf(":") + 1);
                    }
                } catch (Exception ignored) {
                }

                saveSelectedFolder(treeUri, folderName);

                Toast.makeText(this, "Folder selected: " + folderName, Toast.LENGTH_SHORT).show();

                // Open the folder immediately after selecting
                openCurrentFolder();
            }
        }
    }

    private void saveImageToFolder(Bitmap bitmap) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timeStamp + ".jpg";

        if (currentFolderUri != null) {
            // ✅ SAF MODE (User selected folder)
            try {
                Uri docUri = DocumentsContract.buildDocumentUriUsingTree(
                        currentFolderUri,
                        DocumentsContract.getTreeDocumentId(currentFolderUri)
                );

                Uri newFileUri = DocumentsContract.createDocument(
                        getContentResolver(),
                        docUri,
                        "image/jpeg",
                        fileName
                );

                if (newFileUri != null) {
                    try (OutputStream os = getContentResolver().openOutputStream(newFileUri)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
                        Toast.makeText(this, "Saved to selected folder", Toast.LENGTH_SHORT).show();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to save in selected folder", Toast.LENGTH_SHORT).show();
            }

        } else {
            // ✅ DEFAULT MODE
            if (!defaultFolder.exists()) defaultFolder.mkdirs();

            File imageFile = new File(defaultFolder, fileName);

            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                Toast.makeText(this, "Saved in ImageHandler", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
package com.example.imagehandler;

import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageDetailActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_PATH = "image_path";
    public static final String EXTRA_IMAGE_URI = "image_uri";

    private File imageFile;
    private Uri imageUri;
    private boolean isUriMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detail);

        ImageView imageView = findViewById(R.id.detailImageView);
        TextView tvName = findViewById(R.id.tvImageName);
        TextView tvPath = findViewById(R.id.tvImagePath);
        TextView tvSize = findViewById(R.id.tvImageSize);
        TextView tvDate = findViewById(R.id.tvImageDate);
        Button btnDelete = findViewById(R.id.btnDelete);

        // Check which type of image was passed
        String path = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        String uriString = getIntent().getStringExtra(EXTRA_IMAGE_URI);

        if (path != null) {
            // Default folder case (File)
            imageFile = new File(path);
            loadFileImage(imageView, tvName, tvPath, tvSize, tvDate);
        }
        else if (uriString != null) {
            // User selected folder case (Uri)
            imageUri = Uri.parse(uriString);
            isUriMode = true;
            loadUriImage(imageView, tvName, tvPath, tvSize, tvDate);
        }
        else {
            Toast.makeText(this, "No image data found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void loadFileImage(ImageView imageView, TextView tvName, TextView tvPath,
                               TextView tvSize, TextView tvDate) {
        imageView.setImageBitmap(BitmapFactory.decodeFile(imageFile.getAbsolutePath()));

        tvName.setText("Name: " + imageFile.getName());
        tvPath.setText("Path: " + imageFile.getAbsolutePath());
        tvSize.setText("Size: " + imageFile.length() / 1024 + " KB");

        Date lastModDate = new Date(imageFile.lastModified());
        String date = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(lastModDate);
        tvDate.setText("Date: " + date);
    }

    private void loadUriImage(ImageView imageView, TextView tvName, TextView tvPath,
                              TextView tvSize, TextView tvDate) {
        try {
            // Load Image
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            imageView.setImageBitmap(bitmap);

            // Get metadata using ContentResolver
            Cursor cursor = getContentResolver().query(imageUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                long size = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));

                tvName.setText("Name: " + (name != null ? name : "Unknown"));
                tvPath.setText("Path: " + imageUri.toString());
                tvSize.setText("Size: " + (size / 1024) + " KB");
                tvDate.setText("Date: (From Document Provider)");

                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image details", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image?")
                .setPositiveButton("Delete", (dialog, which) -> deleteImage())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteImage() {
        if (!isUriMode && imageFile != null) {
            // File mode (Default folder)
            if (imageFile.delete()) {
                Toast.makeText(this, "Image deleted successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show();
            }
        }
        else if (isUriMode && imageUri != null) {
            // Uri mode (User selected folder)
            try {
                boolean deleted = DocumentsContract.deleteDocument(getContentResolver(), imageUri);
                if (deleted) {
                    Toast.makeText(this, "Image deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Cannot delete from this folder type", Toast.LENGTH_LONG).show();
            }
        }
    }
}
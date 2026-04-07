package com.example.imagehandler;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private GalleryAdapter adapter;
    private File folder;

    private boolean isDefault;
    private String uriString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        recyclerView = findViewById(R.id.recyclerViewGallery);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        isDefault = getIntent().getBooleanExtra("isDefaultFolder", true);
        uriString = getIntent().getStringExtra("folderUri");

        if (isDefault) {
            folder = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "ImageHandler");
            loadImagesFromFile();
        } else if (uriString != null) {
            loadImagesFromUri(Uri.parse(uriString));
        } else {
            Toast.makeText(this, "No folder selected", Toast.LENGTH_SHORT).show();
        }
    }

    // 🔥 IMPORTANT FIX: Refresh when coming back
    @Override
    protected void onResume() {
        super.onResume();

        if (isDefault) {
            loadImagesFromFile();
        } else if (uriString != null) {
            loadImagesFromUri(Uri.parse(uriString));
        }
    }

    // ================= FILE MODE =================
    private void loadImagesFromFile() {
        List<File> imageFiles = new ArrayList<>();

        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (isImageFile(file)) {
                        imageFiles.add(file);
                    }
                }
            }
        }

        if (imageFiles.isEmpty()) {
            Toast.makeText(this, "No images found in this folder", Toast.LENGTH_SHORT).show();
        }

        adapter = new GalleryAdapter(imageFiles, null);
        recyclerView.setAdapter(adapter);
    }

    // ================= URI MODE =================
    private void loadImagesFromUri(Uri treeUri) {
        List<Uri> imageUris = new ArrayList<>();

        try {
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri, DocumentsContract.getTreeDocumentId(treeUri));

            Cursor cursor = getContentResolver().query(childrenUri,
                    new String[]{
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_MIME_TYPE
                    },
                    null, null, null);

            if (cursor != null) {
                while (cursor.moveToNext()) {

                    String mimeType = cursor.getString(
                            cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE));

                    if (mimeType != null && mimeType.startsWith("image/")) {

                        String documentId = cursor.getString(
                                cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID));

                        Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
                        imageUris.add(fileUri);
                    }
                }
                cursor.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading folder: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        if (imageUris.isEmpty()) {
            Toast.makeText(this, "No images found", Toast.LENGTH_SHORT).show();
        }

        adapter = new GalleryAdapter(null, imageUris);
        recyclerView.setAdapter(adapter);
    }

    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
    }

    // ================= ADAPTER =================
    private class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

        private final List<File> files;
        private final List<Uri> uris;

        public GalleryAdapter(List<File> files, List<Uri> uris) {
            this.files = files;
            this.uris = uris;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_image, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            if (files != null) {
                File file = files.get(position);
                holder.imageView.setImageBitmap(
                        BitmapFactory.decodeFile(file.getAbsolutePath()));

                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(GalleryActivity.this, ImageDetailActivity.class);
                    intent.putExtra(ImageDetailActivity.EXTRA_IMAGE_PATH, file.getAbsolutePath());
                    startActivity(intent);
                });

            } else if (uris != null) {
                Uri uri = uris.get(position);
                holder.imageView.setImageURI(uri);

                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(GalleryActivity.this, ImageDetailActivity.class);
                    intent.putExtra(ImageDetailActivity.EXTRA_IMAGE_URI, uri.toString());
                    startActivity(intent);
                });
            }
        }

        @Override
        public int getItemCount() {
            if (files != null) return files.size();
            if (uris != null) return uris.size();
            return 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public ViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageViewItem);
            }
        }
    }
}
package com.example.softnotesandcanvas;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.softnotesandcanvas.databinding.ActivityCanvasEditorBinding;
import com.example.softnotesandcanvas.db.Note;
import com.example.softnotesandcanvas.sync.DeviceUtil;
import com.example.softnotesandcanvas.viewmodel.NoteViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CanvasEditorActivity extends AppCompatActivity {

    private ActivityCanvasEditorBinding binding;
    private NoteViewModel noteViewModel;
    private Note currentNote;
    private FirebaseAuth mAuth;
    private boolean isNewNote = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityCanvasEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarCanvas);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("New Canvas");

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        mAuth = FirebaseAuth.getInstance();

        if (getIntent().hasExtra("EXISTING_NOTE")) {
            currentNote = (Note) getIntent().getSerializableExtra("EXISTING_NOTE");
            isNewNote = false;
            binding.noteTitleCanvas.setText(currentNote.title);
            getSupportActionBar().setTitle("Edit Canvas");
            loadCanvas();
        } else {
            currentNote = new Note();
            currentNote.type = Note.TYPE_CANVAS; // Set the type
        }

        setupToolbarButtons();
    }

    private void setupToolbarButtons() {
        binding.buttonPen.setOnClickListener(v -> binding.canvasView.setPenMode());
        binding.buttonEraser.setOnClickListener(v -> binding.canvasView.setEraserMode());
        binding.buttonText.setOnClickListener(v -> {
            // Text functionality is complex. We'll add a placeholder.
            Toast.makeText(this, "Text tool coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadCanvas() {
        if (currentNote.canvasImagePath != null && !currentNote.canvasImagePath.isEmpty()) {
            try {
                File file = new File(currentNote.canvasImagePath);
                if (file.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    binding.canvasView.loadBitmap(bitmap);
                }
            } catch (Exception e) {
                Log.e("CanvasEditor", "Error loading bitmap", e);
                Toast.makeText(this, "Error loading drawing", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveCanvasNote() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be signed in to save", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = binding.noteTitleCanvas.getText().toString().trim();
        if (title.isEmpty()) {
            title = "Untitled Canvas";
        }

        // 1. Get Bitmap from CanvasView
        Bitmap bitmap = binding.canvasView.getBitmap();
        if (bitmap == null) {
            Log.e("CanvasEditor", "Bitmap is null, cannot save.");
            return;
        }

        // 2. Save Bitmap to a file
        String filePath = saveBitmapToFile(bitmap);
        if (filePath == null) {
            Toast.makeText(this, "Error saving drawing", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Create/Update Note object
        String deviceId = DeviceUtil.getDeviceId(this);
        currentNote.title = title;
        currentNote.canvasImagePath = filePath; // Store the file path
        currentNote.content = null; // Canvas notes have no text content
        currentNote.type = Note.TYPE_CANVAS;

        if (isNewNote) {
            // We create the new note manually
            currentNote = new Note(); // Use default constructor
            currentNote.id = java.util.UUID.randomUUID().toString();
            currentNote.userId = user.getUid();
            currentNote.title = title;
            currentNote.canvasImagePath = filePath;
            currentNote.type = Note.TYPE_CANVAS;
            currentNote.content = null;
            currentNote.createdAt = new java.util.Date();
            currentNote.updatedAt = currentNote.createdAt;
            currentNote.lastEditedByDeviceId = deviceId;
            currentNote.isDeleted = false;
            currentNote.isTrashed = false;
            currentNote.syncStatus = com.example.softnotesandcanvas.db.SyncStatus.SYNCING;

            noteViewModel.insert(currentNote); // This now works
        } else {
            currentNote.lastEditedByDeviceId = deviceId;
            noteViewModel.update(currentNote);
        }

        Toast.makeText(this, "Canvas saved", Toast.LENGTH_SHORT).show();
    }

    private String saveBitmapToFile(Bitmap bitmap) {
        // Create a unique filename
        String fileName = "canvas_" + System.currentTimeMillis() + ".png";
        // Get the app's internal files directory
        File directory = getFilesDir();
        File file = new File(directory, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            // Return the absolute path to the saved file
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e("CanvasEditor", "Error saving bitmap to file", e);
            return null;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        // Save the note when backing out
        saveCanvasNote();
        super.onBackPressed();
    }
}
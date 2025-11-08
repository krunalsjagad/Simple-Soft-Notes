package com.example.softnotesandcanvas;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.softnotesandcanvas.db.Note; // Import the Note class
import com.example.softnotesandcanvas.viewmodel.NoteViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText noteEditText;
    private NoteViewModel noteViewModel;
    private Note existingNote; // To hold the note being edited

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        noteEditText = findViewById(R.id.note_edit_text);
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EXISTING_NOTE")) {
            existingNote = (Note) intent.getSerializableExtra("EXISTING_NOTE");
            getSupportActionBar().setTitle("Edit Note");
            if (existingNote != null) {
                // Combine title and content for editing in one field
                String fullText = existingNote.title + "\n" + existingNote.content;
                noteEditText.setText(fullText);
                noteEditText.setSelection(fullText.length()); // Move cursor to the end
            }
        } else {
            existingNote = null;
            getSupportActionBar().setTitle("New Note");
        }

        // Get the current user and pass the UID to the ViewModel
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            noteViewModel.loadNotesForUser(user.getUid());
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { // Handle back arrow click
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_save) {
            saveNote();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveNote() {
        String noteText = noteEditText.getText().toString().trim();
        if (noteText.isEmpty()) {
            // Optional: Show a toast message that note can't be empty
            return;
        }

        // Split text back into title (first line) and content (rest)
        String[] lines = noteText.split("\n", 2);
        String title = lines[0];
        String content = lines.length > 1 ? lines[1] : "";

        if (existingNote != null) {
            // We are editing an existing note
            existingNote.title = title;
            existingNote.content = content;
            noteViewModel.update(existingNote);
        } else {
            // We are creating a new note
            noteViewModel.insert(title, content);
        }

        finish(); // Close the editor and return to the main activity
    }
}
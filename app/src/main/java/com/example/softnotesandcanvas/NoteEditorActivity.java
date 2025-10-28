package com.example.softnotesandcanvas;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.softnotesandcanvas.viewmodel.NoteViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText noteEditText;
    private NoteViewModel noteViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        noteEditText = findViewById(R.id.note_edit_text);
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        // Get the current user and pass the UID to the ViewModel
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            noteViewModel.loadNotesForUser(user.getUid());
        }


        // Set up the toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Edit Note");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveNote();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveNote() {
        String noteText = noteEditText.getText().toString();
        if (!noteText.trim().isEmpty()) {
            // Split the note text into title and content
            String[] lines = noteText.split("\n", 2);
            String title = lines[0];
            String content = lines.length > 1 ? lines[1] : "";

            noteViewModel.insert(title, content);
            finish(); // Close the editor and return to the main activity
        }
    }
}
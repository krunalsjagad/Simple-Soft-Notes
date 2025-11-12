package com.example.softnotesandcanvas;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.softnotesandcanvas.databinding.ActivityNoteEditorBinding;
import com.example.softnotesandcanvas.db.Note;
import com.example.softnotesandcanvas.sync.DeviceUtil;
import com.example.softnotesandcanvas.viewmodel.NoteViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Date;

public class NoteEditorActivity extends AppCompatActivity {

    private ActivityNoteEditorBinding binding;
    private NoteViewModel noteViewModel;
    private Note currentNote;
    private boolean isNewNote = true;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityNoteEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("New Note");

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        mAuth = FirebaseAuth.getInstance();

        // --- ADD THESE LINES ---
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return; // Don't continue if user is null
        }
        // This ensures the ViewModel knows the user's UID for save/update operations
        noteViewModel.loadNotesForUser(user.getUid());

        if (getIntent().hasExtra("EXISTING_NOTE")) {
            currentNote = (Note) getIntent().getSerializableExtra("EXISTING_NOTE");
            isNewNote = false;
            binding.noteTitle.setText(currentNote.title);

            // Load content as HTML
            if (currentNote.content != null) {
                binding.noteContent.setText(Html.fromHtml(currentNote.content, Html.FROM_HTML_MODE_LEGACY));
            }

            getSupportActionBar().setTitle("Edit Note");
            // Hide formatting bar for canvas notes (though they shouldn't open here)
            if (currentNote.type != null && currentNote.type.equals(Note.TYPE_CANVAS)) {
                binding.formattingToolbar.setVisibility(View.GONE);
                binding.noteContent.setHint("This is a canvas note and cannot be edited as text.");
                binding.noteContent.setEnabled(false);
            }
        } else {
            // It's a new note, check the type from MainActivity's FAB dialog
            String noteType = getIntent().getStringExtra("noteType");
            currentNote = new Note();
            currentNote.type = (noteType != null) ? noteType : Note.TYPE_TEXT;

            if (currentNote.type.equals(Note.TYPE_CANVAS)) {
                // This shouldn't happen, but as a fallback...
                Toast.makeText(this, "Error: Opening text editor for canvas.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        // Setup formatting button listeners
        setupFormattingToolbar();
    }

    private void setupFormattingToolbar() {
        binding.buttonBold.setOnClickListener(v -> applyStyle(new StyleSpan(Typeface.BOLD)));
        binding.buttonItalic.setOnClickListener(v -> applyStyle(new StyleSpan(Typeface.ITALIC)));
        binding.buttonUnderline.setOnClickListener(v -> applyStyle(new UnderlineSpan()));
        binding.buttonStrikethrough.setOnClickListener(v -> applyStyle(new StrikethroughSpan()));
    }

    /**
     * Applies or removes a CharacterStyle (Bold, Italic, etc.) to the selected text.
     * @param styleToApply The style to apply (e.g., new StyleSpan(Typeface.BOLD))
     */
    private void applyStyle(CharacterStyle styleToApply) {
        Editable editable = binding.noteContent.getEditableText();
        int start = binding.noteContent.getSelectionStart();
        int end = binding.noteContent.getSelectionEnd();

        if (start == end) {
            // No selection, don't do anything (or you could toggle for future typing)
            return;
        }

        // Get all spans of the *same type* within the selection
        CharacterStyle[] existingSpans = editable.getSpans(start, end, styleToApply.getClass());

        boolean styleExists = false;
        if (existingSpans.length > 0) {
            // For simplicity, we check if *any* part of the selection has the style.
            // A more complex implementation would handle partial overlaps.
            styleExists = true;
        }

        // If the style already exists, remove it. Otherwise, add it.
        if (styleExists) {
            // Remove all spans of this type from the selection
            for (CharacterStyle span : existingSpans) {
                editable.removeSpan(span);
            }
        } else {
            // Apply the new style
            editable.setSpan(styleToApply, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }


    private void saveNote() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be signed in to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // Only save if it's a text note
        if (currentNote.type.equals(Note.TYPE_TEXT)) {
            String title = binding.noteTitle.getText().toString().trim();

            // Get editable text and convert to HTML
            Editable editable = binding.noteContent.getEditableText();
            String contentHtml = Html.toHtml(editable, Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL);

            if (title.isEmpty() && contentHtml.isEmpty()) {
                // Don't save an empty note
                Toast.makeText(this, "Empty note discarded", Toast.LENGTH_SHORT).show();
                return;
            }

            String deviceId = DeviceUtil.getDeviceId(this);

            currentNote.title = title.isEmpty() ? "Untitled Note" : title;
            currentNote.content = contentHtml; // Save the HTML content
            currentNote.canvasImagePath = null; // Ensure this is null for text notes
            currentNote.type = Note.TYPE_TEXT;

            if (isNewNote) {
                // Use the text constructor
                currentNote = new Note(user.getUid(), currentNote.title, currentNote.content, deviceId);

                // --- THIS IS THE FIX ---
                // We call the insert method that takes a full Note object
                // OLD: noteViewModel.insert(currentNote.title, currentNote.content);
                noteViewModel.insert(currentNote); //
            } else {
                currentNote.lastEditedByDeviceId = deviceId;
                currentNote.updatedAt = new Date(); // Update timestamp
                noteViewModel.update(currentNote);
            }
            Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (item.getItemId() == R.id.action_delete) {
            // We should trash the note
            if (!isNewNote) {
                noteViewModel.trash(currentNote);
                Toast.makeText(this, "Note moved to trash", Toast.LENGTH_SHORT).show();
                finish(); // Close the editor
            } else {
                Toast.makeText(this, "Note discarded", Toast.LENGTH_SHORT).show();
                finish(); // Just close, no need to save or trash
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        saveNote();
        super.onBackPressed();
    }
}
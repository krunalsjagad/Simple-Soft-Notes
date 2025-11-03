package com.example.softnotesandcanvas;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.softnotesandcanvas.databinding.ActivityTrashBinding;
import com.example.softnotesandcanvas.db.Note;
import com.example.softnotesandcanvas.ui.NoteAdapter;
import com.example.softnotesandcanvas.viewmodel.NoteViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class TrashActivity extends AppCompatActivity {

    private ActivityTrashBinding binding;
    private NoteViewModel noteViewModel;
    private NoteAdapter noteAdapter;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTrashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarTrash);
        // Add back button to the toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        mAuth = FirebaseAuth.getInstance();

        // RecyclerView setup
        noteAdapter = new NoteAdapter(note -> {
            // Optional: Define what happens when a trashed note is clicked
            // For example, you could show an option to restore it.
        });
        binding.recyclerViewTrash.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewTrash.setAdapter(noteAdapter);

        // ViewModel setup
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        loadTrashedNotes();
    }

    private void loadTrashedNotes() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // IMPORTANT: You will need to create the getTrashedNotes method in your ViewModel
            noteViewModel.getTrashedNotes(currentUser.getUid()).observe(this, trashedNotes -> {
                noteAdapter.submitList(trashedNotes);
                binding.emptyViewTrash.setVisibility((trashedNotes == null || trashedNotes.isEmpty()) ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle the back button click
        onBackPressed();
        return true;
    }
}
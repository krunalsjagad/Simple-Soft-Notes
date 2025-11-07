package com.example.softnotesandcanvas;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.softnotesandcanvas.databinding.ActivityTrashBinding;
import com.example.softnotesandcanvas.db.Note;
import com.example.softnotesandcanvas.ui.NoteAdapter;
import com.example.softnotesandcanvas.viewmodel.NoteViewModel;
import com.google.android.material.snackbar.Snackbar;
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

        // ✅ NEW: Add swipe functionality for restore and delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                Note note = noteAdapter.getCurrentList().get(viewHolder.getAdapterPosition());

                if (direction == ItemTouchHelper.LEFT) {
                    // Swipe Left: Restore
                    noteViewModel.restore(note);
                    Snackbar.make(binding.getRoot(), "Note restored", Snackbar.LENGTH_SHORT).show();
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Swipe Right: Delete Permanently (with confirmation)
                    new AlertDialog.Builder(TrashActivity.this)
                            .setTitle("Delete Note")
                            .setMessage("Are you sure you want to permanently delete this note? This action cannot be undone.")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                noteViewModel.deletePermanently(note);
                                Snackbar.make(binding.getRoot(), "Note permanently deleted", Snackbar.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                // User cancelled, adapter needs to be notified to re-draw the item
                                noteAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                            })
                            .setOnCancelListener(dialog -> {
                                // Also handle back button press during dialog
                                noteAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                            })
                            .show();
                }
            }
        }).attachToRecyclerView(binding.recyclerViewTrash);
    }

    private void loadTrashedNotes() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // This will now correctly get notes that are trashed but not permanently deleted
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
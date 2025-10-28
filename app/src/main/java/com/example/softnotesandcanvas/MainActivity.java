package com.example.softnotesandcanvas;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.softnotesandcanvas.databinding.ActivityMainBinding;
import com.example.softnotesandcanvas.db.AppDatabase;
import com.example.softnotesandcanvas.db.Note;
import com.example.softnotesandcanvas.ui.NoteAdapter;
import com.example.softnotesandcanvas.viewmodel.NoteViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private AppDatabase mDb;

    private NoteViewModel noteViewModel;
    private NoteAdapter noteAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        mAuth = FirebaseAuth.getInstance();
        mDb = AppDatabase.getInstance(getApplicationContext());

        // RecyclerView setup
        noteAdapter = new NoteAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(noteAdapter);

        // ViewModel setup
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        // FAB click listener
        binding.fab.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                goToAuthActivity();
                return;
            }
            Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
            startActivity(intent);
        });

        binding.swipeRefresh.setOnRefreshListener(() -> {
            binding.swipeRefresh.setRefreshing(false);
        });

        // Add swipe-to-delete functionality
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Get the note at the swiped position
                Note noteToDelete = noteAdapter.getCurrentList().get(viewHolder.getAdapterPosition());
                // Delete the note
                noteViewModel.delete(noteToDelete);
                // Show a snackbar with an undo option
                Snackbar.make(binding.getRoot(), "Note deleted", Snackbar.LENGTH_LONG)
                        .setAction("Undo", v -> {
                            // To "undo" a soft delete, we'd need a method in the repository
                            // to set isDeleted = false. For now, we'll just re-insert it
                            // as a new note for simplicity.
                            noteViewModel.insert(noteToDelete.title, noteToDelete.content);
                        }).show();
            }
        }).attachToRecyclerView(binding.recyclerView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToAuthActivity();
        } else {
            // Load notes and then start observing
            noteViewModel.loadNotesForUser(currentUser.getUid());
            noteViewModel.getNotes().observe(this, notes -> {
                noteAdapter.submitList(notes);
                binding.emptyView.setVisibility((notes == null || notes.isEmpty()) ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sign_out) {
            signOutAndClearData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOutAndClearData() {
        mAuth.signOut();

        // Stop observing before clearing data
        noteViewModel.getNotes().removeObservers(this);

        // Clear local data
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            mDb.noteDao().nukeTable();
        });

        goToAuthActivity();
    }

    private void goToAuthActivity() {
        Intent intent = new Intent(MainActivity.this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
package com.example.softnotesandcanvas;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.softnotesandcanvas.databinding.ActivityMainBinding;
import com.example.softnotesandcanvas.db.AppDatabase;
// ✅ Import the single, consolidated Note model
import com.example.softnotesandcanvas.db.Note;
import com.example.softnotesandcanvas.ui.NoteAdapter;
import com.example.softnotesandcanvas.viewmodel.NoteViewModel;
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

        // Set up the toolbar
        setSupportActionBar(binding.toolbar);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Initialize Room Database
        mDb = AppDatabase.getInstance(getApplicationContext());

        // RecyclerView setup
        noteAdapter = new NoteAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(noteAdapter);

        // ViewModel setup
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        // FAB click listener - create a new note (launch NoteEditorActivity or Auth-protected editor)
        binding.fab.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                goToAuthActivity();
                return;
            }
            // TODO: Create NoteEditorActivity.class
            // For now, we can test creating a new note directly
            noteViewModel.insert("New Note", "Tap to edit...");
            // Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
            // startActivity(intent);
        });

        // Swipe-to-refresh or empty view handling
        binding.swipeRefresh.setOnRefreshListener(() -> {
            // With live data and a Firestore listener, manual refresh is less critical,
            // but we can leave it to manually trigger a sync if needed.
            // For now, just stop the spinner.
            binding.swipeRefresh.setRefreshing(false);
        });

        // Observe notes LiveData
        // ✅ Check for null before observing, as notes is loaded in onStart
        if (noteViewModel.getNotes() != null) {
            noteViewModel.getNotes().observe(this, notes -> {
                noteAdapter.submitList(notes);
                binding.emptyView.setVisibility((notes == null || notes.isEmpty()) ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // No user is signed in, redirect to AuthActivity
            goToAuthActivity();
        } else {
            // User is signed in. Load notes and start listener.
            noteViewModel.loadNotesForUser(currentUser.getUid());

            // ✅ Observe notes LiveData here, *after* loadNotesForUser is called
            noteViewModel.getNotes().observe(this, notes -> {
                noteAdapter.submitList(notes);
                binding.emptyView.setVisibility((notes == null || notes.isEmpty()) ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here.
        if (item.getItemId() == R.id.action_sign_out) {
            signOutAndClearData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOutAndClearData() {
        // 1. Sign out from Firebase
        mAuth.signOut();

        // 2. Clear all local data from Room database
        // ✅ This is now handled by the repository which also stops listeners
        noteViewModel.getNotes().removeObservers(this); // Stop observing
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class); // Get new VM instance

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            mDb.noteDao().nukeTable();
        });


        // 3. Redirect to AuthActivity
        goToAuthActivity();
    }

    private void goToAuthActivity() {
        Intent intent = new Intent(MainActivity.this, AuthActivity.class);
        // Clear the activity stack
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close this MainActivity
    }
}

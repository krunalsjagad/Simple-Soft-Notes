package com.example.softnotesandcanvas;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.widget.TextView;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
// ADDED: Import for theme delegate
import androidx.appcompat.app.AppCompatDelegate;
// ADDED: Import for Switch
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.softnotesandcanvas.databinding.ActivityMainBinding;
import com.example.softnotesandcanvas.db.AppDatabase;
import com.example.softnotesandcanvas.db.Note;
import com.example.softnotesandcanvas.ui.NoteAdapter;
import com.example.softnotesandcanvas.viewmodel.NoteViewModel;
// ADDED: Import for new ThemeHelper class
import com.example.softnotesandcanvas.ThemeHelper;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// --- ADD THESE IMPORTS for the dialog ---
import android.app.Dialog;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
// ----------------------------------------

public class MainActivity extends AppCompatActivity implements NoteAdapter.OnItemClickListener {

    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private AppDatabase mDb;

    private NoteViewModel noteViewModel;
    private NoteAdapter noteAdapter;

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ✅ Initialize views AFTER setContentView
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // ✅ NEW: Listen for text changes in the search bar
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                noteViewModel.search(newText);
                return true;
            }
        });

        // --- ADDED: Theme Switch Setup ---
        // Get the theme menu item
        MenuItem themeItem = navigationView.getMenu().findItem(R.id.nav_theme);
        View actionView = themeItem.getActionView();

        // Ensure the actionView is a SwitchCompat
        if (actionView instanceof SwitchCompat) {
            SwitchCompat themeSwitch = (SwitchCompat) actionView;

            // Set the switch's initial state based on the saved preference
            themeSwitch.setChecked(ThemeHelper.isDarkMode(this));

            // Set a listener to react to switch changes
            themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Save and apply the new theme
                // This will cause the activity to restart
                ThemeHelper.setTheme(this, isChecked);
            });
        }
        // --- End Theme Switch Setup ---

        setSupportActionBar(binding.toolbar);

        // ✅ Get the header view from the navigation view FIRST
        View headerView = navigationView.getHeaderView(0);

        // ✅ NOW you can find the TextView inside the header view
        TextView navHeaderName = headerView.findViewById(R.id.nav_header_name);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userName = prefs.getString("user_name", "SoftNotes User");

        navHeaderName.setText(getCapitalizedName(userName));

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, binding.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        mAuth = FirebaseAuth.getInstance();
        mDb = AppDatabase.getInstance(getApplicationContext());

        // RecyclerView setup
        noteAdapter = new NoteAdapter(this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(noteAdapter);

        // ViewModel setup
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        // ✅ NEW: Set up Filter Toggle Listener
        binding.filterToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnFilterAll) {
                    noteViewModel.setFilterMode(NoteViewModel.FilterMode.ALL);
                } else if (checkedId == R.id.btnFilterNotes) {
                    noteViewModel.setFilterMode(NoteViewModel.FilterMode.TEXT_ONLY);
                } else if (checkedId == R.id.btnFilterCanvas) {
                    noteViewModel.setFilterMode(NoteViewModel.FilterMode.CANVAS_ONLY);
                }
            }
        });

        // --- MODIFY THIS FAB CLICK LISTENER ---
        binding.fab.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                goToAuthActivity();
                return;
            }
            // Instead of starting the editor, show the options dialog
            showAddOptionsDialog();
        });
        // --- END OF MODIFICATION ---

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
                Note noteToTrash = noteAdapter.getCurrentList().get(viewHolder.getAdapterPosition());
                noteViewModel.trash(noteToTrash);
                // Show a snackbar with an undo option
                Snackbar.make(binding.getRoot(), "Note moved to trash", Snackbar.LENGTH_LONG)
                        .setAction("Undo", v -> {
                            // To "undo" a soft delete, we'd need a method in the repository
                            // to set isDeleted = false. For now, we'll just re-insert it
                            // as a new note for simplicity.
                            noteViewModel.restore(noteToTrash);
                        }).show();
            }
        }).attachToRecyclerView(binding.recyclerView);

        // ✅ Add the listener for the navigation drawer HERE
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // ADDED: Handle theme item click to prevent drawer from closing
            if (itemId == R.id.nav_theme) {
                // Do nothing when the row is clicked; the switch listener will handle it
                return false; // Return false so it doesn't get "selected"
            }

            if (itemId == R.id.nav_search) {
                // Toggle visibility
                if (binding.searchView.getVisibility() == View.VISIBLE) {
                    binding.searchView.setVisibility(View.GONE);

                    // ✅ NEW: Clear search when hiding so all notes reappear
                    binding.searchView.setQuery("", false);
                    noteViewModel.search("");
                } else {
                    binding.searchView.setVisibility(View.VISIBLE);
                    binding.searchView.setIconified(false); // Open keyboard
                }
            } else if (itemId == R.id.nav_new_note) {
                binding.fab.performClick();
            } else if (itemId == R.id.nav_trash) {
                startActivity(new Intent(MainActivity.this, TrashActivity.class));
                // MODIFIED: Removed 'nav_settings' from this 'else if' block
            } else if (itemId == R.id.nav_favorites ||
                    itemId == R.id.nav_about) {
                Toast.makeText(MainActivity.this, item.getTitle() + " - Coming Soon!", Toast.LENGTH_SHORT).show();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

    }

    // --- ADD THIS NEW METHOD for the dialog ---
    private void showAddOptionsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_options);

        LinearLayout layoutNewNote = dialog.findViewById(R.id.layout_new_note);
        LinearLayout layoutNewCanvas = dialog.findViewById(R.id.layout_new_canvas);

        layoutNewNote.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
            // Pass the type to the editor
            intent.putExtra("noteType", Note.TYPE_TEXT);
            startActivity(intent);
        });

        layoutNewCanvas.setOnClickListener(v -> {
            dialog.dismiss();
            // We will create CanvasEditorActivity in the next step
            // For now, this will crash if clicked, but we will create the file soon.
            Intent intent = new Intent(MainActivity.this, CanvasEditorActivity.class);
            // Pass the type to the editor
            intent.putExtra("noteType", Note.TYPE_CANVAS);
            startActivity(intent);
        });

        // Style the dialog to appear from the bottom
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            // Use the background drawable we created
            window.setBackgroundDrawableResource(R.drawable.dialog_add_background);
            // Set gravity to bottom
            window.setGravity(Gravity.BOTTOM);
            // Remove the default dim background
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        dialog.show();
    }
    // --- END OF NEW METHOD ---


    private String getCapitalizedName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String[] words = name.split("\\s");
        StringBuilder capitalizedWord = new StringBuilder();
        for (String w : words) {
            if(w.length() > 0) {
                String first = w.substring(0, 1);
                String afterfirst = w.substring(1);
                capitalizedWord.append(first.toUpperCase()).append(afterfirst).append(" ");
            }
        }
        return capitalizedWord.toString().trim();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // --- MODIFY THIS METHOD to handle different note types ---
    @Override
    public void onItemClick(Note note) {
        Intent intent;
        // Check the note type to open the correct editor
        if (note.type != null && note.type.equals(Note.TYPE_CANVAS)) {
            // We will create CanvasEditorActivity soon
            intent = new Intent(this, CanvasEditorActivity.class);
        } else {
            // Default to text editor for old notes (where type is null) or new text notes
            intent = new Intent(this, NoteEditorActivity.class);
        }
        intent.putExtra("EXISTING_NOTE", note); // Pass the entire note object
        startActivity(intent);
    }
    // --- END OF MODIFICATION ---

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
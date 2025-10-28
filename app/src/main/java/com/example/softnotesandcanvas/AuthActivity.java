package com.example.softnotesandcanvas;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.softnotesandcanvas.databinding.ActivityAuthBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthActivity extends AppCompatActivity {

    private ActivityAuthBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Set click listeners
        binding.buttonSignIn.setOnClickListener(v -> attemptSignIn());
        binding.buttonSignUp.setOnClickListener(v -> attemptSignUp());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, go to MainActivity
            goToMainActivity();
        }
    }

    private void attemptSignIn() {
        String email = binding.editTextEmail.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();

        if (!validateForm(email, password)) {
            return;
        }

        toggleLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, go to MainActivity
                        goToMainActivity();
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(AuthActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                    toggleLoading(false);
                });
    }

    private void attemptSignUp() {
        String email = binding.editTextEmail.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();

        if (!validateForm(email, password)) {
            return;
        }

        toggleLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign up success, go to MainActivity
                        goToMainActivity();
                    } else {
                        // If sign up fails, display a message to the user.
                        Toast.makeText(AuthActivity.this, "Sign up failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                    toggleLoading(false);
                });
    }

    private boolean validateForm(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            binding.layoutEmail.setError("Email is required.");
            return false;
        } else {
            binding.layoutEmail.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            binding.layoutPassword.setError("Password is required.");
            return false;
        } else {
            binding.layoutPassword.setError(null);
        }

        if (password.length() < 6) {
            binding.layoutPassword.setError("Password must be at least 6 characters.");
            return false;
        } else {
            binding.layoutPassword.setError(null);
        }

        return true;
    }

    private void goToMainActivity() {
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        // Clear the activity stack so the user can't press "back" to the login screen
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close this AuthActivity
    }

    private void toggleLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.buttonSignIn.setEnabled(false);
            binding.buttonSignUp.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.buttonSignIn.setEnabled(true);
            binding.buttonSignUp.setEnabled(true);
        }
    }
}
package com.example.softnotesandcanvas;

import android.content.Intent;
import android.content.SharedPreferences;
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
    private boolean isSignUpMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        binding.buttonSignIn.setOnClickListener(v -> {
            binding.layoutName.setVisibility(View.GONE);
            binding.textViewSubtitle.setText("Sign in to your account");
            attemptSignIn();
        });

        binding.buttonSignUp.setOnClickListener(v -> {
            if (!isSignUpMode) {
                isSignUpMode = true;
                binding.layoutName.setVisibility(View.VISIBLE);
                binding.textViewSubtitle.setText("Create your account");
                binding.buttonSignUp.setText("Confirm Sign Up");
                binding.buttonSignIn.setEnabled(false);
            } else {
                attemptSignUp();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
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
                        goToMainActivity();
                    } else {
                        Toast.makeText(AuthActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                    toggleLoading(false);
                });
    }

    private void attemptSignUp() {
        String name = binding.editTextName.getText().toString().trim();
        String email = binding.editTextEmail.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            binding.layoutName.setError("Name is required.");
            return;
        } else {
            binding.layoutName.setError(null);
        }

        if (!validateForm(email, password)) {
            return;
        }

        toggleLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                        prefs.edit().putString("user_name", name).apply();
                        goToMainActivity();
                    } else {
                        Toast.makeText(AuthActivity.this, "Sign up failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                    toggleLoading(false);
                });
    }

    private boolean validateForm(String email, String password) {
        boolean valid = true;

        if (TextUtils.isEmpty(email)) {
            binding.layoutEmail.setError("Email is required.");
            valid = false;
        } else {
            binding.layoutEmail.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            binding.layoutPassword.setError("Password is required.");
            valid = false;
        } else if (password.length() < 6) {
            binding.layoutPassword.setError("Password must be at least 6 characters.");
            valid = false;
        } else {
            binding.layoutPassword.setError(null);
        }

        return valid;
    }

    private void goToMainActivity() {
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void toggleLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.buttonSignIn.setEnabled(!isLoading);
        binding.buttonSignUp.setEnabled(!isLoading);
    }
}

package com.example.smartech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegistrationActivity extends AppCompatActivity {

    private EditText usernameEditText, firstNameEditText, lastNameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        usernameEditText = findViewById(R.id.usernameEditText);
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        addTestDataToFirestore();
        registerButton.setOnClickListener(v -> {
            if (validateInputs()) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString();

                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Save user info to Firestore
                        String userId = mAuth.getCurrentUser().getUid();
                        saveUserToFirestore(userId);

                        Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegistrationActivity.this, EmergencyContactActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        TextView loginTextView = findViewById(R.id.loginTextView);
        SpannableString spannableString = new SpannableString("Already have an account? Log in");

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(Color.BLUE);
            }
        };

        spannableString.setSpan(clickableSpan, 25, 31, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        loginTextView.setText(spannableString);
        loginTextView.setMovementMethod(LinkMovementMethod.getInstance());
        loginTextView.setHighlightColor(Color.TRANSPARENT);
    }

    private void saveUserToFirestore(String userId) {
        String username = usernameEditText.getText().toString().trim();
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();

        User user = new User(userId, username, firstName, lastName, email);

        db.collection("users").document(userId).set(user)
                .addOnSuccessListener(aVoid -> {
                    // Successfully added user data
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegistrationActivity.this, "Error saving user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateInputs() {
        String username = usernameEditText.getText().toString().trim();
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (username.isEmpty()) {
            usernameEditText.setError("Username is required");
            return false;
        }

        if (firstName.length() < 2) {
            firstNameEditText.setError("First name must be at least 2 letters");
            return false;
        }

        if (lastName.length() < 2) {
            lastNameEditText.setError("Last name must be at least 2 letters");
            return false;
        }

        if (!email.contains("@")) {
            emailEditText.setError("Enter a valid email");
            return false;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            return false;
        }

        return true;
    }

    public static class User {
        public String userId;
        public String username;
        public String firstName;
        public String lastName;
        public String email;

        public User() {
            // Default constructor required for Firestore
        }

        public User(String userId, String username, String firstName, String lastName, String email) {
            this.userId = userId;
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }
    }
    // Example method to test adding data to Firestore
    private void addTestDataToFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create a new document in the "test_users" collection
        Map<String, Object> user = new HashMap<>();
        user.put("firstName", "John");
        user.put("lastName", "Doe");
        user.put("email", "john.doe@example.com");

        // Add the data to Firestore
        db.collection("test_users")
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    // Document was added successfully
                    Log.d("Firestore", "DocumentSnapshot successfully written with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    // Error occurred
                    Log.w("Firestore", "Error writing document", e);
                });
    }

}

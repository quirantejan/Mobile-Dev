package com.example.smartech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
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

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView incorrectPasswordTextView, invalidEmailTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        incorrectPasswordTextView = findViewById(R.id.incorrectPasswordTextView);
        invalidEmailTextView = findViewById(R.id.invalidEmailTextView); // New error message TextView

        TextView signUpTextView = findViewById(R.id.signUpTextView);

        // Make "Sign Up" clickable
        SpannableString spannableString = new SpannableString("Don't have an account? Sign Up");

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                // Navigate to Registration Activity
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(Color.BLUE);
            }
        };

        spannableString.setSpan(clickableSpan, 23, 30, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        signUpTextView.setText(spannableString);
        signUpTextView.setMovementMethod(LinkMovementMethod.getInstance());
        signUpTextView.setHighlightColor(Color.TRANSPARENT);

        // Set the login button click listener
        loginButton.setOnClickListener(v -> validateLogin());
    }

    private void validateLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Check if email is empty
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill out both fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if email is valid (contains '@')
        if (!email.contains("@")) {
            invalidEmailTextView.setVisibility(View.VISIBLE); // Show error message
            return;
        } else {
            invalidEmailTextView.setVisibility(View.GONE); // Hide error message if email is valid
        }

        // Simulating email and password check (replace this with your actual validation logic)
        if (!email.equals("user@example.com")) {
            // Email does not match any account
            Toast.makeText(this, "No account found with this email.", Toast.LENGTH_SHORT).show();
        } else {
            // Check if the password is correct
            if (!password.equals("password123")) {  // Replace with actual password validation logic
                incorrectPasswordTextView.setVisibility(View.VISIBLE); // Show the incorrect password message
            } else {
                // Password is correct, proceed to the next activity
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class); // MainActivity is your next screen
                startActivity(intent);
                finish();
            }
        }
    }
}

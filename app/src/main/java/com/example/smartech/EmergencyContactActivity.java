package com.example.smartech;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EmergencyContactActivity extends AppCompatActivity {

    private static final int MAX_CONTACTS = 5;
    private int contactCount = 1;

    private LinearLayout emergencyContactsContainer;
    private Button addContactButton;
    private Button doneButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contact);

        // Initialize UI components
        emergencyContactsContainer = findViewById(R.id.emergencyContactsContainer);
        addContactButton = findViewById(R.id.addContactButton);
        doneButton = findViewById(R.id.doneButton);

        // Initial emergency contact field
        addInitialContactField();

        // Add another contact button click listener
        addContactButton.setOnClickListener(v -> {
            if (validateLastContact() && contactCount < MAX_CONTACTS) {
                addNewContactField();
            } else if (contactCount >= MAX_CONTACTS) {
                Toast.makeText(this, "Maximum of 5 contacts allowed", Toast.LENGTH_SHORT).show();
            }
        });

        // Done button click listener
        doneButton.setOnClickListener(v -> {
            // Navigate to the Home Activity
            Intent intent = new Intent(EmergencyContactActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();  // Optional, if you want to close this activity and not allow back navigation
        });
    }

    private void addInitialContactField() {
        EditText nameEditText = new EditText(this);
        nameEditText.setHint("Contact Name");
        nameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        nameEditText.setBackground(getDrawable(android.R.drawable.edit_text));
        nameEditText.setPadding(12, 12, 12, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, 8);
        nameEditText.setLayoutParams(lp);

        EditText emailEditText = new EditText(this);
        emailEditText.setHint("Contact Email");
        emailEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailEditText.setBackground(getDrawable(android.R.drawable.edit_text));
        emailEditText.setPadding(12, 12, 12, 12);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp2.setMargins(0, 0, 0, 16);
        emailEditText.setLayoutParams(lp2);

        emergencyContactsContainer.addView(nameEditText);
        emergencyContactsContainer.addView(emailEditText);
    }

    private boolean validateLastContact() {
        View nameView = emergencyContactsContainer.getChildAt(emergencyContactsContainer.getChildCount() - 2);
        View emailView = emergencyContactsContainer.getChildAt(emergencyContactsContainer.getChildCount() - 1);

        EditText nameField = (EditText) nameView;
        EditText emailField = (EditText) emailView;

        String name = nameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();

        if (!name.matches("^[A-Za-z]{2,}\\s[A-Za-z]{2,}$")) {
            nameField.setError("Enter first and last name with at least 2 letters each");
            return false;
        }

        if (!email.contains("@")) {
            emailField.setError("Enter a valid email");
            return false;
        }

        return true;
    }

    private void addNewContactField() {
        contactCount++;

        EditText nameEditText = new EditText(this);
        nameEditText.setHint("Contact Name");
        nameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        nameEditText.setBackground(getDrawable(android.R.drawable.edit_text));
        nameEditText.setPadding(12, 12, 12, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, 8);
        nameEditText.setLayoutParams(lp);

        EditText emailEditText = new EditText(this);
        emailEditText.setHint("Contact Email");
        emailEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailEditText.setBackground(getDrawable(android.R.drawable.edit_text));
        emailEditText.setPadding(12, 12, 12, 12);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp2.setMargins(0, 0, 0, 16);
        emailEditText.setLayoutParams(lp2);

        emergencyContactsContainer.addView(nameEditText);
        emergencyContactsContainer.addView(emailEditText);
    }
}

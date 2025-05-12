package com.example.smartech;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EmergencyContactActivity extends AppCompatActivity {

    private static final int MAX_CONTACTS = 5;
    private int contactCount = 1;

    private LinearLayout emergencyContactsContainer;
    private Button addContactButton;
    private Button removeContactButton;
    private Button doneButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contact);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emergencyContactsContainer = findViewById(R.id.emergencyContactsContainer);
        addContactButton = findViewById(R.id.addContactButton);
        removeContactButton = findViewById(R.id.removeContactButton);
        doneButton = findViewById(R.id.doneButton);

        // Initial contact field
        addInitialContactField();

        // Button to add a new contact field
        addContactButton.setOnClickListener(v -> {
            if (validateLastContact() && contactCount < MAX_CONTACTS) {
                addNewContactField();
            } else if (contactCount >= MAX_CONTACTS) {
                Toast.makeText(this, "Maximum of 5 contacts allowed", Toast.LENGTH_SHORT).show();
            }
        });

        // Button to save all contacts to Firestore
        doneButton.setOnClickListener(v -> {
            if (validateLastContact()) {
                saveContactsToFirestore();
            }
        });

        // Button to remove the most recent contact
        removeContactButton.setOnClickListener(v -> {
            if (contactCount > 1) {
                removeLastContactField();
            }
        });

        // Disable remove button when there's only one contact
        removeContactButton.setEnabled(false);
        removeContactButton.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
    }

    private void addInitialContactField() {
        addContactField();
    }

    private void addNewContactField() {
        contactCount++;
        addContactField();
        updateRemoveButtonState();
    }

    private void addContactField() {
        // Add number label
        TextView numberLabel = new TextView(this);
        numberLabel.setText(String.valueOf(contactCount));
        numberLabel.setTextSize(16);
        numberLabel.setTextColor(getColor(R.color.black)); // Change to your desired color
        numberLabel.setPadding(0, 8, 0, 4);
        emergencyContactsContainer.addView(numberLabel);

        // Name row with first and last name
        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        nameRow.setPadding(0, 0, 0, 8);
        nameRow.setWeightSum(2);

        EditText firstName = new EditText(this);
        firstName.setHint("First Name");
        firstName.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        firstName.setBackground(getDrawable(android.R.drawable.edit_text));
        firstName.setPadding(12, 12, 12, 12);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp1.setMargins(0, 0, 8, 0);
        firstName.setLayoutParams(lp1);

        EditText lastName = new EditText(this);
        lastName.setHint("Last Name");
        lastName.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        lastName.setBackground(getDrawable(android.R.drawable.edit_text));
        lastName.setPadding(12, 12, 12, 12);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp2.setMargins(8, 0, 0, 0);
        lastName.setLayoutParams(lp2);

        nameRow.addView(firstName);
        nameRow.addView(lastName);

        // Email input
        EditText emailEditText = new EditText(this);
        emailEditText.setHint("Contact Email");
        emailEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailEditText.setBackground(getDrawable(android.R.drawable.edit_text));
        emailEditText.setPadding(12, 12, 12, 12);
        LinearLayout.LayoutParams lpEmail = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lpEmail.setMargins(0, 8, 0, 16);
        emailEditText.setLayoutParams(lpEmail);

        // Add to the container
        emergencyContactsContainer.addView(nameRow);
        emergencyContactsContainer.addView(emailEditText);
    }

    private boolean validateLastContact() {
        int childCount = emergencyContactsContainer.getChildCount();
        if (childCount < 2) return false;

        View nameRowView = emergencyContactsContainer.getChildAt(childCount - 2);
        View emailView = emergencyContactsContainer.getChildAt(childCount - 1);

        if (!(nameRowView instanceof LinearLayout)) return false;

        LinearLayout nameRow = (LinearLayout) nameRowView;
        if (nameRow.getChildCount() < 2) return false;

        EditText firstName = (EditText) nameRow.getChildAt(0);
        EditText secondName = (EditText) nameRow.getChildAt(1);
        EditText email = (EditText) emailView;

        String first = firstName.getText().toString().trim();
        String second = secondName.getText().toString().trim();
        String emailStr = email.getText().toString().trim();

        if (!first.matches("^[A-Za-z]{2,}$")) {
            firstName.setError("At least 2 letters");
            return false;
        }

        if (!second.matches("^[A-Za-z]{2,}$")) {
            secondName.setError("At least 2 letters");
            return false;
        }

        if (!emailStr.contains("@")) {
            email.setError("Enter a valid email");
            return false;
        }

        return true;
    }

    private void saveContactsToFirestore() {
        String userId = mAuth.getCurrentUser().getUid();
        ArrayList<Map<String, String>> contactsList = new ArrayList<>();

        int childIndex = 0;

        for (int i = 0; i < contactCount; i++) {
            // Skip number label (TextView)
            childIndex++; // this skips the label

            if (childIndex + 1 >= emergencyContactsContainer.getChildCount()) break;

            View nameRowView = emergencyContactsContainer.getChildAt(childIndex);
            View emailView = emergencyContactsContainer.getChildAt(childIndex + 1);

            if (!(nameRowView instanceof LinearLayout) || !(emailView instanceof EditText)) {
                childIndex += 2;
                continue;
            }

            LinearLayout nameRow = (LinearLayout) nameRowView;
            EditText firstName = (EditText) nameRow.getChildAt(0);
            EditText lastName = (EditText) nameRow.getChildAt(1);
            EditText email = (EditText) emailView;

            Map<String, String> contact = new HashMap<>();
            contact.put("firstName", firstName.getText().toString().trim());
            contact.put("lastName", lastName.getText().toString().trim());
            contact.put("email", email.getText().toString().trim());

            contactsList.add(contact);

            childIndex += 2;
        }

        db.collection("users").document(userId).update("emergencyContacts", contactsList)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EmergencyContactActivity.this, "Contacts saved successfully!", Toast.LENGTH_SHORT).show();
                    // Navigate to LoginActivity instead of HomeActivity
                    startActivity(new Intent(EmergencyContactActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EmergencyContactActivity.this, "Error saving contacts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void removeLastContactField() {
        if (contactCount <= 1) return;

        // Remove the number label, name row, and email
        for (int i = 0; i < 3; i++) {
            emergencyContactsContainer.removeViewAt(emergencyContactsContainer.getChildCount() - 1);
        }

        contactCount--;
        updateRemoveButtonState();
    }

    private void updateRemoveButtonState() {
        if (contactCount <= 1) {
            removeContactButton.setEnabled(false);
            removeContactButton.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
        } else {
            removeContactButton.setEnabled(true);
            removeContactButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2D4FB2"))); // Use hex color code
            // Replace with your primary color
        }
    }
}

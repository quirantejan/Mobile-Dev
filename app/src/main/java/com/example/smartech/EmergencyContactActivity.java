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
        doneButton = findViewById(R.id.doneButton);

        addInitialContactField();

        addContactButton.setOnClickListener(v -> {
            if (validateLastContact() && contactCount < MAX_CONTACTS) {
                addNewContactField();
            } else if (contactCount >= MAX_CONTACTS) {
                Toast.makeText(this, "Maximum of 5 contacts allowed", Toast.LENGTH_SHORT).show();
            }
        });

        doneButton.setOnClickListener(v -> {
            if (validateLastContact()) {
                saveContactsToFirestore();
            }
        });
    }

    private void addInitialContactField() {
        addContactField();
    }

    private void addNewContactField() {
        contactCount++;
        addContactField();
    }

    private void addContactField() {
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

        for (int i = 0; i < contactCount; i++) {
            LinearLayout nameRow = (LinearLayout) emergencyContactsContainer.getChildAt(i * 2);
            EditText firstName = (EditText) nameRow.getChildAt(0);
            EditText secondName = (EditText) nameRow.getChildAt(1);
            EditText email = (EditText) emergencyContactsContainer.getChildAt(i * 2 + 1);

            Map<String, String> contact = new HashMap<>();
            contact.put("firstName", firstName.getText().toString().trim());
            contact.put("lastName", secondName.getText().toString().trim());
            contact.put("email", email.getText().toString().trim());

            contactsList.add(contact);
        }

        db.collection("users").document(userId).update("emergencyContacts", contactsList)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EmergencyContactActivity.this, "Contacts saved successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(EmergencyContactActivity.this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EmergencyContactActivity.this, "Error saving contacts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

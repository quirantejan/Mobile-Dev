package com.example.smartech;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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
import java.util.Locale;
import java.util.Map;

public class EmergencyContactActivity extends AppCompatActivity {

    private static final int MAX_CONTACTS = 5;
    private int contactCount = 1;

    private LinearLayout emergencyContactsContainer; // For non-inputted contacts
    private LinearLayout inputtedContactsContainer; // For inputted contacts
    private Button addContactButton;
    private Button removeContactButton;
    private Button doneButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextToSpeech tts;

    private ArrayList<Map<String, String>> validatedContacts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contact);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emergencyContactsContainer = findViewById(R.id.emergencyContactsContainer);
        inputtedContactsContainer = findViewById(R.id.inputtedContactsContainer);
        addContactButton = findViewById(R.id.addContactButton);
        removeContactButton = findViewById(R.id.removeContactButton);
        doneButton = findViewById(R.id.doneButton);

        // Initialize TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
        });

        // Initial contact field
        addInitialContactField();

        // Button to add a new contact field
        addContactButton.setOnClickListener(v -> {
            if (validateLastContact()) {
                moveLastContactToInputted();
                if (contactCount + validatedContacts.size() < MAX_CONTACTS) {
                    addNewContactField();
                } else {
                    Toast.makeText(this, "Maximum of 5 contacts allowed", Toast.LENGTH_SHORT).show();
                    speak("You can only add up to five emergency contacts");
                }
            }
        });

        // Button to save all contacts to Firestore
        doneButton.setOnClickListener(v -> {
            if (validateLastContact()) {
                moveLastContactToInputted();
                saveContactsToFirestore();
            }
        });

        // Button to remove the most recent contact
        removeContactButton.setOnClickListener(v -> {
            if (contactCount > 1) {
                removeLastContactField();
            } else if (!validatedContacts.isEmpty()) {
                // Remove the last validated contact if no non-inputted contacts remain
                validatedContacts.remove(validatedContacts.size() - 1);
                updateInputtedContactsDisplay();
                updateRemoveButtonState();
            }
        });

        // Initialize remove button state
        updateRemoveButtonState();
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
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
        numberLabel.setText("Contact " + (contactCount + validatedContacts.size()));
        numberLabel.setTextSize(16);
        numberLabel.setTextColor(getColor(R.color.black));
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
        if (childCount < 3) return false;

        View nameRowView = emergencyContactsContainer.getChildAt(childCount - 2);
        View emailView = emergencyContactsContainer.getChildAt(childCount - 1);

        if (!(nameRowView instanceof LinearLayout)) return false;

        LinearLayout nameRow = (LinearLayout) nameRowView;
        if (nameRow.getChildCount() < 2) return false;

        EditText firstName = (EditText) nameRow.getChildAt(0);
        EditText lastName = (EditText) nameRow.getChildAt(1);
        EditText email = (EditText) emailView;

        String first = firstName.getText().toString().trim();
        String last = lastName.getText().toString().trim();
        String emailStr = email.getText().toString().trim();

        if (!first.matches("^[A-Za-z]{2,}$")) {
            firstName.setError("At least 2 letters");
            speak("Please enter a valid first name.");
            return false;
        }

        if (!last.matches("^[A-Za-z]{2,}$")) {
            lastName.setError("At least 2 letters");
            speak("Please enter a valid last name.");
            return false;
        }

        if (!emailStr.contains("@")) {
            email.setError("Enter a valid email");
            speak("Please enter a valid email address.");
            return false;
        }

        return true;
    }

    private void moveLastContactToInputted() {
        int childCount = emergencyContactsContainer.getChildCount();
        if (childCount < 3) return;

        View numberLabelView = emergencyContactsContainer.getChildAt(childCount - 3);
        View nameRowView = emergencyContactsContainer.getChildAt(childCount - 2);
        View emailView = emergencyContactsContainer.getChildAt(childCount - 1);

        if (!(nameRowView instanceof LinearLayout) || !(emailView instanceof EditText)) return;

        LinearLayout nameRow = (LinearLayout) nameRowView;
        EditText firstName = (EditText) nameRow.getChildAt(0);
        EditText lastName = (EditText) nameRow.getChildAt(1);
        EditText email = (EditText) emailView;

        Map<String, String> contact = new HashMap<>();
        contact.put("firstName", firstName.getText().toString().trim());
        contact.put("lastName", lastName.getText().toString().trim());
        contact.put("email", email.getText().toString().trim());

        validatedContacts.add(contact);

        // Remove from non-inputted container
        emergencyContactsContainer.removeView(numberLabelView);
        emergencyContactsContainer.removeView(nameRowView);
        emergencyContactsContainer.removeView(emailView);
        contactCount--;

        // Update inputted contacts display
        updateInputtedContactsDisplay();

        // Update button state
        updateRemoveButtonState();
    }

    private void updateInputtedContactsDisplay() {
        inputtedContactsContainer.removeAllViews();

        if (validatedContacts.isEmpty()) {
            findViewById(R.id.inputtedContactsCard).setVisibility(View.GONE);
            return;
        }

        findViewById(R.id.inputtedContactsCard).setVisibility(View.VISIBLE);

        for (int i = 0; i < validatedContacts.size(); i++) {
            Map<String, String> contact = validatedContacts.get(i);

            // Add number label
            TextView numberLabel = new TextView(this);
            numberLabel.setText("Contact " + (i + 1));
            numberLabel.setTextSize(16);
            numberLabel.setTextColor(getColor(R.color.black));
            numberLabel.setPadding(0, 8, 0, 4);
            inputtedContactsContainer.addView(numberLabel);

            // Add name
            TextView nameText = new TextView(this);
            nameText.setText(contact.get("firstName") + " " + contact.get("lastName"));
            nameText.setTextSize(16);
            nameText.setTextColor(getColor(R.color.black));
            nameText.setPadding(0, 0, 0, 8);
            inputtedContactsContainer.addView(nameText);

            // Add email
            TextView emailText = new TextView(this);
            emailText.setText(contact.get("email"));
            emailText.setTextSize(16);
            emailText.setTextColor(getColor(R.color.black));
            emailText.setPadding(0, 0, 0, 16);
            inputtedContactsContainer.addView(emailText);
        }
    }

    private void saveContactsToFirestore() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId).update("emergencyContacts", validatedContacts)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EmergencyContactActivity.this, "Contacts saved successfully!", Toast.LENGTH_SHORT).show();
                    speak("Your emergency contacts have been saved successfully.");
                    startActivity(new Intent(EmergencyContactActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EmergencyContactActivity.this, "Error saving contacts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    speak("Failed to save emergency contacts.");
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
        if (contactCount <= 1 && validatedContacts.isEmpty()) {
            removeContactButton.setEnabled(false);
            removeContactButton.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
        } else {
            removeContactButton.setEnabled(true);
            removeContactButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2D4FB2")));
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
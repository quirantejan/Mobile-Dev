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
            Intent intent = new Intent(EmergencyContactActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
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
}

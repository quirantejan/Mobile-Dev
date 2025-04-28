package com.example.mobiledevsmartech;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class Homepage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Homepage.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        LinearLayout accountManagementSquare = findViewById(R.id.accountManagementSquare);
        accountManagementSquare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Homepage.this, AccountManagement.class);
                startActivity(intent);
            }
        });
    }
}

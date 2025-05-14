package com.example.smartech;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private VoiceAssistantHelper voiceAssistantHelper;
    private LottieAnimationView micAnimation;
    private ConstraintLayout mainLayout;
    private TextToSpeech textToSpeech;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String customName = null;  // Added to store the corrected name
    private String firstName = "";  // Variable to store first name, adjust if needed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        micAnimation = findViewById(R.id.micAnimation);
        mainLayout = findViewById(R.id.main);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Retrieve custom name from IntroductionActivity
        customName = getIntent().getStringExtra("customName");

        // Retrieve first name (you may have this from a prior activity or Firebase)
        firstName = getIntent().getStringExtra("firstName");  // Adjust this to how you store the first name

        // If custom name isn't passed, fetch it from Firebase
        if (customName == null) {
            fetchCustomNameFromFirebase();
        }

        // Check for audio recording permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }

        // Initialize TextToSpeech engine
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int langResult = textToSpeech.setLanguage(Locale.US);
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(HomeActivity.this, "Language not supported", Toast.LENGTH_SHORT).show();
                }
                // Automatically greet the user with their name
                greetUser();
            } else {
                Toast.makeText(HomeActivity.this, "TextToSpeech initialization failed", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize VoiceAssistantHelper
        voiceAssistantHelper = new VoiceAssistantHelper(this, new VoiceAssistantHelper.Listener() {
            @Override
            public void onCommandReceived(String command) {
                routeCommand(command);  // Process the command
            }

            @Override
            public void onListeningStarted() {
                micAnimation.playAnimation();
            }

            @Override
            public void onListeningStopped() {
                micAnimation.pauseAnimation();
                micAnimation.setProgress(0);
            }
        });

        // Set up mic animation trigger
        mainLayout.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    vibrate();
                    voiceAssistantHelper.startListening();  // Start listening on touch down
                    break;
                case MotionEvent.ACTION_UP:
                    voiceAssistantHelper.stopListening();  // Stop listening on touch release
                    break;
            }
            return true;
        });
    }

    // Method to make the device vibrate
    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(100);
            }
        }
    }

    // Method to greet the user automatically with their corrected name or firstName if no corrected name
    private void greetUser() {
        String greetingMessage = "Hello, " + (customName != null ? customName : firstName) + "! What can I do for you?";
        speakOut(greetingMessage);
    }

    // Method to route and handle the received commands
    private void routeCommand(String command) {
        command = command.toLowerCase();

        // Command to open the DailyPlannerActivity
        if (command.contains("daily planner")) {
            startActivity(new Intent(this, DailyPlannerActivity.class));
            speakOut("Opening your daily planner.");
        }
        // Command to open ObjectRecognitionActivity
        else if (command.contains("object recognition")) {
            startActivity(new Intent(this, ObjectRecognitionActivity.class));
            speakOut("Opening object recognition.");
        }
        // Command to open EmergencyActivity
        else if (command.contains("emergency")) {
            startActivity(new Intent(this, EmergencyActivity.class));
            speakOut("Opening emergency features.");
        }
        // Command to get emergency contacts
        else if (command.contains("who are my contacts")) {
            getEmergencyContacts();
        }
        // Command to say the user's name
        else if (command.contains("what's my name") || command.contains("what is my name")) {
            speakOut("Your name is " + (customName != null ? customName : firstName));
        }
    }

    // Fetch and speak out emergency contacts
    private void getEmergencyContacts() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("emergencyContacts")) {
                    ArrayList<Map<String, String>> emergencyContacts = (ArrayList<Map<String, String>>) documentSnapshot.get("emergencyContacts");
                    StringBuilder response = new StringBuilder();

                    response.append("You have ").append(emergencyContacts.size()).append(" emergency contacts. ");

                    for (int i = 0; i < emergencyContacts.size(); i++) {
                        Map<String, String> contact = emergencyContacts.get(i);
                        response.append(contact.get("firstName")).append(" ").append(contact.get("lastName"));
                        if (i < emergencyContacts.size() - 1) response.append(", ");
                    }
                    speakOut(response.toString());
                } else {
                    speakOut("No emergency contacts found.");
                }
            }
        });
    }

    // Method to fetch custom name from Firebase if not passed from IntroductionActivity
    private void fetchCustomNameFromFirebase() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("customName")) {
                    customName = documentSnapshot.getString("customName");
                }
                greetUser();
            }
        });
    }

    private void speakOut(String text) {
        if (customName != null) {
            text = text.replace("user", customName);
        }
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission to record audio is required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

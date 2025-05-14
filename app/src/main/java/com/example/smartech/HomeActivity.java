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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        micAnimation = findViewById(R.id.micAnimation);
        mainLayout = findViewById(R.id.main);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
                        String firstName = contact.get("firstName");
                        String lastName = contact.get("lastName");
                        response.append(i + 1).append(" is ").append(firstName).append(" ").append(lastName);
                        if (i < emergencyContacts.size() - 1) {
                            response.append(", ");
                        }
                    }

                    speakOut(response.toString());
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(HomeActivity.this, "Error fetching emergency contacts", Toast.LENGTH_SHORT).show();
        });
    }

    // Method to speak out text using TTS
    private void speakOut(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // Handle permissions result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission denied to record audio", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Cleanup TextToSpeech on destroy
    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}

package com.example.smartech;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;
import java.util.Locale;

public class HelpActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private VoiceAssistantHelper voiceAssistantHelper;
    private LottieAnimationView micAnimation;
    private TextView recognizedText;
    private ConstraintLayout mainLayout;
    private TextToSpeech textToSpeech;
    private FirebaseAuth mAuth;
    private DatabaseReference db;

    // State variables for help flow
    private boolean awaitingContactConfirmation = false;
    private boolean awaitingMessage = false;
    private String selectedContactName = "";
    private String selectedContactEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        initializeViews();
        checkPermissions();
        initializeFirebase();
        setupTextToSpeech();
        setupVoiceAssistant();
        setupTouchListener();
    }

    private void initializeViews() {
        micAnimation = findViewById(R.id.micAnimation);
        recognizedText = findViewById(R.id.recognizedText);
        mainLayout = findViewById(R.id.main);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupVoiceAssistant() {
        voiceAssistantHelper = new VoiceAssistantHelper(this, new VoiceAssistantHelper.Listener() {
            @Override
            public void onCommandReceived(String command) {
                recognizedText.setText(command);
                handleHelpCommand(command.toLowerCase());
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
    }

    private void setupTouchListener() {
        mainLayout.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    vibrate();
                    voiceAssistantHelper.startListening();
                    break;
                case MotionEvent.ACTION_UP:
                    voiceAssistantHelper.stopListening();
                    break;
            }
            return true;
        });
    }

    private void handleHelpCommand(String command) {
        if (command.contains("back to home") || command.contains("homepage") || command.contains("home")) {
            navigateToHome();
        } else if (command.startsWith("send help to ")) {
            String contactName = command.substring("send help to ".length()).trim();
            searchContact(contactName);
        } else if (awaitingContactConfirmation && (command.contains("yes") || command.contains("confirm"))) {
            awaitingContactConfirmation = false;
            awaitingMessage = true;
            speakOut("Contact confirmed. Please speak your message now.");
        } else if (awaitingMessage) {
            sendHelpMessage(command);
        } else {
            speakOut("I didn't understand that command. Please try again.");
        }
    }

    private void searchContact(String contactName) {
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference contactsRef = db.child("users").child(userId).child("contacts");

        Query query = contactsRef.orderByChild("name").equalTo(contactName);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot contactSnapshot : dataSnapshot.getChildren()) {
                        selectedContactName = contactSnapshot.child("name").getValue(String.class);
                        selectedContactEmail = contactSnapshot.child("email").getValue(String.class);

                        speakOut("Contact found. Is this the correct contact? Say yes or no.");
                        awaitingContactConfirmation = true;
                        break;
                    }
                } else {
                    speakOut("Contact not found. Please try again.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                speakOut("Error searching for contact. Please try again.");
            }
        });
    }

    private void sendHelpMessage(String message) {
        awaitingMessage = false;

        try {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:" + selectedContactEmail));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Emergency Help Request");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "HELP NEEDED: " + message);
            startActivity(Intent.createChooser(emailIntent, "Send email via..."));

            speakOut("Help message sent to " + selectedContactName);
        } catch (Exception e) {
            speakOut("Failed to send email. Please try again.");
        }
    }

    private void navigateToHome() {
        speakOut("Going back to home.");
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("isReturning", true);
        startActivity(intent);
        finish();
    }

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

    private void speakOut(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone permission required for voice commands", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}

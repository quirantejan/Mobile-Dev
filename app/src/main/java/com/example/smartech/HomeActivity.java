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
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private VoiceAssistantHelper voiceAssistantHelper;
    private LottieAnimationView micAnimation;
    private ConstraintLayout mainLayout;
    private TextToSpeech textToSpeech;
    private FirebaseAuth mAuth;
    private TextView recognizedText;
    private FirebaseFirestore db;
    private String customName = null;
    private String firstName = "";
    private VoiceCommandRouter voiceCommandRouter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        micAnimation = findViewById(R.id.micAnimation);
        recognizedText = findViewById(R.id.recognizedText);
        mainLayout = findViewById(R.id.main);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        firstName = getIntent().getStringExtra("firstName");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int langResult = textToSpeech.setLanguage(Locale.US);
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(HomeActivity.this, "Language not supported", Toast.LENGTH_SHORT).show();
                }
                fetchCustomNameFromFirebase();
            } else {
                Toast.makeText(HomeActivity.this, "TextToSpeech initialization failed", Toast.LENGTH_SHORT).show();
            }
        });

        voiceCommandRouter = new VoiceCommandRouter(this, mAuth, db, textToSpeech);

        voiceAssistantHelper = new VoiceAssistantHelper(this, new VoiceAssistantHelper.Listener() {
            @Override
            public void onCommandReceived(String command) {
                recognizedText.setText(command);
                voiceCommandRouter.routeCommand(command, customName != null ? customName : firstName);
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

    private void fetchCustomNameFromFirebase() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("customName")) {
                    customName = documentSnapshot.getString("customName");
                }
                if (documentSnapshot.contains("firstName")) {
                    firstName = documentSnapshot.getString("firstName");
                }
                greetUser();
            }
        });
    }

    private void greetUser() {
        String greetingMessage = "Hello, " + (customName != null ? customName : firstName) + "! What can I do for you?";
        textToSpeech.speak(greetingMessage, TextToSpeech.QUEUE_FLUSH, null, null);
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

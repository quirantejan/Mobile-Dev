package com.example.smartech;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.Locale;

public class IntroductionActivity extends AppCompatActivity {

    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private GestureDetector gestureDetector;
    private ConstraintLayout mainLayout;

    private String firstName;
    private String lastName;
    private String customName = null;
    private boolean awaitingNameCorrection = false;
    private boolean nameConfirmed = false;
    private boolean listeningEnabled = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduction);

        mainLayout = findViewById(R.id.main);
        firstName = getIntent().getStringExtra("firstName");
        lastName = getIntent().getStringExtra("lastName");

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                speak("Welcome to Smart Tech, " + firstName + "! Did I pronounce your name correctly? Swipe up for yes, swipe down for no.");
            }
        });

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffY) > Math.abs(e2.getX() - e1.getX()) &&
                        Math.abs(diffY) > SWIPE_THRESHOLD &&
                        Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY < 0) {
                        onSwipeUp();
                    } else {
                        onSwipeDown();
                    }
                    return true;
                }
                return false;
            }
        });

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {
                Toast.makeText(IntroductionActivity.this, "Error in speech recognition", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                List<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    customName = matches.get(0);
                    listeningEnabled = false;
                    speak(customName, () -> speak("Did I get it right this time? Swipe up for yes, swipe down for no."));
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        mainLayout.setOnTouchListener((v, event) -> {
            if (listeningEnabled) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        vibrate();
                        startVoiceInput();
                        break;
                    case MotionEvent.ACTION_UP:
                        speechRecognizer.stopListening();
                        break;
                }
                return true;
            } else {
                return gestureDetector.onTouchEvent(event);
            }
        });
    }

    private void onSwipeUp() {
        vibrate();
        if (awaitingNameCorrection && customName != null) {
            speak("Perfect! Thanks, " + customName + ". I’ll remember that.");
            nameConfirmed = true;
            updateFirebaseName(customName);
            redirectAfterDelay();
        } else if (!awaitingNameCorrection) {
            customName = firstName; 
            speak("Thank you, " + firstName + "!");
            nameConfirmed = true;
            redirectAfterDelay();
        }
    }


    private void onSwipeDown() {
        vibrate();
        if (!awaitingNameCorrection) {
            awaitingNameCorrection = true;
            speak("Could you please say your name for me? Tap and hold on the screen to speak." );
            listeningEnabled = true;
        } else {
            speak("Let's try again. Could you please say your name?");
            listeningEnabled = true;
        }
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizer.startListening(intent);
    }

    private void speak(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void speak(String text, Runnable onDone) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID");
        tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {}
            @Override public void onError(String utteranceId) {}
            @Override public void onDone(String utteranceId) {
                runOnUiThread(onDone);
            }
        });
    }

    private void redirectAfterDelay() {
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(IntroductionActivity.this, HomeActivity.class);
            intent.putExtra("customName", customName);
            // Pass corrected name to HomeActivity
            startActivity(intent);
            finish();
        }, 3000);
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

    private void updateFirebaseName(String name) {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).update("customName", name)
                .addOnSuccessListener(aVoid -> Toast.makeText(IntroductionActivity.this, "Name updated successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(IntroductionActivity.this, "Error updating name", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }
}



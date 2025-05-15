package com.example.smartech;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EmergencyActivity extends AppCompatActivity implements SensorEventListener {

    private static final int REQUEST_PERMISSIONS = 1;
    private VoiceAssistantHelper voiceAssistantHelper;
    private LottieAnimationView micAnimation;
    private TextView recognizedText;
    private ConstraintLayout mainLayout;
    private TextToSpeech textToSpeech;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastX, lastY, lastZ;
    private long lastShakeTime = 0;
    private static final int SHAKE_THRESHOLD = 800;

    private LocationManager locationManager;
    private String currentLocation = "Location not available";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        micAnimation = findViewById(R.id.micAnimation);
        recognizedText = findViewById(R.id.recognizedText);
        mainLayout = findViewById(R.id.main);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS);
        }

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int langResult = textToSpeech.setLanguage(Locale.US);
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(EmergencyActivity.this, "Language not supported", Toast.LENGTH_SHORT).show();
                } else {
                    speakOut("Emergency feature opened.");
                }
            } else {
                Toast.makeText(EmergencyActivity.this, "TextToSpeech initialization failed", Toast.LENGTH_SHORT).show();
            }
        });

        voiceAssistantHelper = new VoiceAssistantHelper(this, new VoiceAssistantHelper.Listener() {
            @Override
            public void onCommandReceived(String command) {
                recognizedText.setText(command);
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
                case android.view.MotionEvent.ACTION_DOWN:
                    vibrate();
                    voiceAssistantHelper.startListening();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                    voiceAssistantHelper.stopListening();
                    break;
            }
            return true;
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            currentLocation = "Lat: " + location.getLatitude() + ", Long: " + location.getLongitude();
        }
    };

    private void speakOut(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        long currentTime = System.currentTimeMillis();

        if ((currentTime - lastShakeTime) > 1000) {
            long diffTime = currentTime - lastShakeTime;
            lastShakeTime = currentTime;

            float speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000;

            if (speed > SHAKE_THRESHOLD) {
                speakOut("Shake detected. Sending emergency alert to your contacts.");
                triggerEmergencyProtocol();
            }

            lastX = x;
            lastY = y;
            lastZ = z;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                }
            } else {
                // Permission denied
                Toast.makeText(this, "Location permission is required for emergency alerts", Toast.LENGTH_LONG).show();
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

    private void triggerEmergencyProtocol() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<Map<String, String>> contacts = (List<Map<String, String>>) documentSnapshot.get("emergencyContacts");
                        if (contacts != null) {
                            for (Map<String, String> contact : contacts) {
                                String name = contact.get("firstName") + " " + contact.get("lastName");
                                String email = contact.get("email");
                                sendEmailToContact(name, email, currentLocation);
                            }
                        } else {
                            speakOut("No emergency contacts found.");
                        }
                    } else {
                        speakOut("User data not found.");
                    }
                })
                .addOnFailureListener(e -> speakOut("Failed to retrieve contacts: " + e.getMessage()));
    }

    private void sendEmailToContact(String name, String email, String location) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + email));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "EMERGENCY ALERT");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "This is an emergency alert from your contact. Current location: " + location);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email to " + name));
        } catch (Exception e) {
            speakOut("Could not send email to " + name);
        }
    }
}

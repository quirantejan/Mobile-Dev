package com.example.smartech;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DailyPlannerActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    private VoiceAssistantHelper voiceAssistantHelper;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LottieAnimationView micAnimation;
    private TextView recognizedText;
    private ConstraintLayout mainLayout;

    private final Map<Integer, String> taskIdMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_planner);

        initializeComponents();

        checkAudioPermission();

        setupVoiceAssistant();

        setupTouchListeners();

        fetchTasks();
    }

    private void initializeComponents() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        micAnimation = findViewById(R.id.micAnimation);
        recognizedText = findViewById(R.id.recognizedText);
        mainLayout = findViewById(R.id.main);
    }

    private void checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    private void setupVoiceAssistant() {
        voiceAssistantHelper = new VoiceAssistantHelper(this, new VoiceAssistantHelper.Listener() {
            @Override
            public void onCommandReceived(String command) {
                handleVoiceCommand(command);
            }

            @Override
            public void onListeningStarted() {
                micAnimation.playAnimation();
                recognizedText.setText("Listening...");
            }

            @Override
            public void onListeningStopped() {
                micAnimation.pauseAnimation();
                micAnimation.setProgress(0);
            }
        });
    }

    private void handleVoiceCommand(String command) {
        if (command == null || command.trim().isEmpty()) return;

        recognizedText.setText("You said: " + command);
        String lowerCmd = command.toLowerCase();

        if (lowerCmd.contains("what are my tasks") || lowerCmd.contains("show my tasks") || lowerCmd.contains("list my tasks")) {
            fetchTasks();
        } else if (lowerCmd.startsWith("add task")) {
            String taskDetails = command.replaceFirst("(?i)add task", "").trim();
            if (!taskDetails.isEmpty()) {
                voiceAssistantHelper.speak("Task added: " + taskDetails);
                addNewTask(taskDetails);
            } else {
                voiceAssistantHelper.speak("Please specify a task to add.");
            }
        } else if (lowerCmd.startsWith("remove task")) {
            String numberStr = lowerCmd.replaceFirst("(?i)remove task", "").trim();
            try {
                int index = Integer.parseInt(numberStr);
                removeTaskByIndex(index);
            } catch (NumberFormatException e) {
                voiceAssistantHelper.speak("Please say a valid task number to remove.");
            }
        } else if (lowerCmd.contains("go home") || lowerCmd.contains("go back") || lowerCmd.contains("return home") || lowerCmd.contains("go back home")) {
            voiceAssistantHelper.speak("Going back to home.");
            startActivity(new Intent(DailyPlannerActivity.this, HomeActivity.class));
            finish();
        } else {
            voiceAssistantHelper.speak("Command not recognized.");
        }
    }

    private void setupTouchListeners() {
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

        recognizedText.setOnLongClickListener(v -> {
            voiceAssistantHelper.startListening();
            recognizedText.setText("Listening...");
            return true;
        });

        recognizedText.setOnClickListener(v -> {
            voiceAssistantHelper.stopListening();
            recognizedText.setText("Press and hold to speak");
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

    private void addNewTask(String taskDetails) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        Map<String, String> taskMap = new HashMap<>();
        taskMap.put("task", taskDetails);

        db.collection("daily_plans")
                .document(userId)
                .collection("tasks")
                .add(taskMap)
                .addOnSuccessListener(documentReference -> {
                    fetchTasks();
                    Toast.makeText(this, "Task added", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error adding task", Toast.LENGTH_SHORT).show());
    }

    private void fetchTasks() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("daily_plans")
                .document(userId)
                .collection("tasks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    taskIdMap.clear();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        StringBuilder tasks = new StringBuilder("Your tasks:\n");
                        int index = 1;
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            String task = document.getString("task");
                            if (task != null) {
                                tasks.append(index).append(". ").append(task).append("\n");
                                taskIdMap.put(index, document.getId());
                                index++;
                            }
                        }
                        recognizedText.setText(tasks.toString());
                        voiceAssistantHelper.speak(tasks.toString());
                    } else {
                        recognizedText.setText("No tasks found.");
                        voiceAssistantHelper.speak("You have no tasks for today.");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching tasks", Toast.LENGTH_SHORT).show();
                    voiceAssistantHelper.speak("There was an error fetching your tasks.");
                });
    }

    private void removeTaskByIndex(int index) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        String docId = taskIdMap.get(index);

        if (docId != null) {
            db.collection("daily_plans")
                    .document(userId)
                    .collection("tasks")
                    .document(docId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String removedTask = documentSnapshot.getString("task");
                        db.collection("daily_plans")
                                .document(userId)
                                .collection("tasks")
                                .document(docId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Task removed", Toast.LENGTH_SHORT).show();
                                    voiceAssistantHelper.speak("Task removed: " + removedTask);
                                    fetchTasks();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error removing task", Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error fetching task for removal", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Invalid task number", Toast.LENGTH_SHORT).show();
            voiceAssistantHelper.speak("Invalid task number.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceAssistantHelper != null) {
            voiceAssistantHelper.stopListening();
        }
    }
}

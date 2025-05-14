package com.example.smartech;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class DailyPlannerActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private LottieAnimationView micAnimation;
    private TextView recognizedText;
    private VoiceAssistantHelper voiceAssistantHelper;
    private String recognizedCommand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_planner); // Assuming this is the correct layout

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        micAnimation = findViewById(R.id.micAnimation);
        recognizedText = findViewById(R.id.recognizedText);

        voiceAssistantHelper = new VoiceAssistantHelper(this, new VoiceAssistantHelper.Listener() {
            @Override
            public void onCommandReceived(String command) {
                recognizedCommand = command;
                recognizedText.setText("You said: " + command);

                // Parse command and add task
                if (command.toLowerCase().contains("add task")) {
                    String taskDetails = command.replace("add task", "").trim();
                    if (!taskDetails.isEmpty()) {
                        addNewTask(taskDetails);
                    }
                }
            }

            @Override
            public void onListeningStarted() {
                micAnimation.playAnimation();
            }

            @Override
            public void onListeningStopped() {
                micAnimation.pauseAnimation();
            }
        });

        // Start listening when user presses and holds
        recognizedText.setOnLongClickListener(v -> {
            voiceAssistantHelper.startListening();
            recognizedText.setText("Listening...");
            return true;
        });

        // Stop listening when user releases
        recognizedText.setOnClickListener(v -> {
            voiceAssistantHelper.stopListening();
            recognizedText.setText("Press and hold to speak");
        });

        fetchTasks();
    }

    private void addNewTask(String taskDetails) {
        String userId = mAuth.getCurrentUser().getUid();

        // Store the task in Firestore under the "tasks" collection
        Map<String, String> taskMap = new HashMap<>();
        taskMap.put("task", taskDetails);

        db.collection("daily_plans")
                .document(userId)
                .collection("tasks")
                .add(taskMap)
                .addOnSuccessListener(documentReference -> {
                    fetchTasks(); // Refresh task list after adding
                    Toast.makeText(DailyPlannerActivity.this, "Task added", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(DailyPlannerActivity.this, "Error adding task", Toast.LENGTH_SHORT).show());
    }

    private void fetchTasks() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("daily_plans")
                .document(userId)
                .collection("tasks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        StringBuilder tasks = new StringBuilder("Your tasks:\n");
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            String task = document.getString("task");
                            tasks.append("- ").append(task).append("\n");
                        }
                        recognizedText.setText(tasks.toString());
                    } else {
                        recognizedText.setText("No tasks found.");
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(DailyPlannerActivity.this, "Error fetching tasks", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceAssistantHelper != null) {
            voiceAssistantHelper.stopListening();
        }
    }
}

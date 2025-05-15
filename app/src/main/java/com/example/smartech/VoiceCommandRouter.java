package com.example.smartech;

import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class VoiceCommandRouter {

    private final Context context;
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;
    private final TextToSpeech tts;

    public VoiceCommandRouter(Context context, FirebaseAuth mAuth, FirebaseFirestore db, TextToSpeech tts) {
        this.context = context;
        this.mAuth = mAuth;
        this.db = db;
        this.tts = tts;
    }

    public void routeCommand(String command, String userName) {
        command = command.toLowerCase();

        if ((command.contains("who are") || command.contains("show") || command.contains("who is") || command.contains("tell")) &&
                (command.contains("contact") || command.contains("contacts") || command.contains("emergency contact") || command.contains("emergency contacts") || command.contains("my contacts"))) {
            getEmergencyContacts();
            return;
        }

        if (matches(command, "daily", "planner") ||
                command.contains("add task") ||
                command.contains("add to planner") ||
                (command.contains("add") && command.contains("task")) ||
                (command.contains("schedule") && (command.contains("task") || command.contains("something"))) ||
                command.contains("planner") || command.contains("plan")) {

            context.startActivity(new Intent(context, DailyPlannerActivity.class));
            speak("Opening your daily planner.");
            return;
        }


        if (command.contains("object recognition") ||
                matchesAny(command, new String[]{"recognize", "detect", "identify"}, new String[]{"object", "thing", "item"})) {
            context.startActivity(new Intent(context, ObjectRecognitionActivity.class));
            speak("Opening object recognition.");
            return;
        }

        if (command.contains("help") || command.contains("assist")) {
            context.startActivity(new Intent(context, HelpActivity.class));
            speak("Opening help features.");
            return;
        }

        if (command.contains("emergency")) {
            context.startActivity(new Intent(context, EmergencyActivity.class));
            speak("Opening emergency features.");
            return;
        }

        if (matchesAny(command, new String[]{"what", "say", "tell"}, new String[]{"my name"})) {
            speak("Your name is " + userName);
            return;
        }

        speak("Sorry, I didn't understand that. Could you please repeat?");
    }

    private void getEmergencyContacts() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && documentSnapshot.contains("emergencyContacts")) {
                ArrayList<Map<String, String>> contacts = (ArrayList<Map<String, String>>) documentSnapshot.get("emergencyContacts");
                StringBuilder response = new StringBuilder();
                response.append("You have ").append(contacts.size()).append(" emergency contacts. ");
                for (int i = 0; i < contacts.size(); i++) {
                    Map<String, String> contact = contacts.get(i);
                    response.append(contact.get("firstName")).append(" ").append(contact.get("lastName"));
                    if (i < contacts.size() - 1) response.append(", ");
                }
                speak(response.toString());
            } else {
                speak("No emergency contacts found.");
            }
        });
    }

    private void speak(String message) {
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private boolean matches(String command, String... keywords) {
        for (String keyword : keywords) {
            if (!command.contains(keyword)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesAny(String command, String[] phrases, String[] topics) {
        for (String phrase : phrases) {
            for (String topic : topics) {
                if (command.contains(phrase) && command.contains(topic)) {
                    return true;
                }
            }
        }
        return false;
    }

}

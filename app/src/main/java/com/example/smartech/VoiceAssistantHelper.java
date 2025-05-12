package com.example.smartech;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceAssistantHelper {
    private final Activity activity;
    private final SpeechRecognizer speechRecognizer;
    private final Intent recognizerIntent;
    private final Listener listener;

    public VoiceAssistantHelper(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                listener.onListeningStarted();
            }

            @Override public void onBeginningOfSpeech() {}

            @Override public void onRmsChanged(float rmsdB) {}

            @Override public void onBufferReceived(byte[] buffer) {}

            @Override public void onEndOfSpeech() {
                listener.onListeningStopped();
            }

            @Override public void onError(int error) {
                listener.onListeningStopped();
                Toast.makeText(activity, "Error recognizing speech", Toast.LENGTH_SHORT).show();
            }

            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    listener.onCommandReceived(matches.get(0));
                }
                listener.onListeningStopped();
            }

            @Override public void onPartialResults(Bundle partialResults) {}

            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    public void startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(activity)) {
            speechRecognizer.startListening(recognizerIntent);
        } else {
            Toast.makeText(activity, "Speech recognition not available", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopListening() {
        speechRecognizer.stopListening();
    }

    public interface Listener {
        void onCommandReceived(String command);
        void onListeningStarted();
        void onListeningStopped();
    }
}

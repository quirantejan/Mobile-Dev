package com.example.smartech;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class TextSpeakerHelper {
    private TextToSpeech tts;
    private boolean isReady = false;

    public TextSpeakerHelper(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                isReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
                if (!isReady) {
                    Log.e("TextSpeaker", "TTS language not supported.");
                }
            } else {
                Log.e("TextSpeaker", "TTS initialization failed.");
            }
        });
    }

    public void speak(String message) {
        if (isReady && message != null && !message.isEmpty()) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}

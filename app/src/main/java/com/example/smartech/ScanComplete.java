package com.example.smartech;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;

public class ScanComplete extends AppCompatActivity {

    private LottieAnimationView lottieAnimationView;
    private TextView completionMessage;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_complete);

        lottieAnimationView = findViewById(R.id.lottie_animation);
        completionMessage = findViewById(R.id.completionMessage);

        mediaPlayer = MediaPlayer.create(this, R.raw.scancomplete);
        mediaPlayer.start();

        lottieAnimationView.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                lottieAnimationView.setVisibility(View.GONE);
                completionMessage.setVisibility(View.VISIBLE);
            }
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            Intent intent = new Intent(ScanComplete.this, RegisterVoicePasskey.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}

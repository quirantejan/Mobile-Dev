package com.example.smartech;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ObjectRecognitionActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;

    private PreviewView previewView;
    private TextView objectTextView;
    private ExecutorService cameraExecutor;
    private ObjectDetector objectDetector;
    private GestureDetectorCompat gestureDetector;
    private CameraSelector currentCameraSelector;
    private TextSpeakerHelper textSpeakerHelper;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_object_recognition);

        previewView = findViewById(R.id.previewView);
        objectTextView = findViewById(R.id.objectTextView);

        setupGestureDetector();
        setupObjectDetector();

        if (hasCameraPermission()) {
            startCamera(CameraSelector.LENS_FACING_BACK);
        } else {
            requestCameraPermission();
        }

        cameraExecutor = Executors.newSingleThreadExecutor();

        textSpeakerHelper = new TextSpeakerHelper(this);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                switchCamera();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                // Vibrate the device
                if (vibrator != null) {
                    // Check for device API level to decide vibration effect
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)); // 500 ms vibration
                    } else {
                        vibrator.vibrate(500); // For older versions
                    }
                }
                // Close the camera and go back to HomeActivity when the screen is held.
                closeCameraAndReturnHome();
            }
        });
    }

    private void setupObjectDetector() {
        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build();

        objectDetector = ObjectDetection.getClient(options);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Toast.makeText(this, "Camera access is required for object detection", Toast.LENGTH_LONG).show();
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    private void startCamera(int lensFacing) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                currentCameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();

                androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, currentCameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void switchCamera() {
        int newLensFacing = currentCameraSelector.getLensFacing() == CameraSelector.LENS_FACING_BACK
                ? CameraSelector.LENS_FACING_FRONT
                : CameraSelector.LENS_FACING_BACK;

        startCamera(newLensFacing);
    }

    private void analyzeImage(ImageProxy imageProxy) {
        if (imageProxy.getImage() != null) {
            InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

            objectDetector.process(image)
                    .addOnSuccessListener(detectedObjects -> {
                        StringBuilder resultText = new StringBuilder();

                        for (DetectedObject obj : detectedObjects) {
                            List<DetectedObject.Label> labels = obj.getLabels();
                            if (!labels.isEmpty()) {
                                for (DetectedObject.Label label : labels) {
                                    resultText.append("Detected: ").append(label.getText()).append("\n");
                                }
                            } else {
                                resultText.append("Detected an object (unlabeled).\n");
                            }
                        }

                        updateText(resultText.toString().trim());
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> {
                        updateText("Detection failed.");
                        e.printStackTrace();
                        imageProxy.close();
                    });
        } else {
            imageProxy.close();
        }
    }

    private void updateText(String text) {
        runOnUiThread(() -> {
            objectTextView.setText(text);
            textSpeakerHelper.speak(text);
        });
    }

    private void closeCameraAndReturnHome() {
        // Use ProcessCameraProvider to unbind camera
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get(); // Safely get the camera provider after the future is completed
                cameraProvider.unbindAll(); // Unbind the camera
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this)); // Use the main executor to run this code on the UI thread

        // Return to HomeActivity
        Intent intent = new Intent(ObjectRecognitionActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();  // Optionally finish the current activity
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera(CameraSelector.LENS_FACING_BACK);
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (textSpeakerHelper != null) {
            textSpeakerHelper.shutdown();
        }
    }
}

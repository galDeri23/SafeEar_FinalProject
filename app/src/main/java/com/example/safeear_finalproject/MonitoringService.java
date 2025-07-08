package com.example.safeear_finalproject;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MonitoringService extends Service {

    private static final String CHANNEL_ID = "MonitoringServiceChannel";
    private static final String TAG = "MonitoringService";

    private Handler handler;
    private Runnable recordingRunnable;
    private File outputFile;
    private AudioRecorderHelper audioRecorderHelper;

    private int lastIntervalMinutes = -1;
    private int lastDurationMinutes = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        createNotificationChannel();
        Notification notification = createNotification();
        startForeground(1, notification);
        Log.d(TAG, "Foreground notification started");

        handler = new Handler();
        audioRecorderHelper = new AudioRecorderHelper(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");

        if (!hasAudioPermission()) {
            Log.e(TAG, "Missing RECORD_AUDIO permission. Cannot proceed.");
            stopSelf();
            return START_NOT_STICKY;
        }

        updateConfigurationAndStart();

        return START_STICKY;
    }

    private boolean hasAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void updateConfigurationAndStart() {
        int currentInterval = getIntervalMinutes();
        int currentDuration = getDurationMinutes();

        // אם הערכים השתנו – תעדכן מחדש את הלולאה
        if (currentInterval != lastIntervalMinutes || currentDuration != lastDurationMinutes) {
            Log.d(TAG, "Detected settings change. Restarting loop...");

            lastIntervalMinutes = currentInterval;
            lastDurationMinutes = currentDuration;

            if (handler != null && recordingRunnable != null) {
                handler.removeCallbacks(recordingRunnable);
            }

            startRecording();
            handler.postDelayed(this::stopRecording, currentDuration * 60 * 1000L);
            startRecordingLoop(); // תתחיל לולאה חדשה
        }
    }

    private void startRecordingLoop() {
        recordingRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Recording loop triggered");

                updateConfigurationAndStart(); // בדוק אם המשתמש שינה הגדרות בזמן ריצה

                startRecording();
                handler.postDelayed(() -> stopRecording(), getDurationMinutes() * 60 * 1000L);

                long intervalMs = getIntervalMinutes() * 60L * 1000L;
                Log.d(TAG, "Next recording in: " + intervalMs + " ms");
                handler.postDelayed(this, intervalMs);
            }
        };

        long initialDelay = getIntervalMinutes() * 60L * 1000L;
        Log.d(TAG, "Initial delay before first loop: " + initialDelay + " ms");
        handler.postDelayed(recordingRunnable, initialDelay);
    }

    private int getIntervalMinutes() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int interval = prefs.getInt("recording_interval_minutes", 30); // ברירת מחדל: 30 דקות
        Log.d(TAG, "Recording interval (minutes): " + interval);
        return interval;
    }

    private int getDurationMinutes() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int duration = prefs.getInt("recording_duration", 1); // ברירת מחדל: דקה אחת
        Log.d(TAG, "Recording duration (minutes): " + duration);
        return duration;
    }

    private void startRecording() {
        try {
            outputFile = audioRecorderHelper.startRecording();
            Log.d(TAG, "Recording started using helper: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording", e);
        }
    }

    private void stopRecording() {
        try {
            audioRecorderHelper.stopRecording();
            Log.d(TAG, "Recording stopped");

            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            processRecording(outputFile, timestamp);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
        }
    }

    private void processRecording(File file, String timestamp) {
        new Thread(() -> {
            Log.d(TAG, "Processing recording file: " + file.getAbsolutePath());
            String transcript = "";
            try {
                transcript = SpeechToTextHelper.transcribeAudio(file);
            } catch (Exception e) {
                Log.e(TAG, "Transcription error", e);
            }

            if (transcript != null && !transcript.isEmpty()) {
                Log.d(TAG, "Transcription success: " + transcript);
                saveReport(timestamp, transcript);
                boolean deleted = file.delete();
                Log.d(TAG, "Deleted audio file: " + deleted);
            } else {
                Log.w(TAG, "Transcript is empty or failed");
            }
        }).start();
    }

    private void saveReport(String timestamp, String transcript) {
        SharedPreferences prefs = getSharedPreferences("Reports", MODE_PRIVATE);
        prefs.edit().putString("report_" + timestamp, transcript).apply();
        Log.d(TAG, "Report saved: " + timestamp);
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Monitoring Active")
                .setContentText("Audio monitoring running in background")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Monitoring Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        if (handler != null && recordingRunnable != null) {
            handler.removeCallbacks(recordingRunnable);
        }
        try {
            audioRecorderHelper.stopRecording();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording in onDestroy", e);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

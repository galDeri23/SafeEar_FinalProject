package com.example.safeear_finalproject;

import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.*;

public class AudioRecorderHelper {
    private static final String TAG = "AudioRecorderHelper";

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private final Context context;
    private AudioRecord recorder;
    private boolean isRecording = false;
    private Thread recordingThread;
    private File rawOutputFile;
    private File finalWavFile;

    public AudioRecorderHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public File startRecording() throws IOException {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing RECORD_AUDIO permission");
            throw new SecurityException("RECORD_AUDIO permission not granted");
        }

        recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
        );

        File dir = new File(context.getExternalFilesDir(null), "recordings");
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            Log.d(TAG, "Recording folder created: " + created);
        }

        String fileName = "recording_" + System.currentTimeMillis();
        rawOutputFile = new File(dir, fileName + ".raw");
        finalWavFile = new File(dir, fileName + ".wav");

        recorder.startRecording();
        isRecording = true;

        recordingThread = new Thread(() -> writeAudioDataToFile(bufferSize), "AudioRecorder Thread");
        recordingThread.start();

        Log.d(TAG, "Recording started: " + rawOutputFile.getAbsolutePath());
        return finalWavFile;
    }

    public void stopRecording() throws IOException {
        if (recorder != null) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;

            Log.d(TAG, "Recording stopped, converting to WAV...");
            rawToWave(rawOutputFile, finalWavFile);
            if (rawOutputFile.delete()) {
                Log.d(TAG, "Raw file deleted after conversion");
            }
        }
    }

    private void writeAudioDataToFile(int bufferSize) {
        byte[] audioData = new byte[bufferSize];

        try (FileOutputStream os = new FileOutputStream(rawOutputFile)) {
            while (isRecording) {
                int read = recorder.read(audioData, 0, bufferSize);
                if (read > 0) {
                    os.write(audioData, 0, read);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing audio data", e);
        }
    }

    private void rawToWave(File rawFile, File waveFile) throws IOException {
        byte[] rawData = new byte[(int) rawFile.length()];
        try (DataInputStream input = new DataInputStream(new FileInputStream(rawFile))) {
            input.readFully(rawData);
        }

        try (DataOutputStream output = new DataOutputStream(new FileOutputStream(waveFile))) {
            writeWaveHeader(output, rawData.length);
            output.write(rawData);
        }

        Log.d(TAG, "WAV file written: " + waveFile.getAbsolutePath());
    }

    private void writeWaveHeader(DataOutputStream out, int totalAudioLen) throws IOException {
        int totalDataLen = totalAudioLen + 36;
        int byteRate = SAMPLE_RATE * 2; // 16bit * mono

        out.writeBytes("RIFF");
        out.writeInt(Integer.reverseBytes(totalDataLen));
        out.writeBytes("WAVE");
        out.writeBytes("fmt ");
        out.writeInt(Integer.reverseBytes(16));
        out.writeShort(Short.reverseBytes((short) 1)); // PCM
        out.writeShort(Short.reverseBytes((short) 1)); // Mono
        out.writeInt(Integer.reverseBytes(SAMPLE_RATE));
        out.writeInt(Integer.reverseBytes(byteRate));
        out.writeShort(Short.reverseBytes((short) 2)); // Block align
        out.writeShort(Short.reverseBytes((short) 16)); // Bits per sample
        out.writeBytes("data");
        out.writeInt(Integer.reverseBytes(totalAudioLen));
    }

    public File getFinalWavFile() {
        return finalWavFile;
    }
}

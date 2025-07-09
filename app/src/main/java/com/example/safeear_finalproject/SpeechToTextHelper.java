package com.example.safeear_finalproject;

import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class SpeechToTextHelper {

    private static final String TAG = "SpeechToTextHelper";
    private static final String API_KEY = BuildConfig.API_KEY;

    public static String transcribeAudio(File audioFile) {
        try {
            Log.d(TAG, "Starting transcription for: " + audioFile.getAbsolutePath());
            String base64Audio = encodeFileToBase64(audioFile);

            JSONObject config = new JSONObject();
            config.put("encoding", "LINEAR16");
            config.put("sampleRateHertz", 16000);
            config.put("languageCode", "he-IL");

            JSONObject audio = new JSONObject();
            audio.put("content", base64Audio);

            JSONObject requestBody = new JSONObject();
            requestBody.put("config", config);
            requestBody.put("audio", audio);


            URL url = new URL("https://speech.googleapis.com/v1/speech:longrunningrecognize?key=" + API_KEY);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);

            OutputStream os = connection.getOutputStream();
            os.write(requestBody.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = connection.getResponseCode();
            InputStream is = (responseCode == HttpURLConnection.HTTP_OK)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String response = readStream(is);
            connection.disconnect();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String operationName = new JSONObject(response).getString("name");
                Log.d(TAG, "Operation started: " + operationName);
                return pollOperationResult(operationName);
            } else {
                Log.e(TAG, "Initial request failed: " + response);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in transcription", e);
        }

        return "";
    }

    private static String pollOperationResult(String operationName) throws IOException, JSONException, InterruptedException {
        String urlStr = "https://speech.googleapis.com/v1/operations/" + operationName + "?key=" + API_KEY;

        for (int i = 0; i < 20; i++) {
            Thread.sleep(2000);

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            InputStream is = (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            String response = readStream(is);
            conn.disconnect();

            JSONObject result = new JSONObject(response);
            if (result.optBoolean("done")) {
                Log.d(TAG, "Transcription done: " + response);
                JSONObject responseObj = result.getJSONObject("response");
                return extractTranscriptFromResponse(responseObj.toString());
            } else {
                Log.d(TAG, "Waiting for transcription to complete...");
            }
        }

        Log.w(TAG, "Polling timed out – transcription not ready");
        return "";
    }

    private static String encodeFileToBase64(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] bytes = new byte[(int) file.length()];
        fis.read(bytes);
        fis.close();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private static String extractTranscriptFromResponse(String jsonResponse) {
        try {
            JSONObject responseObj = new JSONObject(jsonResponse);
            JSONArray results = responseObj.getJSONArray("results");
            if (results.length() > 0) {
                StringBuilder transcript = new StringBuilder();
                for (int i = 0; i < results.length(); i++) {
                    JSONArray alternatives = results.getJSONObject(i).getJSONArray("alternatives");
                    if (alternatives.length() > 0) {
                        transcript.append(alternatives.getJSONObject(0).getString("transcript")).append(" ");
                    }
                }
                return transcript.toString().trim();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing transcription", e);
        }

        return "";
    }

    private static String readStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        return response.toString();
    }
}

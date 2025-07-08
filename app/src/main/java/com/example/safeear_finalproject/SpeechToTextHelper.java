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
    private static final String API_KEY = "AIzaSyDipZ0TOiOCSl6ULmjPwuZTCEg2APzCFBo"; // לשימוש זמני - לא לפרודקשן

    public static String transcribeAudio(File audioFile) {
        Log.d(TAG, "Starting transcription for: " + audioFile.getAbsolutePath());

        try {
            String base64Audio = encodeFileToBase64(audioFile);
            Log.d(TAG, "Encoded audio length: " + base64Audio.length());

            JSONObject requestBody = new JSONObject();

            // ⚠️ שימי לב להתאמת הפורמט: אם מקליטה עם AMR_NB אז encoding: "AMR"
            JSONObject config = new JSONObject();
            config.put("encoding", "LINEAR16");
            config.put("sampleRateHertz", 16000);
            config.put("languageCode", "he-IL"); // אם את רוצה עברית

            JSONObject audio = new JSONObject();
            audio.put("content", base64Audio);

            requestBody.put("config", config);
            requestBody.put("audio", audio);

            URL url = new URL("https://speech.googleapis.com/v1/speech:recognize?key=" + API_KEY);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);

            Log.d(TAG, "Sending request to Speech API...");
            OutputStream os = connection.getOutputStream();
            os.write(requestBody.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "API response code: " + responseCode);

            InputStream inputStream;
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream(); // כאן קוראים את גוף השגיאה
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();
            connection.disconnect();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return extractTranscriptFromResponse(response.toString());
            } else {
                Log.e(TAG, "API error response: " + response.toString());
            }

        } catch (IOException | JSONException e) {
            Log.e(TAG, "transcribeAudio: Exception occurred", e);
        }

        Log.w(TAG, "Returning null transcript");
        return null;
    }

    private static String encodeFileToBase64(File file) throws IOException {
        Log.d(TAG, "Encoding file to Base64: " + file.getAbsolutePath());
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
                JSONObject firstResult = results.getJSONObject(0);
                JSONArray alternatives = firstResult.getJSONArray("alternatives");
                if (alternatives.length() > 0) {
                    return alternatives.getJSONObject(0).getString("transcript");
                }
            } else {
                Log.w(TAG, "No results found in response");
            }
        } catch (JSONException e) {
            Log.e(TAG, "extractTranscriptFromResponse: JSON parsing error", e);
        }

        return "";
    }
}

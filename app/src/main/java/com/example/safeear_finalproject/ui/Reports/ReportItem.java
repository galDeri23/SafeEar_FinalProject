package com.example.safeear_finalproject.ui.Reports;

public class ReportItem {
    private String time;
    private String transcript;

    public ReportItem(String time, String transcript) {
        this.time = time;
        this.transcript = transcript;
    }

    public String getTime() {
        return time;
    }


    public String getTranscript() {
        return transcript;
    }
}
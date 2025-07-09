package com.example.safeear_finalproject.ui.Reports;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.safeear_finalproject.databinding.FragmentReportsBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ReportsFragment extends Fragment {

    private FragmentReportsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        List<ReportItem> reportItems = new ArrayList<>();

        // קריאה מה־SharedPreferences לדוחות האמיתיים
        SharedPreferences prefs = requireContext().getSharedPreferences("Reports", Context.MODE_PRIVATE);
        Map<String, ?> allReports = prefs.getAll();

        for (Map.Entry<String, ?> entry : allReports.entrySet()) {
            String timestamp = entry.getKey().replace("report_", "");
            String transcript = entry.getValue().toString();
            reportItems.add(new ReportItem(timestamp, transcript));
        }

        // מיון לפי תאריך/שעה מהחדש לישן
        Collections.sort(reportItems, new Comparator<ReportItem>() {
            @Override
            public int compare(ReportItem r1, ReportItem r2) {
                return r2.getTime().compareTo(r1.getTime());
            }
        });

        // הצגת הדוחות
        ReportAdapter adapter = new ReportAdapter(reportItems);
        binding.recyclerReport.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerReport.setAdapter(adapter);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
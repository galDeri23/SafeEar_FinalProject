package com.example.safeear_finalproject.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.safeear_finalproject.MonitoringService;
import com.example.safeear_finalproject.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private boolean isMonitoringActive = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        updateMonitoringUI();

        binding.buttonPause.setOnClickListener(v -> {
            isMonitoringActive = !isMonitoringActive;

            if (isMonitoringActive) {
                requireActivity().startService(new Intent(requireContext(), MonitoringService.class));
            } else {
                requireActivity().stopService(new Intent(requireContext(), MonitoringService.class));
            }

            updateMonitoringUI();
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (binding != null) {
            SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            int interval = prefs.getInt("recording_interval", 30);
            int duration = prefs.getInt("recording_duration", 1);
            binding.textSettingsSummary.setText("Every " + interval + " min  •  Duration: " + duration + " min");
        }
    }

    private void updateMonitoringUI() {
        if (isMonitoringActive) {
            binding.textMonitoringStatus.setText("Monitoring is ACTIVE");
            binding.buttonPause.setText("Pause Monitoring");
            binding.buttonPause.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_dark));
        } else {
            binding.textMonitoringStatus.setText("Monitoring is PAUSED");
            binding.buttonPause.setText("Resume Monitoring");
            binding.buttonPause.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_blue_dark));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

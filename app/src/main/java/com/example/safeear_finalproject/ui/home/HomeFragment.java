package com.example.safeear_finalproject.ui.home;

import android.app.ActivityManager;
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
    private boolean isMonitoringActive = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        isMonitoringActive = isMonitoringServiceRunning();
        updateMonitoringUI();

        binding.buttonPause.setOnClickListener(v -> {
            if (isMonitoringServiceRunning()) {
                requireActivity().stopService(new Intent(requireContext(), MonitoringService.class));
                isMonitoringActive = false;
            } else {
                requireActivity().startService(new Intent(requireContext(), MonitoringService.class));
                isMonitoringActive = true;
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
            binding.buttonPause.setText("PAUSE MONITORING");
            binding.buttonPause.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_dark));
        } else {
            binding.textMonitoringStatus.setText("Monitoring is PAUSED");
            binding.buttonPause.setText("RESUME MONITORING");
            binding.buttonPause.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_blue_dark));
        }
    }

    private boolean isMonitoringServiceRunning() {
        ActivityManager manager = (ActivityManager) requireActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MonitoringService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

package com.example.safeear_finalproject.ui.Settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.safeear_finalproject.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        sharedPreferences = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        // Load saved values if available
        loadSavedSettings();

        // Save button click handler
        binding.buttonSaveSettings.setOnClickListener(v -> saveSettings());

        return root;
    }

    private void loadSavedSettings() {
        int interval = sharedPreferences.getInt("recording_interval", 30);


        binding.inputRecordingInterval.setText(String.valueOf(interval));

    }

    private void saveSettings() {
        String intervalText = binding.inputRecordingInterval.getText().toString().trim();


        if (intervalText.isEmpty() ) {
            Toast.makeText(getContext(), "Please fill in both fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int interval = Integer.parseInt(intervalText);


        sharedPreferences.edit()
                .putInt("recording_interval", interval)
                .apply();

        Toast.makeText(getContext(), "Settings saved", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

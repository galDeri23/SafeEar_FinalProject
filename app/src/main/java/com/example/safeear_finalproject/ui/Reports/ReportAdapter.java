package com.example.safeear_finalproject.ui.Reports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safeear_finalproject.R;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private final List<ReportItem> reportItems;

    public ReportAdapter(List<ReportItem> reportItems) {
        this.reportItems = reportItems;
    }

    @Override
    public ReportViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_row, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ReportViewHolder holder, int position) {
        ReportItem item = reportItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return reportItems.size();
    }

    class ReportViewHolder extends RecyclerView.ViewHolder {
        private final TextView timeView;
        private final TextView textView;
        private boolean expanded = false;

        public ReportViewHolder(View itemView) {
            super(itemView);
            timeView = itemView.findViewById(R.id.text_time);
            textView = itemView.findViewById(R.id.text_transcript);

            itemView.setOnClickListener(v -> {
                expanded = !expanded;
                textView.setMaxLines(expanded ? Integer.MAX_VALUE : 3);
            });
        }

        public void bind(ReportItem item) {
            timeView.setText(item.getTime());
            textView.setText(item.getTranscript());
            textView.setMaxLines(3);  // reset on bind
        }
    }
}

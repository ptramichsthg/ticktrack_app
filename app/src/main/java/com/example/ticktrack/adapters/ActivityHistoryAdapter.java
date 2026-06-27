package com.example.ticktrack.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ticktrack.R;
import com.example.ticktrack.models.ActivityModel;
import java.util.List;

public class ActivityHistoryAdapter extends RecyclerView.Adapter<ActivityHistoryAdapter.ActivityViewHolder> {

    private List<ActivityModel> activityList;

    public ActivityHistoryAdapter(List<ActivityModel> activityList) {
        this.activityList = activityList;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity_history, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ActivityModel activity = activityList.get(position);
        holder.tvActionDescription.setText(activity.getDescription());
        holder.tvActionDate.setText(activity.getCreatedAt());
        
        if (activity.getActionType().contains("CREATED")) {
            holder.ivActionIcon.setImageResource(android.R.drawable.ic_menu_add);
        } else if (activity.getActionType().contains("REPLY")) {
            holder.ivActionIcon.setImageResource(android.R.drawable.ic_menu_send);
        } else if (activity.getActionType().contains("STATUS")) {
            holder.ivActionIcon.setImageResource(android.R.drawable.ic_menu_edit);
        } else {
            holder.ivActionIcon.setImageResource(android.R.drawable.ic_menu_recent_history);
        }
    }

    @Override
    public int getItemCount() {
        return activityList.size();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        TextView tvActionDescription, tvActionDate;
        ImageView ivActionIcon;
        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvActionDescription = itemView.findViewById(R.id.tvActionDescription);
            tvActionDate = itemView.findViewById(R.id.tvActionDate);
            ivActionIcon = itemView.findViewById(R.id.ivActionIcon);
        }
    }
}

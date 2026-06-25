package com.example.ticktrack.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ticktrack.R;
import com.example.ticktrack.models.Ticket;
import com.example.ticktrack.listeners.OnTicketClickListener;
import java.util.ArrayList;
import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.ViewHolder> {
    private List<Ticket> tickets = new ArrayList<>();
    private OnTicketClickListener listener;

    public TicketAdapter(OnTicketClickListener listener) {
        this.listener = listener;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ticket ticket = tickets.get(position);
        holder.tvCode.setText(ticket.getCode());
        holder.tvTitle.setText(ticket.getTitle());
        holder.tvPriority.setText(capitalizeFirst(ticket.getPriority()));
        holder.tvDate.setText(ticket.getCreatedAt() != null ? ticket.getCreatedAt().substring(0, 10) : "");

        // Status badge
        String status = ticket.getStatus();
        holder.tvStatus.setText(getStatusLabel(status));
        holder.tvStatus.setTextColor(getStatusColor(status));
        holder.cvStatus.setCardBackgroundColor(getStatusBackgroundColor(status));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTicketClick(ticket);
        });
    }

    @Override
    public int getItemCount() { return tickets.size(); }

    private String getStatusLabel(String status) {
        switch (status) {
            case "open": return "Buka";
            case "in_progress": return "Diproses";
            case "resolved": return "Selesai";
            case "rejected": return "Ditolak";
            default: return status;
        }
    }

    private int getStatusColor(String status) {
        switch (status) {
            case "open": return Color.parseColor("#1D4ED8"); // Blue
            case "in_progress": return Color.parseColor("#B45309"); // Orange
            case "resolved": return Color.parseColor("#047857"); // Green
            case "rejected": return Color.parseColor("#B91C1C"); // Red
            default: return Color.GRAY;
        }
    }

    private int getStatusBackgroundColor(String status) {
        switch (status) {
            case "open": return Color.parseColor("#DBEAFE"); // Light Blue
            case "in_progress": return Color.parseColor("#FEF3C7"); // Light Orange
            case "resolved": return Color.parseColor("#D1FAE5"); // Light Green
            case "rejected": return Color.parseColor("#FEE2E2"); // Light Red
            default: return Color.parseColor("#F3F4F6");
        }
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode, tvTitle, tvStatus, tvPriority, tvDate;
        com.google.android.material.card.MaterialCardView cvStatus;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tvTicketCode);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            cvStatus = itemView.findViewById(R.id.cvStatus);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}

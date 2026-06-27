package com.example.ticktrack.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ticktrack.R;
import com.example.ticktrack.models.UserModel;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
    private List<UserModel> users = new ArrayList<>();
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(UserModel user);
    }

    public UserAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<UserModel> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserModel user = users.get(position);
        holder.tvUserName.setText(user.getName());
        holder.tvUserEmail.setText(user.getEmail());
        holder.tvTotalTickets.setText("Total Tiket: " + user.getTotalTickets());

        if (user.isActive()) {
            holder.tvStatus.setText("AKTIF");
            holder.cvStatus.setCardBackgroundColor(Color.parseColor("#10B981")); // Emerald 500
        } else {
            holder.tvStatus.setText("NONAKTIF");
            holder.cvStatus.setCardBackgroundColor(Color.parseColor("#EF4444")); // Red 500
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUserClick(user);
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserEmail, tvTotalTickets, tvStatus;
        MaterialCardView cvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvTotalTickets = itemView.findViewById(R.id.tvTotalTickets);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            cvStatus = itemView.findViewById(R.id.cvStatus);
        }
    }
}

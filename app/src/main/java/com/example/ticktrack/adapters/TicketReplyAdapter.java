package com.example.ticktrack.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ticktrack.R;
import com.example.ticktrack.models.TicketReply;
import java.util.ArrayList;
import java.util.List;

public class TicketReplyAdapter extends RecyclerView.Adapter<TicketReplyAdapter.ViewHolder> {
    private List<TicketReply> replies = new ArrayList<>();
    // Normally we pass current session user ID here to know which is 'User' vs 'Admin'
    // But since the model has `getUserRole()`, we can use `admin` vs `user`.

    public void setReplies(List<TicketReply> replies) {
        this.replies = replies;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket_reply, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TicketReply reply = replies.get(position);
        
        boolean isAdmin = reply.getUserRole() != null && reply.getUserRole().equals("admin");

        if (isAdmin) {
            // Show Admin (Left), Hide User (Right)
            holder.layoutAdminMessage.setVisibility(View.VISIBLE);
            holder.layoutUserMessage.setVisibility(View.GONE);

            holder.tvAdminName.setText(reply.getUserName() + " (Admin)");
            holder.tvAdminText.setText(reply.getMessage());
            holder.tvAdminDate.setText(reply.getCreatedAt());
        } else {
            // Show User (Right), Hide Admin (Left)
            holder.layoutUserMessage.setVisibility(View.VISIBLE);
            holder.layoutAdminMessage.setVisibility(View.GONE);

            holder.tvUserName.setText(reply.getUserName());
            holder.tvUserText.setText(reply.getMessage());
            holder.tvUserDate.setText(reply.getCreatedAt());
        }
    }

    @Override
    public int getItemCount() {
        return replies.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutAdminMessage, layoutUserMessage;
        TextView tvAdminName, tvAdminDate, tvAdminText;
        TextView tvUserName, tvUserDate, tvUserText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutAdminMessage = itemView.findViewById(R.id.layoutAdminMessage);
            layoutUserMessage = itemView.findViewById(R.id.layoutUserMessage);
            
            tvAdminName = itemView.findViewById(R.id.tvAdminName);
            tvAdminDate = itemView.findViewById(R.id.tvAdminDate);
            tvAdminText = itemView.findViewById(R.id.tvAdminText);
            
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserDate = itemView.findViewById(R.id.tvUserDate);
            tvUserText = itemView.findViewById(R.id.tvUserText);
        }
    }
}

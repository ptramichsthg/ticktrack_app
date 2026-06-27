package com.example.ticktrack.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ticktrack.R;
import com.example.ticktrack.models.CategoryModel;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private List<CategoryModel> categories = new ArrayList<>();
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onEditClick(CategoryModel category);
        void onDeleteClick(CategoryModel category);
    }

    public CategoryAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<CategoryModel> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryModel category = categories.get(position);
        holder.tvCategoryName.setText(category.getName());
        holder.tvTotalTickets.setText("Digunakan pada: " + category.getTotalTickets() + " tiket");
        
        try {
            holder.cvColorBadge.setCardBackgroundColor(Color.parseColor(category.getColor()));
        } catch (Exception e) {
            holder.cvColorBadge.setCardBackgroundColor(Color.parseColor("#3B82F6")); // Fallback to blue
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(category);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(category);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvTotalTickets;
        MaterialCardView cvColorBadge;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvTotalTickets = itemView.findViewById(R.id.tvTotalTickets);
            cvColorBadge = itemView.findViewById(R.id.cvColorBadge);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

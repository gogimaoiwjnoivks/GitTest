package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class PlantAdapter extends RecyclerView.Adapter<PlantAdapter.PlantViewHolder> {

    private final List<Plant> plantList;
    private OnPlantLongClickListener longClickListener;
    private OnPlantClickListener clickListener;

    public interface OnPlantLongClickListener {
        void onPlantLongClick(Plant plant, int position);
    }

    public interface OnPlantClickListener {
        void onPlantClick(Plant plant, int position);
    }

    public PlantAdapter(List<Plant> plantList) {
        this.plantList = plantList;
    }

    public void setOnPlantClickListener(OnPlantClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnPlantLongClickListener(OnPlantLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public PlantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_plant, parent, false);
        return new PlantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlantViewHolder holder, int position) {
        Plant plant = plantList.get(position);
        holder.tvGridPlantName.setText(plant.getName());

        if (plant.getImageUrl() != null && !plant.getImageUrl().isEmpty() && plant.getImageUrl().startsWith("http")) {
            Glide.with(holder.itemView.getContext())
                    .load(plant.getImageUrl())
                    .placeholder(R.drawable.white)
                    .error(R.drawable.white)
                    .into(holder.ivGridPlantItem);
        } else {
            holder.ivGridPlantItem.setImageResource(R.drawable.white);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPlantClick(plant, position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onPlantLongClick(plant, holder.getBindingAdapterPosition());
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return plantList.size();
    }

    static class PlantViewHolder extends RecyclerView.ViewHolder {
        ImageView ivGridPlantItem;
        TextView tvGridPlantName;

        public PlantViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGridPlantItem = itemView.findViewById(R.id.ivGridPlantItem);
            tvGridPlantName = itemView.findViewById(R.id.tvGridPlantName);
        }
    }
}
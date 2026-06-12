package com.example.myapplication; // ⚠️ 본인의 실제 패키지명인지 꼭 확인하세요!

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide; // 💡 Glide 임포트 추가
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

        // -------------------------------------------------------------------------
        // 🔥 [버그 완치 핵심] 구시대적인 new Thread 스레드 지옥을 철거하고,
        // 리사이클러뷰 뷰 재사용 버그를 완벽히 통제하는 Glide 엔진으로 전면 교체!
        // -------------------------------------------------------------------------
        if (plant.getImageUrl() != null && !plant.getImageUrl().isEmpty() && plant.getImageUrl().startsWith("http")) {
            Glide.with(holder.itemView.getContext())
                    .load(plant.getImageUrl())
                    .placeholder(R.drawable.white) // 로딩 중에 보여줄 임시 이미지
                    .error(R.drawable.white)       // 로딩 실패 시 보여줄 이미지
                    .into(holder.ivGridPlantItem);                  // 최종 목적지 뷰 지정
        } else {
            holder.ivGridPlantItem.setImageResource(R.drawable.white);
        }

        // 짧은 클릭 이벤트 연동
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPlantClick(plant, position);
            }
        });

        // 롱 클릭 이벤트 연동 (getAdapterPosition 대신 안정적인 bindingAdapterPosition 적용 권장)
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
package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> chatMessages;

    public ChatAdapter(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);

        if (message.getViewType() == ChatMessage.TYPE_USER) {
            holder.layoutUserMessage.setVisibility(View.VISIBLE);
            holder.layoutGeminiMessage.setVisibility(View.GONE);
            holder.tvUserMessage.setText(message.getMessage());
        } else {
            holder.layoutUserMessage.setVisibility(View.GONE);
            holder.layoutGeminiMessage.setVisibility(View.VISIBLE);
            holder.tvGeminiMessage.setText(message.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages != null ? chatMessages.size() : 0;
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutUserMessage;
        LinearLayout layoutGeminiMessage;
        TextView tvUserMessage;
        TextView tvGeminiMessage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutUserMessage = itemView.findViewById(R.id.layoutUserMessage);
            layoutGeminiMessage = itemView.findViewById(R.id.layoutGeminiMessage);
            tvUserMessage = itemView.findViewById(R.id.tvUserMessage);
            tvGeminiMessage = itemView.findViewById(R.id.tvGeminiMessage);
        }
    }
}
package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * 챗봇 대화 화면의 리사이클러뷰(RecyclerView) 연동을 위한 어댑터 클래스입니다.
 * 사용자 메시지와 AI(Gemini) 응답 메시지를 동적으로 바인딩하여 리스트로 렌더링합니다.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> chatMessages;

    public ChatAdapter(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    /**
     * 지정된 뷰 타입을 위한 새로운 ViewHolder 객체를 생성 및 초기화합니다.
     * 메모리 효율성을 위해 대화방의 기본 행 XML 레이아웃을 인플레이트합니다.
     */
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new ChatViewHolder(view);
    }

    /**
     * 데이터 리스트의 특정 포지션에 위치한 데이터를 ViewHolder의 뷰 구성요소에 바인딩합니다.
     * 메시지 송신자 타입(사용자 vs AI)에 따라 레이아웃의 가시성(Visibility)을 동적으로 제어합니다.
     */
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

    /**
     * 어댑터가 관리하는 총 데이터 항목의 개수를 반환합니다.
     */
    @Override
    public int getItemCount() {
        return chatMessages != null ? chatMessages.size() : 0;
    }

    /**
     * 리사이클러뷰의 행 내부 뷰 객체들을 보유하고 재사용하기 위한 ViewHolder 클래스입니다.
     * findViewById 레이아웃 탐색 비용을 절감하여 스크롤 성능을 최적화합니다.
     */
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
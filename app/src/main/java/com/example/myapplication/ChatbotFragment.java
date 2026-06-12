package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Google Gemini API와 통신하여 사용자에게 실시간 식물 케어 피드백을 제공하는 챗봇 프래그먼트입니다.
 * 비동기 네트워크 통신 및 SharedPreferences 기반의 대화 내역 영속성 관리를 수행합니다.
 */
public class ChatbotFragment extends Fragment {

    private RecyclerView rvChatMessages;
    private EditText etChatInput;
    private Button btnChatSend;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessageList;

    /** HTTP 비동기 통신을 처리하는 OkHttpClient 인스턴스입니다. (연결/읽기/쓰기 타임아웃 60초 설정) */
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private final Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chatbot, container, false);

        rvChatMessages = view.findViewById(R.id.rvChatMessages);
        etChatInput = view.findViewById(R.id.etChatInput);
        btnChatSend = view.findViewById(R.id.btnChatSend);

        // 로컬 SharedPreferences 스토리지로부터 복원된 이전 대화 기록을 로드합니다.
        chatMessageList = loadChatHistory();

        // 초기 진입하여 기존 대화 데이터가 공백인 경우, 기본 시스템 안내 메시지를 구성합니다.
        if (chatMessageList.isEmpty()) {
            chatMessageList.add(new ChatMessage("안녕하세요! 반려식물 케어 도우미 AI입니다. 무엇이든 물어보세요!", ChatMessage.TYPE_GEMINI));
            saveChatHistory();
        }

        chatAdapter = new ChatAdapter(chatMessageList);
        rvChatMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChatMessages.setAdapter(chatAdapter);

        if (!chatMessageList.isEmpty()) {
            rvChatMessages.scrollToPosition(chatMessageList.size() - 1);
        }

        /**
         * 전송 버튼 클릭 리스너 이벤트 처리 구역입니다.
         * 입력 폼 검증 후 리사이클러뷰 UI를 갱신하고 백엔드 API 요청을 유발합니다.
         */
        btnChatSend.setOnClickListener(v -> {
            String query = etChatInput.getText().toString().trim();
            if (query.isEmpty()) return;

            chatMessageList.add(new ChatMessage(query, ChatMessage.TYPE_USER));
            chatAdapter.notifyItemInserted(chatMessageList.size() - 1);
            rvChatMessages.scrollToPosition(chatMessageList.size() - 1);
            etChatInput.setText("");

            saveChatHistory();
            askChatbotQuestionToGemini(query);
        });

        return view;
    }

    /**
     * 구글 Gemini 원격 백엔드 서버로 REST API HTTP POST 요청을 전송하는 핵심 네트워크 메서드입니다.
     * * @param promptText 사용자가 입력한 자연어 질문 데이터
     */
    private void askChatbotQuestionToGemini(String promptText) {
        String apiKey = "AQ.Ab8RN6KPqavUEN-FxJrwBhgMWgj-WAZk4-9Cp7jOQc2z4bsnQQ";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" + apiKey;

        // API 규격에 맞는 JSON 데이터 트리 구조 생성
        Map<String, Object> root = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> contentMap = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", "너는 상냥하고 전문적인 반려식물 케어 인공지능 챗봇이야. 다음 질문에 친절하게 답변해줘: " + promptText);
        parts.add(textPart);

        contentMap.put("parts", parts);
        contents.add(contentMap);
        root.put("contents", contents);

        String jsonRequestBody = gson.toJson(root);

        RequestBody requestBody = RequestBody.create(jsonRequestBody, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(url).post(requestBody).build();

        /**
         * 비동기 작업 스레드 풀(Worker Thread Pool) 기반의 네트워크 큐 등록 인터페이스입니다.
         * 메인 UI 스레드 블로킹(ANR 현상)을 원천 차단합니다.
         */
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (getActivity() == null) return;
                // 비동기 작업 종료 후 UI 컴포넌트 접근을 위해 메인 스레드 루퍼로 컨텍스트 전환
                getActivity().runOnUiThread(() -> {
                    chatMessageList.add(new ChatMessage("죄송합니다. 네트워크 에러가 발생하여 답변을 가져오지 못했습니다.", ChatMessage.TYPE_GEMINI));
                    chatAdapter.notifyItemInserted(chatMessageList.size() - 1);
                    rvChatMessages.scrollToPosition(chatMessageList.size() - 1);
                    saveChatHistory();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseString = response.body().string();
                    try {
                        // 반환된 고밀도 JSON 페이로드 오브젝트 트리를 데이터 맵 구조로 동적 역직렬화
                        Map<String, Object> responseMap = gson.fromJson(responseString, Map.class);
                        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
                        Map<String, Object> firstCandidate = candidates.get(0);
                        Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
                        List<Map<String, Object>> resParts = (List<Map<String, Object>>) content.get("parts");
                        String resultText = (String) resParts.get(0).get("text");

                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            chatMessageList.add(new ChatMessage(resultText.trim(), ChatMessage.TYPE_GEMINI));
                            chatAdapter.notifyItemInserted(chatMessageList.size() - 1);
                            rvChatMessages.scrollToPosition(chatMessageList.size() - 1);
                            saveChatHistory();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * 최신 상태의 대화 리스트 객체를 JSON 포맷 문자열로 직렬화하여
     * 기기 내부의 SharedPreferences 영속성 스토리지 공간에 저장합니다.
     */
    private void saveChatHistory() {
        if (getContext() == null) return;

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("plant_chat_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String jsonHistory = gson.toJson(chatMessageList);
        editor.putString("chat_history", jsonHistory);
        editor.apply(); // 비동기 디스크 파일 I/O 동기화 명령 처리
    }

    /**
     * 기기 내부의 SharedPreferences 영속성 보관소에서 저장된 JSON 문자열을 로드하여,
     * 오리지널 ChatMessage 객체 리스트 파이프라인으로 역직렬화 복원합니다.
     */
    private List<ChatMessage> loadChatHistory() {
        List<ChatMessage> historyList = new ArrayList<>();
        if (getContext() == null) return historyList;

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("plant_chat_prefs", Context.MODE_PRIVATE);
        String jsonHistory = sharedPreferences.getString("chat_history", null);

        if (jsonHistory != null) {
            try {
                // 제네릭스 타입 렌더링 손실을 막기 위해 TypeToken 메커니즘 차용
                Type type = new TypeToken<List<ChatMessage>>(){}.getType();
                List<ChatMessage> restoredList = gson.fromJson(jsonHistory, type);
                if (restoredList != null) {
                    historyList.addAll(restoredList);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return historyList;
    }
}
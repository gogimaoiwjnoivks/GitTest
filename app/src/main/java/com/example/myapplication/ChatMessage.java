package com.example.myapplication;

/**
 * 챗봇 대화방에서 유동적으로 교환되는 메시지 데이터의 구조를 정의하는 모델 클래스입니다.
 */
public class ChatMessage {

    /** 사용자 메시지를 나타내는 뷰 타입 상수입니다. */
    public static final int TYPE_USER = 0;

    /** Gemini AI 응답 메시지를 나타내는 뷰 타입 상수입니다. */
    public static final int TYPE_GEMINI = 1;

    private String message;
    private int viewType;

    public ChatMessage(String message, int viewType) {
        this.message = message;
        this.viewType = viewType;
    }

    public String getMessage() { return message; }
    public int getViewType() { return viewType; }
}
package com.example.myapplication;


public class ChatMessage {
    public static final int TYPE_USER = 0;

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
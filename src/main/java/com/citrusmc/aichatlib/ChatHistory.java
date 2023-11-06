package com.citrusmc.chatbot;

import com.google.gson.Gson;

import java.util.LinkedList;

public class ChatHistory {
    private final LinkedList<Message> conversationHistory = new LinkedList<>();
    private int maxHistorySize;

    // Inner class to represent a message
    private static class Message {
        String role; // "user" or "assistant"
        String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public ChatHistory(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
    }

    public ChatHistory(ChatHistory chatHistory) {
        this.maxHistorySize = chatHistory.maxHistorySize;
        // Copy the reference to messages from the other chat history
        this.conversationHistory.addAll(chatHistory.conversationHistory);
    }

    // Method to add a message to the history
    private void addMessage(String role, String content) {
        if (conversationHistory.size() >= maxHistorySize) {
            conversationHistory.removeFirst(); // Remove the oldest message
        }
        conversationHistory.addLast(new Message(role, content));
    }

    // Method to add a user message to the history
    public void addUserMessage(String message) {
        addMessage("user", message);
    }

    // Method to add a system message to the history
    public void addSystemMessage(String message) {
        addMessage("system", message);
    }

    // Method to add an assistant message to the history
    public void addAssistantMessage(String message) {
        addMessage("assistant", message);
    }

    public int length() {
        return conversationHistory.size();
    }

    // Method to generate a JSON string from the conversation history
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(conversationHistory);
    }

    public ChatHistory concat(ChatHistory other) {
        ChatHistory newChatHistory = new ChatHistory(this.maxHistorySize + other.maxHistorySize);
        newChatHistory.conversationHistory.addAll(this.conversationHistory);
        newChatHistory.conversationHistory.addAll(other.conversationHistory);
        return newChatHistory;
    }

    public ChatHistory concat(ChatHistory other, boolean inplace) {
        if (inplace) {
            this.maxHistorySize += other.maxHistorySize;
            this.conversationHistory.addAll(other.conversationHistory);
            return this;
        } else {
            return concat(other);
        }
    }
}
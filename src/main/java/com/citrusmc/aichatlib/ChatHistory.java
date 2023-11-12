package com.citrusmc.aichatlib;

import com.google.gson.Gson;

import java.util.LinkedList;

/**
 * Class to represent the conversation history
 */
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

    /**
     * Constructor
     * @param maxHistorySize the maximum size of the chat history
     */
    public ChatHistory(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
    }

    /**
     * Copy constructor
     * @param chatHistory the chat history to copy
     */
    public ChatHistory(ChatHistory chatHistory) {
        this.maxHistorySize = chatHistory.maxHistorySize;
        // Copy the reference to messages from the other chat history
        this.conversationHistory.addAll(chatHistory.conversationHistory);
    }

    /**
     * Add a message to the chat history
     * @param role the role of the message
     * @param content the content of the message
     */
    private void addMessage(String role, String content) {
        if (conversationHistory.size() >= maxHistorySize) {
            conversationHistory.removeFirst(); // Remove the oldest message
        }
        conversationHistory.addLast(new Message(role, content));
    }

    /**
     * Add a user message to the chat history
     * @param message the message
     */
    public void addUserMessage(String message) {
        addMessage("user", message);
    }

    /**
     * Add a system message to the chat history
     * @param message the message
     */
    public void addSystemMessage(String message) {
        addMessage("system", message);
    }

    /**
     * Add an assistant message to the chat history
     * @param message the message
     */
    public void addAssistantMessage(String message) {
        addMessage("assistant", message);
    }

    public void addAssistantMessage(String message, boolean mergeAssistantMessage) {
        if (mergeAssistantMessage) {
            if (conversationHistory.size() > 0) {
                Message lastMessage = conversationHistory.getLast();
                if (lastMessage.role.equals("assistant")) {
                    lastMessage.content += message;
                    return;
                }
            }
        }
        addMessage("assistant", message);
    }


    /**
     * Get the length of the chat history
     */
    public int length() {
        return conversationHistory.size();
    }

    /**
     * Convert the chat history to JSON
     * @return the JSON string
     */
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(conversationHistory);
    }

    /**
     * Concatenate two chat histories
     * @param other the other chat history
     * @return the concatenated chat history
     */
    public ChatHistory concat(ChatHistory other) {
        ChatHistory newChatHistory = new ChatHistory(this.maxHistorySize + other.maxHistorySize);
        newChatHistory.conversationHistory.addAll(this.conversationHistory);
        newChatHistory.conversationHistory.addAll(other.conversationHistory);
        return newChatHistory;
    }

    /**
     * Concatenate two chat histories
     * @param other the other chat history
     * @param inplace whether to concatenate in place
     * @return the concatenated chat history
     */
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
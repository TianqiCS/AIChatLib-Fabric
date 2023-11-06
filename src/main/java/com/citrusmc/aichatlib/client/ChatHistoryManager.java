package com.citrusmc.chatbot.client;

import com.citrusmc.chatbot.ChatHistory;

import java.util.HashMap;
import java.util.Map;

public class ChatHistoryManager {
    private static final ChatHistoryManager INSTANCE = new ChatHistoryManager();
    private final Map<String, ChatHistory> chatHistoryMap;

    private ChatHistoryManager() {
        chatHistoryMap = new HashMap<>();
    }

    public static ChatHistoryManager getInstance() {
        return INSTANCE;
    }

    public void removeChatHistory(ChatGroup chatGroup, String sender) {
        if (chatGroup.includeNames) {
            this.chatHistoryMap.remove(chatGroup.name);
        } else {
            this.chatHistoryMap.remove(String.format("%s#%s", chatGroup.name, sender));
        }
    }

    public void addChatHistory(ChatGroup chatGroup, String sender, ChatHistory chatHistory) {
        if (chatGroup.includeNames) {
            this.chatHistoryMap.put(chatGroup.name, chatHistory);
        } else {
            this.chatHistoryMap.put(String.format("%s#%s", chatGroup.name, sender) , chatHistory);
        }
    }

    public ChatHistory getChatHistory(ChatGroup chatGroup, String sender) {
        ChatHistory chatHistory;
        if (chatGroup.includeNames) {
            chatHistory = this.chatHistoryMap.get(chatGroup.name);
        } else {
            chatHistory = this.chatHistoryMap.get(String.format("%s#%s", chatGroup.name, sender));
        }

        if (chatHistory == null) {
            // add 1 extra slots for the latest request
            chatHistory = new ChatHistory(chatGroup.chatHistorySize + 1);
            addChatHistory(chatGroup, sender, chatHistory);
        }
        return chatHistory;
    }
}

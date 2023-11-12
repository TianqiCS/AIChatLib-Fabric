package com.citrusmc.aichatlib.client;

import com.citrusmc.aichatlib.ChatHistory;

import java.util.HashMap;
import java.util.Map;

/**
 * ChatHistoryManager is a singleton class that manages the chat histories of the client.
 * If includeNames is false, then the key will be stored in the format of chatGroup.name#sender
 * Otherwise, the key will be stored in the format of chatGroup.name
 */
public class ChatHistoryManager {
    private static final ChatHistoryManager INSTANCE = new ChatHistoryManager();
    private final Map<String, ChatHistory> chatHistoryMap;

    private ChatHistoryManager() {
        chatHistoryMap = new HashMap<>();
    }

    public static ChatHistoryManager getInstance() {
        return INSTANCE;
    }

    /**
     * Remove the chat history from the chatHistoryMap
     * @param chatGroup the chat group
     * @param sender the sender
     */
    public void removeChatHistory(ChatGroup chatGroup, String sender) {
        if (chatGroup.includeNames) {
            this.chatHistoryMap.remove(chatGroup.name);
        } else {
            this.chatHistoryMap.remove(String.format("%s#%s", chatGroup.name, sender));
        }
    }

    /**
     * Add the chat history to the chatHistoryMap
     * @param chatGroup the chat group
     * @param sender the sender
     * @param chatHistory the chat history
     */
    public void addChatHistory(ChatGroup chatGroup, String sender, ChatHistory chatHistory) {
        if (chatGroup.includeNames) {
            this.chatHistoryMap.put(chatGroup.name, chatHistory);
        } else {
            this.chatHistoryMap.put(String.format("%s#%s", chatGroup.name, sender) , chatHistory);
        }
    }

    /**
     * Add the chat history to the chatHistoryMap directly
     * @param key the key name of the chat history
     * @param chatHistory the chat history
     */
    public void addChatHistory(String key, ChatHistory chatHistory) {
        this.chatHistoryMap.put(key, chatHistory);
    }

    /**
     * Get the chat history from the chatHistoryMap by chat group and sender name.
     * If not found, create and return a new chat history and add it to the chatHistoryMap
     * @param chatGroup the chat group
     * @param sender the sender
     * @return the chat history
     */
    public ChatHistory retrieveChatHistory(ChatGroup chatGroup, String sender) {
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

    /**
     * Get the chat history from the chatHistoryMap by key. Will not create a new chat history if not found.
     * @param key the key name of the chat history
     * @return the chat history
     */
    public ChatHistory getChatHistory(String key) {
        ChatHistory chatHistory;
        chatHistory = this.chatHistoryMap.get(key);
        return chatHistory;
    }
}

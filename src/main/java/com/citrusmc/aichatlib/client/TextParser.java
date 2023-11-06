package com.citrusmc.chatbot.client;

import com.citrusmc.chatbot.configs.ClientConfig;
import com.citrusmc.chatbot.configs.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextParser {
    public static final Logger LOGGER = LoggerFactory.getLogger("ChatBot");
    private static final Config CONFIG = ClientConfig.getInstance();
    public static List<ChatGroup> chatGroups;
    static TextParser instance = null;

    private TextParser() {
        chatGroups = loadChatGroups((Map<String, Object>) CONFIG.get("ChatGroups"));
    }

    public static TextParser getInstance() {
        if (instance == null) {
            instance = new TextParser();
        }
        return instance;
    }

    private List<ChatGroup> loadChatGroups(Map<String, Object> chatGroups) {
        List<ChatGroup> chatGroupList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : chatGroups.entrySet()) {
            String name = entry.getKey();
            Map<String, Object> chatGroup = (Map<String, Object>) entry.getValue();
            ChatGroup newChatGroup = new ChatGroup(name, chatGroup);
            chatGroupList.add(newChatGroup);
        }
        ChatGroup vanillaChatGroup = new ChatGroup();
        chatGroupList.add(vanillaChatGroup);

        return chatGroupList;
    }

    public String[] parseChatMessage(String text) {
        ChatGroup chatGroup = getChatGroupByName("vanilla");
        Pattern sender_pattern = Pattern.compile(chatGroup.sender);
        Pattern message_pattern = Pattern.compile(chatGroup.message);
        Matcher sender_match = sender_pattern.matcher(text);
        Matcher message_match = message_pattern.matcher(text);
        if (sender_match.find() && message_match.find()) {
            String sender = sender_match.group(1);
            String message = message_match.group(1);

            // check if the sender or message is blacklisted
            boolean blacklisted = false;
            if (chatGroup.blacklist != null && chatGroup.blacklist.size() > 0) {
                for (String blacklist : chatGroup.blacklist) {
                    if (sender.equals(blacklist) || message.contains(blacklist)) {
                        blacklisted = true;
                        break;
                    }
                }
            }
            if (blacklisted) {
                return null;
            }

            // check if the message contains any triggers
            if (chatGroup.triggers != null && chatGroup.triggers.size() > 0) {
                for (String trigger : chatGroup.triggers) {
                    if (message.contains(trigger)) {
                        return new String[]{chatGroup.name, sender, message};
                    }
                }
            } else {
                return new String[]{chatGroup.name, sender, message};
            }
        }
        return null;
    }

    // parse game message and return the first matching chat group by group name, sender name and message
    public String[] parseGameMessage(String text) {
        for (ChatGroup chatGroup : chatGroups) {
            // check if the message matches the chat group
            Pattern sender_pattern = Pattern.compile(chatGroup.sender);
            Pattern message_pattern = Pattern.compile(chatGroup.message);
            Matcher sender_match = sender_pattern.matcher(text);
            Matcher message_match = message_pattern.matcher(text);
            if (sender_match.find() && message_match.find()) {
                String sender = sender_match.group(1);
                String message = message_match.group(1);

                // check if the sender or message is blacklisted
                boolean blacklisted = false;
                if (chatGroup.blacklist != null && chatGroup.blacklist.size() > 0) {
                    for (String blacklist : chatGroup.blacklist) {
                        if (sender.equals(blacklist) || message.contains(blacklist)) {
                            blacklisted = true;
                            break;
                        }
                    }
                }
                if (blacklisted) {
                    continue;
                }

                // check if the message contains any triggers
                if (chatGroup.triggers != null && chatGroup.triggers.size() > 0) {
                    for (String trigger : chatGroup.triggers) {
                        if (message.contains(trigger)) {
                            return new String[]{chatGroup.name, sender, message};
                        }
                    }
                } else {
                    return new String[]{chatGroup.name, sender, message};
                }
            }
        }
        return null;
    }

    public static ChatGroup getChatGroupByName(String name) {
        for (ChatGroup chatGroup : chatGroups) {
            if (chatGroup.name.equals(name)) {
                return chatGroup;
            }
        }
        return null;
    }
}

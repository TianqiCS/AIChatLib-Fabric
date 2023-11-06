package com.citrusmc.aichatlib.client;

import com.citrusmc.aichatlib.configs.ClientConfig;
import com.citrusmc.aichatlib.configs.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TextParser class
 * <p>
 * This class is used to parse the chat message and game message.
 * <p>
 */
public class TextParser {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChatBot-TextParser");
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

    /**
     * Load chat groups defined from config file
     *
     * @param chatGroups the chat groups from config file
     * @return the list of chat groups
     */
    private List<ChatGroup> loadChatGroups(Map<String, Object> chatGroups) {
        List<ChatGroup> chatGroupList = new ArrayList<>();
        // add custom chat groups
        for (Map.Entry<String, Object> entry : chatGroups.entrySet()) {
            String name = entry.getKey();
            Map<String, Object> chatGroup = (Map<String, Object>) entry.getValue();
            ChatGroup newChatGroup = new ChatGroup(name, chatGroup);
            chatGroupList.add(newChatGroup);
        }
        // add vanilla chat group
        ChatGroup vanillaChatGroup = new ChatGroup();
        chatGroupList.add(vanillaChatGroup);

        return chatGroupList;
    }

    /**
     * Parse chat message in vanilla format and return the first matching group name, sender name and message
     * @param text the chat message
     * @return the chat group name, sender name and message body
     */
    public String[] parseChatMessage(String text) {
        ChatGroup chatGroup = getChatGroupByName("vanilla");
        Pattern sender_pattern = Pattern.compile(chatGroup.sender);
        Pattern message_pattern = Pattern.compile(chatGroup.message);
        Matcher sender_match = sender_pattern.matcher(text);
        Matcher message_match = message_pattern.matcher(text);

        // check if we can find sender and message body matches the chat group
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

    /**
     * Parse game message and return the first matching group name, sender name and message
     * @param text the game message
     * @return the chat group name, sender name and message body
     */
    public String[] parseGameMessage(String text) {
        for (ChatGroup chatGroup : chatGroups) {
            // check if the message matches the chat group
            Pattern sender_pattern = Pattern.compile(chatGroup.sender);
            Pattern message_pattern = Pattern.compile(chatGroup.message);
            Matcher sender_match = sender_pattern.matcher(text);
            Matcher message_match = message_pattern.matcher(text);

            // check if we can find sender and message body matches the chat group
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

    /**
     * Get the chat group by name
     * @param name the chat group name
     * @return the chat group
     */
    public static ChatGroup getChatGroupByName(String name) {
        for (ChatGroup chatGroup : chatGroups) {
            if (chatGroup.name.equals(name)) {
                return chatGroup;
            }
        }
        return null;
    }
}

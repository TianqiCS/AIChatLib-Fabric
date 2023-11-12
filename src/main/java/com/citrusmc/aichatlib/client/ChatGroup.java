package com.citrusmc.aichatlib.client;

import com.citrusmc.aichatlib.configs.ClientConfig;
import com.citrusmc.aichatlib.configs.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;

/**
 * ChatGroup class
 * <p>
 * This class is used to store the settings for each chat group.
 * <p>
 * The settings are loaded from the config file.
 */
public class ChatGroup {
    private static final Config CONFIG = ClientConfig.getInstance();
    private static final Logger LOGGER = LoggerFactory.getLogger("ChatBot-ChatGroup");
    private static final String DEFAULT_MODEL = (String) CONFIG.get("Model.openai-engine");
    private static final String DEFAULT_MAX_TOKENS = CONFIG.get("Model.max-tokens").toString();
    private static final String DEFAULT_TEMPERATURE = CONFIG.get("Model.temperature").toString();
    private static final String DEFAULT_TOP_P = CONFIG.get("Model.top-p").toString();
    private static final String DEFAULT_FREQUENCY_PENALTY = CONFIG.get("Model.frequency-penalty").toString();
    private static final String DEFAULT_PRESENCE_PENALTY = CONFIG.get("Model.presence-penalty").toString();

    /**
     * The name of the chat group
     */
    public String name;

    /**
     * The regex pattern to match the sender
     */
    public String sender;
    /**
     * The regex pattern to match the message
     */
    public String message;
    /**
     * The list of blacklisted words in sender and message
     */
    public ArrayList<String> blacklist;

    /**
     * The list of triggers so that the bot will only respond to messages that contain one of the triggers
     */
    public ArrayList<String> triggers;

    public String model;
    public String maxTokens;
    public String prompt;
    public double temperature;
    public double topP;
    public double frequencyPenalty;
    public double presencePenalty;

    /**
     * Whether to include the sender's name in the message
     * Potentially useful for group chats
     */
    public boolean includeNames;

    /**
     * The number of messages allowed in the chat history
     * 0 means no chat history
     */
    public int chatHistorySize;

    /**
     * The list of stop sequences
     * OpenAI: Up to 4 sequences where the API will stop generating further tokens.
     * No checks are made for the number of sequences.
     */
    public ArrayList<String> stop;

    // vanilla chat group
    public ChatGroup() {
        this.name = "vanilla";
        this.sender = "^<([^> ]+)> .*$";
        this.message = "^<[^> ]+> (.*)$";

        if (CONFIG.get("VanillaChatGroup.triggers") != null)
            this.triggers = (ArrayList<String>) CONFIG.get("VanillaChatGroup.triggers");
        if (CONFIG.get("VanillaChatGroup.blacklist") != null)
            this.blacklist = (ArrayList<String>) CONFIG.get("VanillaChatGroup.blacklist");
        if (CONFIG.get("VanillaChatGroup.stop-sequences")!= null)
            this.stop = (ArrayList<String>) CONFIG.get("VanillaChatGroup.stop-sequences");

        if (CONFIG.get("VanillaChatGroup.prompt")!= null)
            this.prompt = (String) CONFIG.get("VanillaChatGroup.prompt");

        if (CONFIG.get("VanillaChatGroup.model") != null)
            this.model = (String) CONFIG.get("VanillaChatGroup.model");
        else this.model = DEFAULT_MODEL;
        if (CONFIG.get("VanillaChatGroup.max-tokens") != null)
            this.maxTokens = (String) CONFIG.get("VanillaChatGroup.max-tokens");
        else this.maxTokens = DEFAULT_MAX_TOKENS;
        if (CONFIG.get("VanillaChatGroup.temperature") != null)
            this.temperature = (double) CONFIG.get("VanillaChatGroup.temperature");
        else this.temperature = Float.parseFloat(DEFAULT_TEMPERATURE);
        if (CONFIG.get("VanillaChatGroup.top-p") != null)
            this.topP = (double) CONFIG.get("VanillaChatGroup.top-p");
        else this.topP = Float.parseFloat(DEFAULT_TOP_P);
        if (CONFIG.get("VanillaChatGroup.frequency-penalty") != null)
            this.frequencyPenalty = (double) CONFIG.get("VanillaChatGroup.frequency-penalty");
        else this.frequencyPenalty = Float.parseFloat(DEFAULT_FREQUENCY_PENALTY);
        if (CONFIG.get("VanillaChatGroup.presence-penalty") != null)
            this.presencePenalty = (double) CONFIG.get("VanillaChatGroup.presence-penalty");
        else this.presencePenalty = Float.parseFloat(DEFAULT_PRESENCE_PENALTY);
        if (CONFIG.get("VanillaChatGroup.chat-history-size") != null)
            this.chatHistorySize = (int) CONFIG.get("VanillaChatGroup.chat-history-size");
        else this.chatHistorySize = 0;
        if (CONFIG.get("VanillaChatGroup.include-names") != null)
            this.includeNames = (boolean) CONFIG.get("VanillaChatGroup.include-names");
        else this.includeNames = false;
    }

    public ChatGroup(String name, Map<String, Object> chatGroup) {
        this.name = name;
        this.sender = (String) chatGroup.get("sender");
        this.message = (String) chatGroup.get("message");

        if (chatGroup.get("blacklist") != null)
            this.blacklist = (ArrayList<String>) chatGroup.get("blacklist");
        if (chatGroup.get("triggers") != null)
            this.triggers = (ArrayList<String>) chatGroup.get("triggers");
        if (chatGroup.get("stop-sequences") != null)
            this.stop = (ArrayList<String>) chatGroup.get("stop-sequences");

        if (chatGroup.get("prompt") != null)
            this.prompt = (String) chatGroup.get("prompt");

        if (chatGroup.get("model") != null)
            this.model = (String) chatGroup.get("model");
        else this.model = DEFAULT_MODEL;
        if (chatGroup.get("max-tokens") != null)
            this.maxTokens = String.valueOf(chatGroup.get("max-tokens"));
        else this.maxTokens = DEFAULT_MAX_TOKENS;
        if (chatGroup.get("temperature") != null)
            this.temperature = (double) chatGroup.get("temperature");
        else this.temperature = Float.parseFloat(DEFAULT_TEMPERATURE);
        if (chatGroup.get("top-p") != null)
            this.topP = (double) chatGroup.get("top-p");
        else this.topP = Float.parseFloat(DEFAULT_TOP_P);
        if (chatGroup.get("frequency-penalty") != null)
            this.frequencyPenalty = (double) chatGroup.get("frequency-penalty");
        else this.frequencyPenalty = Float.parseFloat(DEFAULT_FREQUENCY_PENALTY);
        if (chatGroup.get("presence-penalty") != null)
            this.presencePenalty = (double) chatGroup.get("presence-penalty");
        else this.presencePenalty = Float.parseFloat(DEFAULT_PRESENCE_PENALTY);
        if (chatGroup.get("chat-history-size") != null)
            this.chatHistorySize = (int) chatGroup.get("chat-history-size");
        else this.chatHistorySize = 0;
        if (chatGroup.get("include-names") != null)
            this.includeNames = (boolean) chatGroup.get("include-names");
        else this.includeNames = false;
    }
}


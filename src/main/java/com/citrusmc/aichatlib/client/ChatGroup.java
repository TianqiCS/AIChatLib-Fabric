package com.citrusmc.chatbot.client;

import com.citrusmc.chatbot.configs.ClientConfig;
import com.citrusmc.chatbot.configs.Config;

import java.util.ArrayList;
import java.util.Map;

public class ChatGroup {
    private static final Config CONFIG = ClientConfig.getInstance();
    private static final String DEFAULT_MODEL = (String) CONFIG.get("Model.openai-engine");
    private static final String DEFAULT_MAX_TOKENS = CONFIG.get("Model.max-tokens").toString();
    private static final String DEFAULT_TEMPERATURE = CONFIG.get("Model.temperature").toString();
    private static final String DEFAULT_TOP_P = CONFIG.get("Model.top-p").toString();
    private static final String DEFAULT_FREQUENCY_PENALTY = CONFIG.get("Model.frequency-penalty").toString();
    private static final String DEFAULT_PRESENCE_PENALTY = CONFIG.get("Model.presence-penalty").toString();
    String name;
    String sender;
    String message;
    ArrayList<String> blacklist;
    ArrayList<String> triggers;
    public String model;
    public String maxTokens;
    public String prompt;
    public float temperature;
    public float topP;
    public float frequencyPenalty;
    public float presencePenalty;
    public boolean includeNames;
    public int chatHistorySize;

    // vanilla chat group
    public ChatGroup() {
        this.name = "vanilla";
        this.sender = "^<([^> ]+)> .*$";
        this.message = "^<[^> ]+> (.*)$";

        this.triggers = (ArrayList<String>) CONFIG.get("VanillaChatGroup.triggers");
        this.blacklist = (ArrayList<String>) CONFIG.get("VanillaChatGroup.blacklist");
        this.prompt = (String) CONFIG.get("VanillaChatGroup.prompt");

        if (CONFIG.get("VanillaChatGroup.model") != null)
            this.model = (String) CONFIG.get("VanillaChatGroup.model");
        else this.model = DEFAULT_MODEL;
        if (CONFIG.get("VanillaChatGroup.max-tokens") != null)
            this.maxTokens = (String) CONFIG.get("VanillaChatGroup.max-tokens");
        else this.maxTokens = DEFAULT_MAX_TOKENS;
        if (CONFIG.get("VanillaChatGroup.temperature") != null)
            this.temperature = (float) CONFIG.get("VanillaChatGroup.temperature");
        else this.temperature = Float.parseFloat(DEFAULT_TEMPERATURE);
        if (CONFIG.get("VanillaChatGroup.top-p") != null)
            this.topP = (float) CONFIG.get("VanillaChatGroup.top-p");
        else this.topP = Float.parseFloat(DEFAULT_TOP_P);
        if (CONFIG.get("VanillaChatGroup.frequency-penalty") != null)
            this.frequencyPenalty = (float) CONFIG.get("VanillaChatGroup.frequency-penalty");
        else this.frequencyPenalty = Float.parseFloat(DEFAULT_FREQUENCY_PENALTY);
        if (CONFIG.get("VanillaChatGroup.presence-penalty") != null)
            this.presencePenalty = (float) CONFIG.get("VanillaChatGroup.presence-penalty");
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
        if (chatGroup.get("prompt") != null)
            this.prompt = (String) chatGroup.get("prompt");

        if (chatGroup.get("model") != null)
            this.model = (String) chatGroup.get("model");
        else this.model = DEFAULT_MODEL;
        if (chatGroup.get("maxTokens") != null)
            this.maxTokens = (String) chatGroup.get("maxTokens");
        else this.maxTokens = DEFAULT_MAX_TOKENS;
        if (chatGroup.get("temperature") != null)
            this.temperature = (float) chatGroup.get("temperature");
        else this.temperature = Float.parseFloat(DEFAULT_TEMPERATURE);
        if (chatGroup.get("topP") != null)
            this.topP = (float) chatGroup.get("topP");
        else this.topP = Float.parseFloat(DEFAULT_TOP_P);
        if (chatGroup.get("frequencyPenalty") != null)
            this.frequencyPenalty = (float) chatGroup.get("frequencyPenalty");
        else this.frequencyPenalty = Float.parseFloat(DEFAULT_FREQUENCY_PENALTY);
        if (chatGroup.get("presencePenalty") != null)
            this.presencePenalty = (float) chatGroup.get("presencePenalty");
        else this.presencePenalty = Float.parseFloat(DEFAULT_PRESENCE_PENALTY);
        if (chatGroup.get("chatHistorySize") != null)
            this.chatHistorySize = (int) chatGroup.get("chatHistorySize");
        else this.chatHistorySize = 0;
        if (chatGroup.get("includeNames") != null)
            this.includeNames = (boolean) chatGroup.get("includeNames");
        else this.includeNames = false;
    }
}


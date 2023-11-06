package com.citrusmc.aichatlib;

import com.citrusmc.aichatlib.client.ChatGroup;
import com.citrusmc.aichatlib.configs.ClientConfig;
import com.citrusmc.aichatlib.configs.Config;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;

public class ChatGPTUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChatBot-ChatGPTUtil");
    private static final OkHttpClient httpClient = HttpClientFactory.createClient();
    private static final Config CONFIG = ClientConfig.getInstance();
    private static final Boolean USE_STREAM = (Boolean) CONFIG.get("Model.use-stream");
    protected static final String API_URL = (String) CONFIG.get("Model.api");
    private static final String API_KEY = (String) CONFIG.get("Model.openai-key");
    private static final String PROMPT = (String) CONFIG.get("Model.system-prompt");

    @Deprecated // Obsolete method, use getChatGPTResponseAsync instead
    public static JsonObject getChatGPTResponse(String message) {
        RequestBody body = RequestBody.create(
                "{\"model\": \"gpt-3.5-turbo\", \"messages\": [{\"role\": \"system\", \"content\": \"You are a helpful assistant.\"}, {\"role\": \"user\", \"content\": \"" + message + "\"}]}",
                okhttp3.MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            // Extract the response from ChatGPT (you might need to adjust this part based on the actual response structure)
            String responseBody = response.body().string();
            return new Gson().fromJson(responseBody, JsonObject.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new Gson().fromJson("{\"error\":\"Failed to get response from ChatGPT\"}", JsonObject.class);
        }
    }


    /**
     * Create the headers for the request
     *
     * @return the headers
     */
    protected static Headers createHeaders() {
        return new Headers.Builder()
                .add("Content-Type", "application/json")
                .add("Authorization", "Bearer " + API_KEY)
                .build();
    }
    /**
     * Create the request body for the request
     *
     * @param message   the message
     * @return the request body
     */
    @Deprecated // Consider using a chat group instead
    public static RequestBody createBody(String message) {
        return RequestBody.create(
                "{" +
                        "\"model\": \"gpt-3.5-turbo\", " +
                        "\"messages\": [{\"role\": \"system\", " +
                        "\"content\": \"You are a helpful assistant.\"}, " +
                        "{" +
                            "\"role\": \"user\", " +
                            "\"content\": \"" + message + "\"" +
                        "}], " +
                        "\"stream\": " + USE_STREAM +
                        "}",
                okhttp3.MediaType.get("application/json; charset=utf-8")
        );
    }

    /**
     * Create the request body for the request
     *
     * @param chatGroup the chat group
     * @param message   the message
     * @return the request body
     */
    public static RequestBody createBody(ChatGroup chatGroup, String message) {
        ChatHistory chatHistory;
        // > 2 size does not matter, we only need the last 2 messages for the prompt and the user message
        chatHistory = new ChatHistory(10);
        chatHistory.addUserMessage(message);
        return createBody(chatGroup, chatHistory);
    }

    /**
     * Create the request body for the request
     * @param chatGroup the chat group
     * @param chatHistory the chat history
     * @return the request body
     */
    public static RequestBody createBody(ChatGroup chatGroup, ChatHistory chatHistory) {
        String newPrompt;
        ChatHistory newChatHistory;

        // add chat-group specific prompt to the system prompt
        if (chatGroup.prompt != null) {
            newPrompt = PROMPT + " " + chatGroup.prompt;
        } else {
            newPrompt = PROMPT;
        }

        // add system message (prompt) to the chat history
        newChatHistory = new ChatHistory(1);
        newChatHistory.addSystemMessage(newPrompt);
        newChatHistory = newChatHistory.concat(chatHistory, true);

        String body = "{" +
                "\"model\": \"" + chatGroup.model + "\", " +
                "\"messages\": " + newChatHistory.toJson() + ", " +
                // stream is not compatible with max_tokens
                (USE_STREAM ? "": "\"max_tokens\": " + chatGroup.maxTokens + ", ") +
                "\"temperature\": " + chatGroup.temperature + ", " +
                "\"top_p\": " + chatGroup.topP + ", " +
                "\"frequency_penalty\": " + chatGroup.frequencyPenalty + ", " +
                "\"presence_penalty\": " + chatGroup.presencePenalty + ", " +
                "\"stream\": " + USE_STREAM +
                "}";

        if ((boolean) CONFIG.get("Settings.debug"))
            LOGGER.info(body);

        return RequestBody.create(
                body,
                okhttp3.MediaType.get("application/json; charset=utf-8")
        );
    }

    /**
     * Send and get response from ChatGPT asynchronously
     *
     * @param chatGroup the chat group
     * @param message   the message
     * @return the response from ChatGPT
     */
    public static CompletableFuture<JsonObject> getChatGPTResponseAsync(ChatGroup chatGroup, String message) {
        RequestBody body = createBody(chatGroup, message);
        Headers headers = createHeaders();
        return getChatGPTResponseAsync(headers, body);
    }

    /**
     * Send and get response from ChatGPT asynchronously
     *
     * @param chatGroup the chat group
     * @param chatHistory the chat history
     * @return the response from ChatGPT
     */
    public static CompletableFuture<JsonObject> getChatGPTResponseAsync(ChatGroup chatGroup, ChatHistory chatHistory) {
        RequestBody body = createBody(chatGroup, chatHistory);
        Headers headers = createHeaders();
        return getChatGPTResponseAsync(headers, body);
    }

    /**
     * Send and get response from ChatGPT asynchronously
     * @param message the message sent to ChatGPT
     * @return the response from ChatGPT
     */
    @Deprecated // Consider using a chat group instead
    public static CompletableFuture<JsonObject> getChatGPTResponseAsync(String message) {
        RequestBody body = createBody(message);
        Headers headers = createHeaders();
        return getChatGPTResponseAsync(headers, body);
    }

    /**
     * Send and get response from ChatGPT asynchronously
     *
     * @param headers the headers
     * @param body    the request body
     * @return the response from ChatGPT
     */
    public static CompletableFuture<JsonObject> getChatGPTResponseAsync(Headers headers, RequestBody body) {
        boolean debug = (boolean) CONFIG.get("Settings.debug");
        CompletableFuture<JsonObject> futureResponse = new CompletableFuture<>();
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .headers(headers)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (e instanceof SocketTimeoutException) {
                    if (debug)
                        LOGGER.info(String.format("Request timed out: %s", e.getMessage()));
                    futureResponse.completeExceptionally(new IOException("Request timed out"));
                } else {
                    if (debug)
                        LOGGER.info(String.format("Exception: %s", e.getMessage()));
                    futureResponse.completeExceptionally(e);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    if (debug)
                        LOGGER.info(String.format("Unexpected code: %s", response));
                    futureResponse.completeExceptionally(new IOException("Unexpected code " + response));
                } else {
                    // Extract the response from ChatGPT
                    String responseBody = response.body().string();
                    if (debug)
                        LOGGER.info(responseBody);
                    JsonObject responseBodyJson = new Gson().fromJson(responseBody, JsonObject.class);
                    futureResponse.complete(responseBodyJson);
                }
            }
        });

        return futureResponse;
    }


    /**
     * Get message from response object
     * @param response the response object
     * @return the message string
     */
    public static String getResponse(JsonObject response) {
        String content = response
                .get("choices").getAsJsonArray()
                .get(0).getAsJsonObject()
                .get("message").getAsJsonObject()
                .get("content").getAsString();
        return content;
    }


}
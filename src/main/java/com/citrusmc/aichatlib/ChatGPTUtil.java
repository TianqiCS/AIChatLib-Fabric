package com.citrusmc.chatbot;

import com.citrusmc.chatbot.client.ChatGroup;
import com.citrusmc.chatbot.configs.ClientConfig;
import com.citrusmc.chatbot.configs.Config;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;

public class ChatGPTUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChatBot");
    private static final OkHttpClient httpClient = HttpClientFactory.createClient();
    private static final Config CONFIG = ClientConfig.getInstance();

    static final Boolean USE_STREAM = (Boolean) CONFIG.get("Model.use-stream");
    static final String API_URL = "https://api.openai.com/v1/chat/completions";
    static final String API_KEY = (String) CONFIG.get("Model.openai-key");
    private static final String PROMPT = (String) CONFIG.get("Model.system-prompt");

    private static JsonObject getChatGPTResponse(String message) {
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



    public static Headers createHeaders() {
        return new Headers.Builder()
                .add("Content-Type", "application/json")
                .add("Authorization", "Bearer " + API_KEY)
                .build();
    }
    @Deprecated
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

    public static RequestBody createBody(ChatGroup chatGroup, String message) {
        ChatHistory chatHistory;
        // > 2 size does not matter, we only need the last 2 messages for the prompt and the user message
        chatHistory = new ChatHistory(10);
        chatHistory.addUserMessage(message);
        return createBody(chatGroup, chatHistory);
    }

    public static RequestBody createBody(ChatGroup chatGroup, ChatHistory chatHistory) {
        String newPrompt;
        ChatHistory newChatHistory;
        if (chatGroup.prompt != null) {
            newPrompt = PROMPT + " " + chatGroup.prompt;
        } else {
            newPrompt = PROMPT;
        }

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
        LOGGER.info(body); // XXX: Remove this line in production
        return RequestBody.create(
                body,
                okhttp3.MediaType.get("application/json; charset=utf-8")
        );
    }

    public static CompletableFuture<JsonObject> getChatGPTResponseAsync(ChatGroup chatGroup, String message) {
        RequestBody body = createBody(chatGroup, message);
        Headers headers = createHeaders();
        return getChatGPTResponseAsync(headers, body);
    }

    public static CompletableFuture<JsonObject> getChatGPTResponseAsync(ChatGroup chatGroup, ChatHistory chatHistory) {
        RequestBody body = createBody(chatGroup, chatHistory);
        Headers headers = createHeaders();
        return getChatGPTResponseAsync(headers, body);
    }

    @Deprecated
    public static CompletableFuture<JsonObject> getChatGPTResponseAsync(String message) {
        RequestBody body = createBody(message);
        Headers headers = createHeaders();
        return getChatGPTResponseAsync(headers, body);
    }

    public static CompletableFuture<JsonObject> getChatGPTResponseAsync(Headers headers, RequestBody body) {
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
                    LOGGER.info(String.format("Request timed out"));
                    futureResponse.completeExceptionally(new IOException("Request timed out"));
                } else {
                    LOGGER.info(String.format(e.getMessage()));
                    futureResponse.completeExceptionally(e);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    LOGGER.info(String.format(response.toString()));
                    futureResponse.completeExceptionally(new IOException("Unexpected code " + response));
                } else {
                    // Extract the response from ChatGPT (you might need to adjust this part based on the actual response structure)
                    String responseBody = response.body().string();
                    LOGGER.info(responseBody);
                    JsonObject responseBodyJson = new Gson().fromJson(responseBody, JsonObject.class);
                    futureResponse.complete(responseBodyJson); // You might want to parse the JSON and extract the specific field you need
                }
            }
        });

        return futureResponse;
    }


    public static String getResponse(JsonObject response) {
        String content = response
                .get("choices").getAsJsonArray()
                .get(0).getAsJsonObject()
                .get("message").getAsJsonObject()
                .get("content").getAsString();
        return content;
    }


}
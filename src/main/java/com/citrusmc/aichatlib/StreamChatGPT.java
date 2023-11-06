package com.citrusmc.aichatlib;

import com.citrusmc.aichatlib.client.ChatGroup;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.launchdarkly.eventsource.ConnectStrategy;
import com.launchdarkly.eventsource.EventSource;
import com.launchdarkly.eventsource.MessageEvent;
import com.launchdarkly.eventsource.background.BackgroundEventHandler;
import com.launchdarkly.eventsource.background.BackgroundEventSource;
import com.launchdarkly.eventsource.background.ConnectionErrorHandler;
import okhttp3.Headers;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.citrusmc.aichatlib.ChatGPTUtil.API_URL;

public class ConcurrentChatGPT {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChatBot");
    private static final HashMap<String, StringBuffer> messagesBuffer = new HashMap<>();
    private BackgroundEventSource eventSource;
    private volatile boolean isClosed = false;
    private int retry = 3;
    private final Consumer<String> onSucceed;
    private final Consumer<String> onTimeout;

    @Deprecated
    public ConcurrentChatGPT(String message, Consumer<String> onSucceed, Consumer<String> onTimeout) {
        this.onSucceed = onSucceed;
        this.onTimeout = onTimeout;
        createEventSource(message);
    }

    public ConcurrentChatGPT(ChatGroup chatGroup, String message, Consumer<String> onSucceed, Consumer<String> onTimeout) {
        this.onSucceed = onSucceed;
        this.onTimeout = onTimeout;
        createEventSource(chatGroup, message);
    }

    public ConcurrentChatGPT(ChatGroup chatGroup, ChatHistory chatHistory, Consumer<String> onSucceed, Consumer<String> onTimeout) {
        this.onSucceed = onSucceed;
        this.onTimeout = onTimeout;
        createEventSource(chatGroup, chatHistory);
    }

    public void start() {
        eventSource.start();
    }

    public void stop() {
        eventSource.close();
    }

    @Deprecated
    public void createEventSource(String message) {
        Headers headers = ChatGPTUtil.createHeaders();
        RequestBody body = ChatGPTUtil.createBody(message);
        createEventSource(headers, body);
    }

    public void createEventSource(ChatGroup chatGroup, String message) {
        Headers headers = ChatGPTUtil.createHeaders();
        RequestBody body = ChatGPTUtil.createBody(chatGroup, message);
        createEventSource(headers, body);
    }

    public void createEventSource(ChatGroup chatGroup, ChatHistory chatHistory) {
        Headers headers = ChatGPTUtil.createHeaders();
        RequestBody body = ChatGPTUtil.createBody(chatGroup, chatHistory);
        createEventSource(headers, body);
    }

    public void createEventSource(Headers headers, RequestBody body) {
        BackgroundEventHandler myHandler = new BackgroundEventHandler() {

            @Override
            public void onOpen() throws Exception {}

            @Override
            public void onClosed() throws Exception {
                if (isClosed) {
                    LOGGER.info("Connection closed by server");
                } else {
                    LOGGER.info("Connection closed unexpectedly");
                }
            }

            public void onMessage(String event, MessageEvent messageEvent) {
                // ... these methods are the same as for EventHandler before
                String data = messageEvent.getData();

                if (data.equals("[DONE]")) {
                    isClosed = true;
                    eventSource.close();
                    return;
                }

                JsonObject response = new Gson().fromJson(data, JsonObject.class);
                JsonElement finishReason = response.get("choices").getAsJsonArray()
                        .get(0).getAsJsonObject()
                        .get("finish_reason");
                if (!finishReason.isJsonNull()) {
                    isClosed = true;
                    eventSource.close();
                }

                String id = response.get("id").getAsString();
                if (!messagesBuffer.containsKey(id)) {
                    messagesBuffer.put(id, new StringBuffer());
                }
                String content = getResponse(response);
                StringBuffer buffer = messagesBuffer.get(id);

                if (content == null) {
                    onSucceed.accept(buffer.toString());
                    messagesBuffer.remove(id);
                    return;
                }
                buffer.append(content);
                int linebreakIndex = buffer.indexOf("\n");
                if (linebreakIndex > -1) {
                    onSucceed.accept(buffer.substring(0, linebreakIndex));
                    buffer.delete(0, linebreakIndex + 1);
                }
            }

            @Override
            public void onComment(String comment) throws Exception {}
            @Override
            public void onError(Throwable t) {}
        };

        ConnectionErrorHandler errorHandler = t -> {
            ConnectionErrorHandler.Action action = (isClosed || retry <= 0) ? ConnectionErrorHandler.Action.SHUTDOWN : ConnectionErrorHandler.Action.PROCEED;
            LOGGER.info(String.format("Connection error [%s %d]: %s", action, retry, t.getMessage()));

            if (action == ConnectionErrorHandler.Action.SHUTDOWN && retry <= 0) {
                onTimeout.accept(t.getMessage());
            }
            retry -= 1;
            return action;
        };


        this.eventSource = new BackgroundEventSource.Builder(myHandler,
                new EventSource.Builder(
                        ConnectStrategy.http(URI.create(API_URL))
                                .headers(headers)
                                .methodAndBody("POST", body)
                                .connectTimeout(10, TimeUnit.SECONDS)
                )
        )
                .threadPriority(Thread.MAX_PRIORITY)
                .connectionErrorHandler(errorHandler)
                // threadPriority, and other options related to worker threads,
                // are now properties of BackgroundEventSource
                .build();
    }

    public static String getResponse(JsonObject response) {
        JsonObject delta = response
                .get("choices").getAsJsonArray()
                .get(0).getAsJsonObject()
                .get("delta").getAsJsonObject();
        JsonElement content = delta.get("content");
        if (content != null) {
            return content.getAsString();
        }
        return null;
    }
}

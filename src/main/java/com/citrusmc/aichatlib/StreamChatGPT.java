package com.citrusmc.aichatlib;

import com.citrusmc.aichatlib.client.ChatGroup;
import com.citrusmc.aichatlib.configs.ClientConfig;
import com.citrusmc.aichatlib.configs.Config;
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

/**
 * StreamChatGPT class
 * <p>
 * This class is used to get the response from ChatGPT with stream feature.
 * <p>
 * Stream feature is used to get the response from ChatGPT in real time.
 * However, it does not allow to control the max tokens.
 */
public class StreamChatGPT {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChatBot-StreamChatGPT");
    private static final Config CONFIG = ClientConfig.getInstance();
    private static final HashMap<String, StringBuffer> messagesBuffer = new HashMap<>();
    private BackgroundEventSource eventSource;
    private volatile boolean isClosed = false;
    private int retry = 3;  // Retry 3 times if the connection is closed unexpectedly
    private final Consumer<String> onSucceed;
    private final Consumer<String> onTimeout;

    /**
     * Constructor with message only
     * @param message the message
     * @param onSucceed the callback function when receiving the response
     * @param onTimeout the callback function when the request times out
     */
    @Deprecated  // Consider using a chat group
    public StreamChatGPT(String message, Consumer<String> onSucceed, Consumer<String> onTimeout) {
        this.onSucceed = onSucceed;
        this.onTimeout = onTimeout;
        createEventSource(message);
    }

    /**
     * Constructor with chat group and message
     * @param chatGroup the chat group
     * @param message the message
     * @param onSucceed the callback function when receiving the response
     * @param onTimeout the callback function when the request times out
     */
    public StreamChatGPT(ChatGroup chatGroup, String message, Consumer<String> onSucceed, Consumer<String> onTimeout) {
        this.onSucceed = onSucceed;
        this.onTimeout = onTimeout;
        createEventSource(chatGroup, message);
    }

    /**
     * Constructor with chat group and chat history
     * @param chatGroup the chat group
     * @param chatHistory the chat history
     * @param onSucceed the callback function when receiving the response
     * @param onTimeout the callback function when the request times out
     */
    public StreamChatGPT(ChatGroup chatGroup, ChatHistory chatHistory, Consumer<String> onSucceed, Consumer<String> onTimeout) {
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

    /**
     * Create the server-sent events source
     * @param message the message
     */
    @Deprecated  // Consider using a chat group
    public void createEventSource(String message) {
        Headers headers = ChatGPTUtil.createHeaders();
        RequestBody body = ChatGPTUtil.createBody(message);
        createEventSource(headers, body);
    }

    /**
     * Create the server-sent events source with chat group and message
     * @param chatGroup the chat group
     * @param message the message
     */
    public void createEventSource(ChatGroup chatGroup, String message) {
        Headers headers = ChatGPTUtil.createHeaders();
        RequestBody body = ChatGPTUtil.createBody(chatGroup, message);
        createEventSource(headers, body);
    }

    /**
     * Create the server-sent events source with chat group and chat history
     * @param chatGroup the chat group
     * @param chatHistory the chat history
     */
    public void createEventSource(ChatGroup chatGroup, ChatHistory chatHistory) {
        Headers headers = ChatGPTUtil.createHeaders();
        RequestBody body = ChatGPTUtil.createBody(chatGroup, chatHistory);
        createEventSource(headers, body);
    }

    /**
     * Create the server-sent events source
     * @param headers the headers
     * @param body the request body
     */
    public void createEventSource(Headers headers, RequestBody body) {
        boolean debug = (boolean) CONFIG.get("Settings.debug");
        BackgroundEventHandler myHandler = new BackgroundEventHandler() {

            @Override
            public void onOpen() throws Exception {}

            @Override
            public void onClosed() throws Exception {
                if (isClosed) {
                    if (debug)
                        LOGGER.info("Connection closed by server");
                } else {
                    if (debug)
                        LOGGER.info("Connection closed unexpectedly");
                }
            }

            public void onMessage(String event, MessageEvent messageEvent) {

                String data = messageEvent.getData();

                // Check if the response is the end of the conversation
                if (data.equals("[DONE]")) {
                    isClosed = true;
                    eventSource.close();
                    return;
                }

                // Check if the response is the end of the conversation
                JsonObject response = new Gson().fromJson(data, JsonObject.class);
                JsonElement finishReason = response.get("choices").getAsJsonArray()
                        .get(0).getAsJsonObject()
                        .get("finish_reason");
                if (!finishReason.isJsonNull()) {
                    isClosed = true;
                    eventSource.close();
                }

                // Add the new stream text to the buffer
                String id = response.get("id").getAsString();
                if (!messagesBuffer.containsKey(id)) {
                    messagesBuffer.put(id, new StringBuffer());
                }
                String content = getResponse(response);
                StringBuffer buffer = messagesBuffer.get(id);

                // The end of stream, activate the callback function
                if (content == null) {
                    onSucceed.accept(buffer.toString());
                    messagesBuffer.remove(id);
                    return;
                }

                buffer.append(content);

                // Check if the buffer contains a line break, if so, activate the callback function and send the message before the line break
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

        // Connection error handler
        ConnectionErrorHandler errorHandler = t -> {
            // Retry 3 times if the connection is closed unexpectedly
            // Do not retry if the connection is closed by our end
            ConnectionErrorHandler.Action action = (isClosed || retry <= 0) ? ConnectionErrorHandler.Action.SHUTDOWN : ConnectionErrorHandler.Action.PROCEED;
            if (debug)
                LOGGER.info(String.format("Connection error [%s %d]: %s", action, retry, t.getMessage()));

            if (action == ConnectionErrorHandler.Action.SHUTDOWN && retry <= 0) {
                onTimeout.accept(t.getMessage());
            }
            retry -= 1;
            return action;
        };

        // Create the server-sent events source
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

    /**
     * Get the response message from the response object
     * @param response the response object
     * @return the response message
      */
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

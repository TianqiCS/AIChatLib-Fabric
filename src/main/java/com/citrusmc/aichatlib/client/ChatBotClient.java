package com.citrusmc.aichatlib.client;

import com.citrusmc.aichatlib.ChatGPTUtil;
import com.citrusmc.aichatlib.ChatHistory;
import com.citrusmc.aichatlib.StreamChatGPT;
import com.citrusmc.aichatlib.configs.ClientConfig;
import com.citrusmc.aichatlib.configs.Config;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

/**
 * The client mod initializer
 */
public class ChatBotClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChatBot-Client");
    private static Config CONFIG = null;
    private static TextParser PARSER = null;
    private final Semaphore semaphore = new Semaphore(1);
    private volatile ChatHistoryManager chatHistoryManager = ChatHistoryManager.getInstance();

    /**
     * Initialize the client mod
     */
    @Override
    public void onInitializeClient() {
        CONFIG = ClientConfig.getInstance();
        PARSER = TextParser.getInstance();
        boolean useStream = (boolean) CONFIG.get("Model.use-stream");
        boolean debug = (boolean) CONFIG.get("Settings.debug");

        // Register the askgpt command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager
                            .literal("askgpt")
                            .then(argument("message", greedyString())
                            .executes(context -> {
                                String message = getString(context, "message");
                                context.getSource().sendFeedback(Text.of("Sending message to ChatGPT: " + message));
                                // Don't send empty messages
                                if (message == null || message.length() == 0) {
                                    return 0;
                                }
                                // Use vanilla chat group properties for the api call
                                ChatGroup chatGroup = TextParser.getChatGroupByName("vanilla");
                                if (useStream) {
                                    StreamChatGPT chatGPT = new StreamChatGPT(chatGroup, message, this::sendSelfMessages, this::onSelfRequestTimeout);
                                    chatGPT.start();
                                } else {
                                    ChatGPTUtil.getChatGPTResponseAsync(chatGroup, message).thenAccept(data -> {
                                        String response = ChatGPTUtil.getResponse(data);
                                        sendSelfMessages(response);
                                    });
                                }
                                return 0;
                            })));
        });

        // Register chat message event to capture chat messages (vanilla and modded environments)
        ClientReceiveMessageEvents.CHAT.register(((message, signedMessage, sender, params, receptionTimestamp) -> {
            ClientPlayerEntity client = MinecraftClient.getInstance().player;

            String text = message.getString();
            assert client != null;

            // Don't process the message if it's sent by the client itself
            String senderName = sender == null ? params.name().getString() : sender.getName();
            if (debug)
                LOGGER.info(String.format("Received chat message: %s from %s", text, senderName));

            String[] results;
            String chatGroupName;

            results = PARSER.parseChatMessage(message.getString());
            if (results == null) {
                return;
            }
            chatGroupName = results[0];
            // senderName = results[1]; // we get the sender name from the sender object
            text = results[2];
            processRequest(chatGroupName, useStream, senderName, text, client);
        }));

        // Register game message event to capture game messages (e.g., spigot server with custom chat format/plugins)
        ClientReceiveMessageEvents.GAME.register(((message, overlay) -> {
            String chatGroupName;
            String sender, text;
            String[] results;
            ClientPlayerEntity client = MinecraftClient.getInstance().player;
            assert client != null;

            results = PARSER.parseGameMessage(message.getString());
            if (results == null) {
                return;
            }

            chatGroupName = results[0];
            sender = results[1];
            text = results[2];
            if (debug)
                LOGGER.info(String.format("Received game message: chatGroup: %s sender: %s text: %s", chatGroupName, sender, text));
            processRequest(chatGroupName, useStream, sender, text, client);
        }));

        LOGGER.info("ChatBotClient initialized.");
    }

    /**
     * Process the request to send to ChatGPT
     * @param chatGroupName chat group name
     * @param useStream whether to use stream feature or not
     * @param sender sender name
     * @param text message text
     * @param client client player entity
     */
    private void processRequest(String chatGroupName, boolean useStream, String sender, String text, ClientPlayerEntity client) {
        ChatGroup chatGroup = TextParser.getChatGroupByName(chatGroupName);
        // Don't process the message if it's sent by the client itself
        if (!client.getName().getString().equals(sender)) {

            if (useStream) {
                // If the chat history size is greater than 0, enable the chat history feature
                if (chatGroup.chatHistorySize > 0) {
                    ChatHistory chatHistory = chatHistoryManager.retrieveChatHistory(chatGroup, sender);

                    // include the sender name in the chat history
                    if (chatGroup.includeNames)
                        chatHistory.addUserMessage(String.format("%s: %s", sender, text));
                    else
                        chatHistory.addUserMessage(text);

                    // warp the sendMessage method to add the response to the chat history
                    Consumer<String> onSucceed = (response) -> {
                        chatHistory.addAssistantMessage(response);
                        sendMessages(response);
                    };

                    // start the stream chat gpt thread
                    StreamChatGPT chatGPT = new StreamChatGPT(chatGroup, chatHistory,
                            onSucceed, this::onRequestTimeout);
                    chatGPT.start();
                }
                else {
                    StreamChatGPT chatGPT = new StreamChatGPT(chatGroup, text, this::sendMessages, this::onRequestTimeout);
                    chatGPT.start();
                }

            // when not using stream, we can just send the request and get the response
            } else {
                // If the chat history size is greater than 0, enable the chat history feature
                if (chatGroup.chatHistorySize > 0) {
                    ChatHistory chatHistory = chatHistoryManager.retrieveChatHistory(chatGroup, sender);

                    // include the sender name in the chat history
                    if (chatGroup.includeNames)
                        chatHistory.addUserMessage(String.format("%s: %s", sender, text));
                    else
                        chatHistory.addUserMessage(text);

                    // send the response from ChatGPT and add it to the chat history
                    ChatGPTUtil.getChatGPTResponseAsync(chatGroup, chatHistory).thenAccept(data -> {
                        String response = ChatGPTUtil.getResponse(data);
                        chatHistory.addAssistantMessage(response);
                        sendMessages(response);
                    });
                }
                // when not using chat history, we can just send the request and get the response
                else {
                    ChatGPTUtil.getChatGPTResponseAsync(chatGroup, text).thenAccept(data -> {
                        String response = ChatGPTUtil.getResponse(data);
                        sendMessages(response);
                    });
                }
            }
        }
    }

    /**
     * Split the response from ChatGPT into sentences and send them to the chat.
     * @param chunk message chunk
     */
    private void sendMessages(String chunk) {
        boolean debug = (boolean) CONFIG.get("Settings.debug");
        if (debug)
            LOGGER.info(String.format("Sending message from ChatGPT: %s", chunk));

        ClientPlayerEntity client = MinecraftClient.getInstance().player;
        // The maximum length of a single chat message
        int maxMessageLength = (int) CONFIG.get("Settings.max-message-length");
        // The delay between sending each message
        long inputDelay = ((Integer) CONFIG.get("Settings.input-delay")).longValue();

        // Replace double newlines with a single newline and split the message into sentences
        chunk = chunk.replace("\n\n", "\n");
        String[] lines = chunk.split("\n");

        for (final String line : lines) {
            // Don't send empty lines
            if (line.length() == 0) {
                continue;
            }

            List<String> messages = new ArrayList<>();

            // Split long sentences into multiple messages
            for (int j = 0; j < line.length(); j += maxMessageLength) {
                messages.add(line.substring(j, Math.min(line.length(), j + maxMessageLength)));
            }

            // Send each sentence into the chat with respect to the input delay
            for (String message : messages) {

                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }

                client.networkHandler.sendChatMessage(message);
                // Schedule the sending of each line on the main thread with a tiny interval in between
                try {
                    // Sleep for a tiny interval (e.g., 100 milliseconds) before scheduling the next line
                    // This is done on a separate thread, so it won't block the main thread
                    Thread.sleep(inputDelay);
                    semaphore.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

        }
    }

    /**
     * Send the response from ChatGPT to the client itself.
     * @param chunk message chunk
     */
    private void sendSelfMessages(String chunk) {
        boolean debug = (boolean) CONFIG.get("Settings.debug");
        if (debug)
            LOGGER.info(String.format("Sending message from ChatGPT: %s", chunk));

        ClientPlayerEntity client = MinecraftClient.getInstance().player;
        client.sendMessage(Text.of(chunk), false);
    }

    /**
     * Handle the timeout error when sending request to ChatGPT
     * @param error error message
     */
    private void onSelfRequestTimeout(String error) {
        ClientPlayerEntity client = MinecraftClient.getInstance().player;
        client.sendMessage(Text.of(String.format("Connection to ChatGPT timed out: %s", error)), false);
    }

    /**
     * Handle the timeout error when sending request to ChatGPT
     * @param error error message
     */
    private void onRequestTimeout(String error) {
        ClientPlayerEntity client = MinecraftClient.getInstance().player;
        client.networkHandler.sendChatMessage(String.format("Connection to ChatGPT timed out: %s", error));
    }

}
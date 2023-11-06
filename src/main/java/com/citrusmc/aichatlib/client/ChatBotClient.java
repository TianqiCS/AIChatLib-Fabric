package com.citrusmc.chatbot.client;

import com.citrusmc.chatbot.ChatGPTUtil;
import com.citrusmc.chatbot.ChatHistory;
import com.citrusmc.chatbot.ConcurrentChatGPT;
import com.citrusmc.chatbot.configs.ClientConfig;
import com.citrusmc.chatbot.configs.Config;
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

public class ChatBotClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("ChatBot");
    public static Config CONFIG = null;
    public static TextParser PARSER = null;
    private final Semaphore semaphore = new Semaphore(1);
    private volatile ChatHistoryManager chatHistoryManager = ChatHistoryManager.getInstance();
    @Override
    public void onInitializeClient() {
        CONFIG = ClientConfig.getInstance();
        PARSER = TextParser.getInstance();
        boolean useStream = (boolean) CONFIG.get("Model.use-stream");

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager
                            .literal("askgpt")
                            .then(argument("message", greedyString())
                            .executes(context -> {
                                String message = getString(context, "message");
                                context.getSource().sendFeedback(Text.of("Sending message to ChatGPT: " + message));
                                if (message == null || message.length() == 0) {
                                    return 0;
                                }
                                ChatGroup chatGroup = TextParser.getChatGroupByName("vanilla");
                                if (useStream) {
                                    ConcurrentChatGPT chatGPT = new ConcurrentChatGPT(chatGroup, message, this::sendSelfMessages, this::onSelfRequestTimeout);
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

        ClientReceiveMessageEvents.CHAT.register(((message, signedMessage, sender, params, receptionTimestamp) -> {
            ClientPlayerEntity client = MinecraftClient.getInstance().player;

            String text = message.getString();
            assert client != null;
            String senderName = sender == null ? params.name().getString() : sender.getName();
            LOGGER.info(String.format("Received chat message: %s from %s", text, senderName));
            // processRequest("vanilla", useStream, senderName, text, client);  // naive way to implement
            String[] results;
            String chatGroup;
            results = PARSER.parseChatMessage(message.getString());
            if (results == null) {
                return;
            }
            chatGroup = results[0];
            // senderName = results[1];
            text = results[2];
            processRequest(chatGroup, useStream, senderName, text, client);
        }));


        ClientReceiveMessageEvents.GAME.register(((message, overlay) -> {
            String chatGroup;
            String sender, text;
            String[] results;
            ClientPlayerEntity client = MinecraftClient.getInstance().player;
            //LOGGER.info(String.format("Received game message: %s", message.getString()));
            results = PARSER.parseGameMessage(message.getString());
            if (results == null) {
                return;
            }
            chatGroup = results[0];
            sender = results[1];
            text = results[2];
            LOGGER.info(String.format("Received game message: chatGroup: %s sender: %s text: %s", chatGroup, sender, text));
            processRequest(chatGroup, useStream, sender, text, client);
        }));

//        ServerMessageEvents.CHAT_MESSAGE.register(((message, sender, params) -> {
//            ClientPlayerEntity client = MinecraftClient.getInstance().player;
//            String text = message.getContent().getString();
//            assert client != null;
//            if (!client.getName().getString().equals(sender.getName().getString())) {
//                ConcurrentChatGPT chatGPT = new ConcurrentChatGPT(text, this::processChunk);
//                chatGPT.start();
////                ChatGPTUtil.getChatGPTResponseAsync(text).thenAccept(data -> {
////                    String response = ChatGPTUtil.getResponse(data);
////                    LOGGER.info(response);
////                    processChunk(response);
////                });
//                //
//
//            } else {
//                client.sendMessage(Text.of("self"), true);
//            }
//
//        }));

        LOGGER.info("ChatBotClient initialized.");
    }

    private void processRequest(String chatGroupName, boolean useStream, String sender, String text, ClientPlayerEntity client) {
        ChatGroup chatGroup = TextParser.getChatGroupByName(chatGroupName);
        if (!client.getName().getString().equals(sender)) {
            if (useStream) {
                if (chatGroup.chatHistorySize > 0) {
                    ChatHistory chatHistory = chatHistoryManager.getChatHistory(chatGroup, sender);
                    if (chatGroup.includeNames)
                        chatHistory.addUserMessage(String.format("%s: %s", sender, text));
                    else
                        chatHistory.addUserMessage(text);

                    // warp the sendMessage method to add the response to the chat history
                    Consumer<String> onSucceed = (response) -> {
                        chatHistory.addAssistantMessage(response);
                        sendMessages(response);
                    };

                    ConcurrentChatGPT chatGPT = new ConcurrentChatGPT(chatGroup, chatHistory,
                            onSucceed, this::onRequestTimeout);
                    chatGPT.start();
                }
                else {
                    ConcurrentChatGPT chatGPT = new ConcurrentChatGPT(chatGroup, text, this::sendMessages, this::onRequestTimeout);
                    chatGPT.start();
                }

            } else {
                if (chatGroup.chatHistorySize > 0) {
                    ChatHistory chatHistory = chatHistoryManager.getChatHistory(chatGroup, sender);
                    if (chatGroup.includeNames)
                        chatHistory.addUserMessage(String.format("%s: %s", sender, text));
                    else
                        chatHistory.addUserMessage(text);
                    ChatGPTUtil.getChatGPTResponseAsync(chatGroup, chatHistory).thenAccept(data -> {
                        String response = ChatGPTUtil.getResponse(data);
                        chatHistory.addAssistantMessage(response);
                        sendMessages(response);
                    });
                }
                else {
                    ChatGPTUtil.getChatGPTResponseAsync(chatGroup, text).thenAccept(data -> {
                        String response = ChatGPTUtil.getResponse(data);
                        sendMessages(response);
                    });
                }
            }
        }
    }

    private void sendMessages(String chunk) {
        ClientPlayerEntity client = MinecraftClient.getInstance().player;
        LOGGER.info(String.format("Sending message from ChatGPT: %s", chunk));
        int maxMessageLength = (int) CONFIG.get("Settings.max-message-length");
        long inputDelay = ((Integer) CONFIG.get("Settings.input-delay")).longValue();
        String[] lines = chunk.split("\n\n");

        for (final String line : lines) {
            List<String> messages = new ArrayList<>();
            for (int j = 0; j < line.length(); j += maxMessageLength) {
                messages.add(line.substring(j, Math.min(line.length(), j + maxMessageLength)));
            }

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

    private void sendSelfMessages(String chunk) {
        ClientPlayerEntity client = MinecraftClient.getInstance().player;
        LOGGER.info(String.format("Sending message from ChatGPT: %s", chunk));
        //int maxMessageLength = (int) CONFIG.get("Settings.max-message-length");
        String[] lines = chunk.split("\n\n");

        for (final String line : lines) {
            client.sendMessage(Text.of(line), false);
        }
    }

    private void onSelfRequestTimeout(String error) {
        ClientPlayerEntity client = MinecraftClient.getInstance().player;
        client.sendMessage(Text.of(String.format("Connection to ChatGPT timed out: %s", error)), false);
    }

    private void onRequestTimeout(String error) {
        ClientPlayerEntity client = MinecraftClient.getInstance().player;
        client.networkHandler.sendChatMessage(String.format("Connection to ChatGPT timed out: %s", error));
    }

}
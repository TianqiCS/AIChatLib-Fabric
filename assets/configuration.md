# Configuration Guide

This document explains how to configure AIChatLib to utilize GPT API.  
The configuration file is divided into several sections, each with its own set of options.  
For more information about the parameters, 
please refer to the [OpenAI API documentation](https://platform.openai.com/docs/api-reference/chat).

## Model Configuration

These default settings are related to the ChatGPT model and API.

- `api`: The URL of the ChatGPT API, replace it if you do not use the official OpenAI API.
- `openai-key`: Your OpenAI API key. Replace `sk-YOUR-KEY-HERE` with your actual key.
- `openai-engine`: The version of the GPT model to use (e.g., `gpt-3.5-turbo`).
- `use-stream`: Set to `true` to use the stream feature from the API. 
Tokens will be sent as data-only server-sent events as they become available.
This will likely result in a rapid response, but may not be supported by all API implementations.
Max-tokens will be ignored if this is enabled.
- `system-prompt`: The system prompt or instructions given to ChatGPT. 
New prompts from the chatGroup will be appended to this.
- `max-tokens`: The maximum number of tokens (words) in the API response.
Will be ignored if `use-stream` is enabled. And the request may be refused if the value is too high.
- `temperature`: Controls randomness in the response. Higher values mean more random responses.
- `top-p`: Controls diversity of the response. Higher values allow for a wider range of possible responses.
- `frequency-penalty`: Reduces the likelihood of repeating the same line.
- `presence-penalty`: Reduces the likelihood of repeating the same topic.

## Settings Configuration

General settings for the mod.

- `debug`: Set to `true` to enable debug mode. Logs will be printed to the console.
- `input-delay`: The delay (in milliseconds) before sending the chat messages. 
This can be useful if you are in a multiplayer environment and want to avoid sending messages too quickly. 
The delay is global to every chat groups respectively. It does not affect the client-side commands.
- `max-message-length`: The maximum length of each message in the Minecraft chat. 
If the response is longer than this, it will be split into multiple messages.

## VanillaChatGroup Configuration

Vanilla chat group settings. 
It will be applied to all chat messages in singleplayer and multiplayer that the client can capture chat events. 
In these events, the chat message and sender are available.
Some servers may not use Chat events but instead convert player messages as game (server) messages to avoid chat reports.
You will need to use the `ChatGroups` section to handle these cases.

- `chat-history-size`: The number of messages to keep in chat history. 0 to disable chat history. 
The question and answer are considered as two messages.
- `include-names`: Set to `true` to include player names in messages. 
This is potentially useful for creating a group-chat bot. 
If you are using chat history, and this is disabled, the chat history only include the messages from the same player.
- `triggers`: Specific words or phrases that trigger the assistant. If this is empty, the assistant will always respond.
- `blacklist`: Words, phrases, or usernames to exclude from processing if present in the message.
- `prompt`: Additional prompt text for ChatGPT specific to the chat group. The prompt will be appended to the system prompt.
- `max-tokens`, `temperature`, `top-p`, `openai-engine`, `frequency-penalty`, `presence-penalty`:
  Same as in the Model section, but override to the chat group.
- `stop-sequences`: Sequences of text that signal the end of a conversation. Leave empty to let the API decide.
- 
## ChatGroups Configuration

Customizable chat groups respond to game messages.
Since there is no sender information, the message is matched against a list of patterns.
You can use all the settings from the `VanillaChatGroup` section.

- `sender`: Regex pattern to identify the sender of a message.
- `message`: Regex pattern to extract the message content.


### Example Chat Groups

These are some example chat groups that you can use as a starting point for your own chat groups.

- `multiplayer1`: An example group with specific settings for a multiplayer environment.
- `vanilla-style-private-chat-example`: A group that mimics vanilla Minecraft one-to-one chat.
- `free-chat-example`: A group chat example.

Remember to restart your Minecraft server or client after making changes to the configuration file for them to take effect.

---

For more information or support, please visit [GitHub Issues](#) or contact the mod developer.

# AIChatLib-Fabric
Minecraft fabric mod (pure client side) to integrate OpenAI GPT API.  
You may find this project useful if you are interested in:
- Using OpenAI GPT API in Minecraft.
- Creating a chat bot in Minecraft.
- Developing a server side mod/plugin that uses OpenAI GPT API.
- Developing generic Java applications that uses OpenAI GPT API.

![screenshot.png](assets%2Fscreenshot.png)
## Features
- Chat integration with ChatGPT for intelligent and interactive conversations.
- Customizable actions and pattern matching for different chat messages during singleplayer and multiplayer.
- Chat history management for context retention.
- Multi-threading real-time text generation and customizable output intervals.
- YAML configuration loader for easy mod customization.

## Installation
1. Ensure you have [Fabric Loader](https://fabricmc.net/use/) installed.
2. Download or compile the latest release of the mod.
3. Place the `.jar` file into your `mods` folder located in your Minecraft directory.
4. If you haven't already, install the required dependencies:
    - [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
5. Run Minecraft with the Fabric profile to generate the config file.
6. Edit the `AIChatLib/ClientConfig.yml` file located in the `config` directory of your Minecraft folder to customize the mod settings.
7. Restart Minecraft and enjoy!

## Usage
- **You must have an OpenAI API key to use this mod.** You can get one at [OpenAI](https://platform.openai.com/account/api-keys/).
- To interact with ChatGPT, simply type your message in the chat with command `/askgpt`, and the AI will respond.
- For acting as a chat bot, edit the `VanillaChatGroup` and `ChatGroups`. When a player (except the client) message matches the pattern, the AI will respond with the corresponding action.
- Feel free to experiment with the prompt settings as it may drastically change the AI's response.
- Note, a longer prompt/message/chat history will result in a longer response time and **higher API token usage**. 

## Configuration
This [document](assets/configuration.md) explains how to configure AIChatLib to utilize GPT API.

## Dependencies
- Minecraft 1.20.2
- Fabric Loader
- Fabric API

## Contributing
Contributions are welcome! ~~Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.~~

## License
This mod is available under the [MIT License](LICENSE).

## Contact
For support or inquiries, please open an issue on the [GitHub issue tracker](https://github.com/TianqiCS/AIChatLib-Fabric/issues).

---

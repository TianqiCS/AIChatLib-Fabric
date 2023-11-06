package com.citrusmc.chatbot.configs;

public class ClientConfig extends Config{
    static ClientConfig instance = null;

    private ClientConfig() {
        String configFileLocation = "config/ClientConfig.yml";
        this.config = this.loadConfig(configFileLocation, getDefaultConfig());
    }

    public static Config getInstance() {
        if (instance == null) {
            instance = new ClientConfig();
        }
        return instance;
    }


    private ClientConfig(String configLocation, String jarDefault) {
        config = loadConfig(configLocation, jarDefault);
    }
    @Override
    String getDefaultConfig() {
        return String.valueOf(getClass().getClassLoader().getResource("configs/ClientConfig.yml"));
    }
}

package com.citrusmc.aichatlib.configs;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public abstract class Config {

    protected Map<String, Object> config = null;

    public synchronized Map<String, Object> loadConfig(String configFileLocation, String jarDefaultLocation) {
        Map<String, Object> defaults = null;

        try (InputStream stream = new URL(jarDefaultLocation).openStream()) {
            defaults = new Yaml().load(stream);

        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, Object> configsFromFile = new HashMap<>();
        File configFile = new File(configFileLocation);

        if (configFile.exists()) {
            try (FileInputStream input = new FileInputStream(configFile)) {
                configsFromFile = new Yaml().load(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (Map.Entry<String, Object> entry : configsFromFile.entrySet()) {

            assert defaults != null;
            if (defaults.containsKey(entry.getKey())) {
                defaults.put(entry.getKey(), entry.getValue());
            }
        }

        saveConfig(configFileLocation, defaults);

        return defaults;
    }


    // save config to file location
    public synchronized void saveConfig(String location, Map<String, Object> config) {
        try {
            Files.createDirectories(new File(location).getParentFile().toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (FileWriter writer = new FileWriter(location, StandardCharsets.UTF_8)) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml yaml = new Yaml(options);
            yaml.dump(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    abstract String getDefaultConfig();

    // get config entry
    public synchronized Object get(String key) {
        String[] keys = key.split("\\.");
        Map<String, Object> childConfig = config;
        for (int i = 0; i < keys.length - 1; i++) {
            Pattern pattern = Pattern.compile("([^\\[\\]]+)(?:\\[(\\\\d+)\\])?");
            Matcher matcher = pattern.matcher(keys[i]);
            if (matcher.find()) {
                String entry = matcher.group(1);
                String entryIndex = matcher.group(2);
                if (entryIndex != null) {
                    childConfig = (Map<String, Object>) ((List<Object>) childConfig.get(entry)).get(Integer.parseInt(entryIndex));
                }
                else {
                    childConfig = (Map<String, Object>) childConfig.get(entry);
                }
            } else {
                throw new IllegalStateException("Invalid config key: " + key);
            }
        }
        return childConfig.get(keys[keys.length - 1]);
    }

}

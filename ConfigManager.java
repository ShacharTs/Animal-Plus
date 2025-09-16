package net.AnimalPlus;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class ConfigManager {
    private final String APPDATA = System.getenv("APPDATA");
    private final String CONFIG_PATH = APPDATA + "\\.minecraft\\config\\animal_plus_config.json";
    private static Map<String, MobSettings> mobSettingsMap = new HashMap<>();

    public ConfigManager() {

    }

    public void saveConfig() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File(CONFIG_PATH);

        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(mobSettingsMap, writer);
            System.out.println("Saved config to: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadConfig() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type type = new TypeToken<Map<String, MobSettings>>() {
        }.getType();
        File file = new File(CONFIG_PATH);

        System.out.println("Loading config from: " + file.getAbsolutePath());

        try (FileReader reader = new FileReader(file)) {
            mobSettingsMap = gson.fromJson(reader, type);

            if (mobSettingsMap == null) {
                mobSettingsMap = new HashMap<>();
            }

            System.out.println("Loaded config: " + mobSettingsMap);

        } catch (FileNotFoundException e) {
            System.out.println("Config not found, creating new one...");
            mobSettingsMap = new HashMap<>();
            saveConfig();
        } catch (Exception e) {
            System.out.println("Error loading config, resetting...");
            e.printStackTrace();
            mobSettingsMap = new HashMap<>();
            saveConfig();
        }
    }


    public void setCoolDown(String mobName, CommandList.SubArgs subCommand, int cooldown) {
        MobSettings settings = mobSettingsMap.get(mobName);
        if (settings != null) {
            if (subCommand.equals(CommandList.SubArgs.AGE)) {
                settings.setAgeCooldown(cooldown);
            }
            if (subCommand.equals(CommandList.SubArgs.BREED)) {
                settings.setBreedCooldown(cooldown);
            }
        } else {
            addMobIfMissing(mobName);
            // Add new mob with the desired cooldown
            settings = new MobSettings(mobName, MobSettings.AGE_DEFAULT_CD, MobSettings.BREED_DEFAULT_CD);
            mobSettingsMap.put(mobName, settings);
            setCoolDown(mobName, subCommand, cooldown);
        }
        // Persist all changes to the config
        saveConfig();
    }

    public void setReset(String mobName, CommandList.SubArgs subCommand) {
        MobSettings settings = mobSettingsMap.get(mobName);
        if (settings != null) {
            if (subCommand.equals(CommandList.SubArgs.AGE)) {
                settings.resetAgeCD();
            }
            if (subCommand.equals(CommandList.SubArgs.BREED)) {
                settings.resetBreedCD();
            }
        } else {
            addMobIfMissing(mobName);
            // Add new mob with the desired cooldown
            settings = new MobSettings(mobName, MobSettings.AGE_DEFAULT_CD, MobSettings.BREED_DEFAULT_CD);
            mobSettingsMap.put(mobName, settings);
            setReset(mobName, subCommand);
        }
        // Persist all changes to the config
        saveConfig();
    }


    /**
     * Get the mob age cooldown from the config file, if not exist add it
     */
    public int getAgeCooldownOrAdd(String mobName) {
        MobSettings settings = mobSettingsMap.get(mobName);
        if (settings == null) {
            addMobIfMissing(mobName);
            settings = mobSettingsMap.get(mobName); // now guaranteed to exist
        }
        return settings.getAgeCooldown();
    }

    /**
     * Get the mob breed cooldown from the config file, if not exist add it
     */
    public int getBreedCooldownOrAdd(String mobName) {
        MobSettings settings = mobSettingsMap.get(mobName);
        if (settings == null) {
            addMobIfMissing(mobName);
            settings = mobSettingsMap.get(mobName); // now guaranteed to exist
        }
        return settings.getBreedCooldown();
    }


    /**
     * Add mob to the config file if not exist
     */
    public void addMobIfMissing(String mobName) {
        if (!mobSettingsMap.containsKey(mobName)) {
            // Create default MobSettings
            MobSettings settings = new MobSettings(mobName, MobSettings.AGE_DEFAULT_CD, MobSettings.BREED_DEFAULT_CD);
            mobSettingsMap.put(mobName, settings);

            // Persist to config file
            saveConfig();
            System.out.println("Added new mob to config: " + mobName);
        }
    }
}

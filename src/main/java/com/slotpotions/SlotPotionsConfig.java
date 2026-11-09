package com.slotpotions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Config file: config/slotpotions.json
 *
 * Slot numbers: 0-8 hotbar, 9-35 main inventory, 36 boots, 37 leggings, 38 chestplate, 39 helmet, 40 offhand.
 */
public final class SlotPotionsConfig {
    public static final int SLOT_COUNT = 41;

    private static final String[] DEFAULT_EFFECTS = {
            "minecraft:strength",
            "minecraft:speed",
            "minecraft:haste",
            "minecraft:resistance",
            "minecraft:regeneration",
            "minecraft:jump_boost",
            "minecraft:fire_resistance",
            "minecraft:night_vision",
            "minecraft:water_breathing",
            "minecraft:health_boost",
            "minecraft:absorption",
            "minecraft:saturation",
            "minecraft:luck",
            "minecraft:slow_falling",
            "minecraft:conduit_power",
            "minecraft:dolphins_grace",
            "minecraft:invisibility"
    };

    /** Highest effect level an item count can give (1-255). */
    public int maxLevel = 10;
    /** Slot number -> effect ID. Set a slot to "" to disable it. */
    public Map<String, String> slots = new LinkedHashMap<>();

    public static SlotPotionsConfig load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("slotpotions.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        SlotPotionsConfig loaded = null;
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                loaded = gson.fromJson(reader, SlotPotionsConfig.class);
            } catch (Exception e) {
                // Keep the user's file untouched, just run with defaults.
                SlotPotions.LOGGER.error("Could not read {}, using defaults", file, e);
            }
            if (loaded == null || loaded.slots == null) {
                loaded = defaults();
            }
        } else {
            loaded = defaults();
            try {
                Files.writeString(file, gson.toJson(loaded));
            } catch (IOException e) {
                SlotPotions.LOGGER.error("Could not write default config {}", file, e);
            }
        }

        loaded.maxLevel = Math.max(1, Math.min(255, loaded.maxLevel));
        return loaded;
    }

    private static SlotPotionsConfig defaults() {
        SlotPotionsConfig config = new SlotPotionsConfig();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            config.slots.put(String.valueOf(slot), DEFAULT_EFFECTS[slot % DEFAULT_EFFECTS.length]);
        }
        return config;
    }
}

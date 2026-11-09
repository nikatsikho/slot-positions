package com.slotpotions;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Every inventory slot (hotbar, inventory, armor, offhand) gives its own potion effect.
 * The number of items in the slot is the effect level. The effect lasts while the items stay in the slot.
 */
public class SlotPotions implements ModInitializer {
    public static final String MOD_ID = "slotpotions";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** How often (in ticks) inventories are checked. 5 ticks = 4 times per second. */
    private static final int CHECK_INTERVAL = 5;
    /** Duration given to each refresh. Long enough that night vision never flickers. */
    private static final int EFFECT_DURATION = 260;

    /** Effect for each slot index, resolved from the config. A null entry means "no effect". */
    private static final List<Holder<MobEffect>> SLOT_EFFECTS = new ArrayList<>();
    /** Effects this mod currently gives each player, and at which level. */
    private static final Map<UUID, Map<Holder<MobEffect>, Integer>> APPLIED = new HashMap<>();

    private static SlotPotionsConfig config;
    private static boolean resolved;
    private static int tickCounter;

    @Override
    public void onInitialize() {
        config = SlotPotionsConfig.load();
        ServerTickEvents.END_SERVER_TICK.register(SlotPotions::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> APPLIED.clear());
        LOGGER.info("Slot Potions loaded (max level {})", config.maxLevel);
    }

    private static void onServerTick(MinecraftServer server) {
        if (++tickCounter % CHECK_INTERVAL != 0) {
            return;
        }
        if (!resolved) {
            resolveEffects();
        }

        Set<UUID> online = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            online.add(player.getUUID());
            updatePlayer(player);
        }
        APPLIED.keySet().retainAll(online);
    }

    /** Looks the configured effect IDs up lazily, so effects added by other mods work too. */
    private static void resolveEffects() {
        SLOT_EFFECTS.clear();
        for (int slot = 0; slot < SlotPotionsConfig.SLOT_COUNT; slot++) {
            String id = config.slots.get(String.valueOf(slot));
            Holder<MobEffect> effect = null;
            if (id != null && !id.isBlank()) {
                effect = resolve(id);
                if (effect == null) {
                    LOGGER.warn("Slot {}: unknown effect '{}', slot will do nothing", slot, id);
                }
            }
            SLOT_EFFECTS.add(effect);
        }
        resolved = true;
    }

    private static Holder<MobEffect> resolve(String id) {
        Identifier key = Identifier.tryParse(id);
        if (key == null) {
            return null;
        }
        return BuiltInRegistries.MOB_EFFECT.getOptional(key)
                .map(BuiltInRegistries.MOB_EFFECT::wrapAsHolder)
                .orElse(null);
    }

    private static void updatePlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (!player.isAlive()) {
            APPLIED.remove(uuid);
            return;
        }

        // Add up the item counts of every slot that feeds the same effect.
        Map<Holder<MobEffect>, Integer> wanted = new HashMap<>();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < SlotPotionsConfig.SLOT_COUNT; slot++) {
            Holder<MobEffect> effect = SLOT_EFFECTS.get(slot);
            if (effect == null) {
                continue;
            }
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                wanted.merge(effect, stack.getCount(), Integer::sum);
            }
        }

        Map<Holder<MobEffect>, Integer> applied = APPLIED.computeIfAbsent(uuid, id -> new HashMap<>());

        // Remove effects we gave that are no longer wanted, or that need a lower level now.
        Iterator<Map.Entry<Holder<MobEffect>, Integer>> iterator = applied.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Holder<MobEffect>, Integer> entry = iterator.next();
            Integer count = wanted.get(entry.getKey());
            int newLevel = count == null ? 0 : Math.min(count, config.maxLevel);
            if (newLevel < entry.getValue()) {
                MobEffectInstance current = player.getEffect(entry.getKey());
                if (current != null && current.isAmbient()) {
                    player.removeEffect(entry.getKey());
                }
                iterator.remove();
            }
        }

        // Give or refresh everything that is wanted. Level N = amplifier N - 1.
        for (Map.Entry<Holder<MobEffect>, Integer> entry : wanted.entrySet()) {
            int level = Math.min(entry.getValue(), config.maxLevel);
            applied.put(entry.getKey(), level);
            player.addEffect(new MobEffectInstance(entry.getKey(), EFFECT_DURATION, level - 1, true, false, true));
        }
    }
}

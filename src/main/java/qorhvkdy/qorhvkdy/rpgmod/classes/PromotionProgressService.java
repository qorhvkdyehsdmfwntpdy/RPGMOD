package qorhvkdy.qorhvkdy.rpgmod.classes;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary progression storage for promotion requirements.
 * Quest/boss progress is in-memory for now, and can be replaced by persistent systems later.
 */
public final class PromotionProgressService {
    private static final Map<UUID, Set<String>> COMPLETED_QUESTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Integer>> BOSS_KILL_COUNTS = new ConcurrentHashMap<>();

    private PromotionProgressService() {
    }

    public static void markQuestCompleted(ServerPlayer player, String questId) {
        if (questId == null || questId.isBlank()) {
            return;
        }
        COMPLETED_QUESTS.computeIfAbsent(player.getUUID(), ignored -> ConcurrentHashMap.newKeySet())
                .add(questId.trim().toLowerCase());
    }

    public static boolean hasCompletedQuest(ServerPlayer player, String questId) {
        if (questId == null || questId.isBlank()) {
            return false;
        }
        return COMPLETED_QUESTS.getOrDefault(player.getUUID(), Set.of()).contains(questId.trim().toLowerCase());
    }

    public static void addBossKill(ServerPlayer player, String bossId, int amount) {
        if (bossId == null || bossId.isBlank() || amount <= 0) {
            return;
        }
        Map<String, Integer> kills = BOSS_KILL_COUNTS.computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>());
        String key = bossId.trim().toLowerCase();
        kills.put(key, kills.getOrDefault(key, 0) + amount);
    }

    public static int getBossKillCount(ServerPlayer player, String bossId) {
        if (bossId == null || bossId.isBlank()) {
            return 0;
        }
        return BOSS_KILL_COUNTS.getOrDefault(player.getUUID(), Map.of()).getOrDefault(bossId.trim().toLowerCase(), 0);
    }

    public static int countInventoryItem(ServerPlayer player, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return 0;
        }
        Identifier id = Identifier.tryParse(itemId.trim().toLowerCase());
        if (id == null) {
            return 0;
        }
        if (!ForgeRegistries.ITEMS.containsKey(id)) {
            return 0;
        }

        int total = 0;
        int size = player.getInventory().getContainerSize();
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            Identifier stackId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (stackId != null && stackId.equals(id)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}


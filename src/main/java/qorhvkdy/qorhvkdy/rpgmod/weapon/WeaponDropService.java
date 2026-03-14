package qorhvkdy.qorhvkdy.rpgmod.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qorhvkdy.qorhvkdy.rpgmod.debug.RpgDebugSettings;
import qorhvkdy.qorhvkdy.rpgmod.weapon.data.WeaponDropRuleJson;
import qorhvkdy.qorhvkdy.rpgmod.weapon.data.WeaponDropRuleRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 무기 드랍 + 옵션 랜덤 롤 서비스.
 */
public final class WeaponDropService {
    public static final String ITEM_DATA_ROOT = "rpgmod_weapon_roll";
    public static final String ITEM_DATA_AFFIX = "affix";
    public static final String ITEM_DATA_OPTION_TEXT = "option_text";
    public static final String ITEM_DATA_SOCKETS = "sockets";
    public static final String ITEM_DATA_SOCKET_MAX = "sockets_max";

    private static final Logger LOGGER = LoggerFactory.getLogger(WeaponDropService.class);

    private WeaponDropService() {
    }

    public static void bootstrap() {
        WeaponDropRuleRepository.bootstrap();
    }

    public static void reload() {
        WeaponDropRuleRepository.reload();
    }

    public static List<ItemStack> rollDrops(ServerPlayer killer, LivingEntity killed) {
        WeaponDropRuleJson rules = WeaponDropRuleRepository.get();
        if (!rules.enabled) {
            return List.of();
        }
        if (rules.playerKillOnly && killer == null) {
            return List.of();
        }

        List<WeaponDescriptor> candidates = WeaponDataService.dropCandidates();
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<ItemStack> result = new ArrayList<>();
        int maxDrops = Math.max(1, rules.maxDropsPerKill);
        for (int i = 0; i < maxDrops; i++) {
            Optional<WeaponDescriptor> selected = selectOne(candidates, killed.getMaxHealth(), rules);
            if (selected.isEmpty()) {
                continue;
            }
            ItemStack rolled = createRolledStack(selected.get(), rules);
            if (!rolled.isEmpty()) {
                result.add(rolled);
                if (RpgDebugSettings.dropVerboseLog()) {
                    LOGGER.debug("Rolled drop: {}", selected.get().itemId());
                }
            }
        }
        return result;
    }

    public static DropSimulationResult simulateDrops(int kills, double mobMaxHealth, boolean playerKill) {
        int safeKills = Math.max(1, kills);
        WeaponDropRuleJson rules = WeaponDropRuleRepository.get();
        if (!rules.enabled || (rules.playerKillOnly && !playerKill)) {
            return new DropSimulationResult(safeKills, 0, Map.of());
        }

        List<WeaponDescriptor> candidates = WeaponDataService.dropCandidates();
        if (candidates.isEmpty()) {
            return new DropSimulationResult(safeKills, 0, Map.of());
        }

        Map<String, Integer> dropCount = new LinkedHashMap<>();
        int totalDrops = 0;
        int maxDrops = Math.max(1, rules.maxDropsPerKill);
        for (int i = 0; i < safeKills; i++) {
            for (int j = 0; j < maxDrops; j++) {
                Optional<WeaponDescriptor> selected = selectOne(candidates, mobMaxHealth, rules);
                if (selected.isEmpty()) {
                    continue;
                }
                totalDrops++;
                dropCount.merge(selected.get().itemId(), 1, Integer::sum);
            }
        }

        Map<String, Integer> sorted = dropCount.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), LinkedHashMap::putAll);
        return new DropSimulationResult(safeKills, totalDrops, sorted);
    }

    private static Optional<WeaponDescriptor> selectOne(List<WeaponDescriptor> candidates, double mobMaxHealth, WeaponDropRuleJson rules) {
        List<WeightedCandidate> filtered = new ArrayList<>();
        double totalWeight = 0.0;
        for (WeaponDescriptor descriptor : candidates) {
            if (mobMaxHealth < descriptor.minMobMaxHealth()) {
                continue;
            }

            double rarityWeight = weight(rules.rarityWeight, descriptor.rarity(), 1.0);
            double gradeWeight = weight(rules.gradeWeight, descriptor.grade(), 1.0);
            double total = Math.max(0.0, descriptor.dropWeight() * rarityWeight * gradeWeight);
            if (total <= 0.0) {
                continue;
            }
            filtered.add(new WeightedCandidate(descriptor, total));
            totalWeight += total;
        }
        if (filtered.isEmpty() || totalWeight <= 0.0) {
            return Optional.empty();
        }

        double pointer = ThreadLocalRandom.current().nextDouble() * totalWeight;
        for (WeightedCandidate candidate : filtered) {
            pointer -= candidate.weight;
            if (pointer <= 0.0) {
                double chance = clamp01(rules.globalDropChance) * clamp01(candidate.descriptor.baseDropChance());
                if (ThreadLocalRandom.current().nextDouble() <= chance) {
                    return Optional.of(candidate.descriptor);
                }
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static ItemStack createRolledStack(WeaponDescriptor descriptor, WeaponDropRuleJson rules) {
        Identifier id = Identifier.tryParse(descriptor.itemId());
        if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);

        RollResult rolled = rollResult(descriptor, rules.optionRollMin, rules.optionRollMax);
        String name = descriptor.displayNameComponent().getString();
        if (!rolled.affixDisplay.isBlank()) {
            name = rolled.affixType.equals("suffix")
                    ? (name + " " + rolled.affixDisplay)
                    : (rolled.affixDisplay + " " + name);
        }
        if (!rolled.optionText.isBlank()) {
            name = name + " [" + rolled.optionText + "]";
        }
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        writeRollData(stack, rolled);
        return stack;
    }

    private static RollResult rollResult(WeaponDescriptor descriptor, double minFactor, double maxFactor) {
        WeaponAffixDescriptor affix = rollAffix(descriptor.affixPool());
        double affixOptionMultiplier = affix == null ? 1.0 : Math.max(0.1, affix.optionMultiplier());

        String optionText = "";
        if (!descriptor.options().isEmpty()) {
            WeaponOptionDescriptor option = descriptor.options().get(ThreadLocalRandom.current().nextInt(descriptor.options().size()));
            optionText = rollOptionText(option, minFactor, maxFactor, affixOptionMultiplier);
        }
        int maxSockets = Math.max(0, descriptor.socket().maxSockets());
        int sockets = maxSockets <= 0 ? 0 : ThreadLocalRandom.current().nextInt(
                Math.max(0, descriptor.socket().minSockets()),
                maxSockets + 1
        );
        return new RollResult(
                affix == null ? "" : affix.type(),
                affix == null ? "" : affix.id(),
                affix == null ? "" : affix.display(),
                optionText,
                sockets,
                maxSockets
        );
    }

    private static WeaponAffixDescriptor rollAffix(List<WeaponAffixDescriptor> pool) {
        if (pool == null || pool.isEmpty()) {
            return null;
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private static String rollOptionText(WeaponOptionDescriptor option, double minFactor, double maxFactor, double extraMultiplier) {
        double clampedMin = Math.max(0.1, Math.min(minFactor, maxFactor));
        double clampedMax = Math.max(clampedMin, maxFactor);
        double factor = ThreadLocalRandom.current().nextDouble(clampedMin, clampedMax);
        double rolled = option.value() * factor * Math.max(0.1, extraMultiplier);

        String stat = option.stat().isBlank() ? "option" : option.stat();
        if ("percent".equalsIgnoreCase(option.mode()) || "pct".equalsIgnoreCase(option.mode())) {
            return stat + "+" + round2(rolled) + "%";
        }
        return stat + "+" + round2(rolled);
    }

    private static void writeRollData(ItemStack stack, RollResult rolled) {
        CompoundTag root = new CompoundTag();
        root.putString(ITEM_DATA_AFFIX, rolled.affixDisplay);
        root.putString(ITEM_DATA_OPTION_TEXT, rolled.optionText);
        root.putInt(ITEM_DATA_SOCKETS, rolled.sockets);
        root.putInt(ITEM_DATA_SOCKET_MAX, rolled.maxSockets);
        root.putString("affix_type", rolled.affixType);
        root.putString("affix_id", rolled.affixId);

        CompoundTag wrapper = new CompoundTag();
        wrapper.put(ITEM_DATA_ROOT, root);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(wrapper));
    }

    public static RollInfo readRollInfo(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return RollInfo.empty();
        }
        CompoundTag wrapper = customData.copyTag();
        CompoundTag root = wrapper.getCompound(ITEM_DATA_ROOT).orElse(null);
        if (root == null) {
            return RollInfo.empty();
        }
        return new RollInfo(
                root.getString(ITEM_DATA_AFFIX).orElse(""),
                root.getString(ITEM_DATA_OPTION_TEXT).orElse(""),
                root.getInt(ITEM_DATA_SOCKETS).orElse(0),
                root.getInt(ITEM_DATA_SOCKET_MAX).orElse(0)
        );
    }

    private static String round2(double value) {
        return String.valueOf(Math.round(value * 100.0) / 100.0);
    }

    private static double weight(java.util.Map<String, Double> table, String key, double fallback) {
        if (table == null) {
            return fallback;
        }
        if (key == null) {
            return fallback;
        }
        return table.getOrDefault(key.trim().toLowerCase(), fallback);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private record WeightedCandidate(WeaponDescriptor descriptor, double weight) {
    }

    private record RollResult(
            String affixType,
            String affixId,
            String affixDisplay,
            String optionText,
            int sockets,
            int maxSockets
    ) {
    }

    public record DropSimulationResult(int kills, int totalDrops, Map<String, Integer> byItemId) {
        public double dropRatePerKill() {
            return kills <= 0 ? 0.0 : (double) totalDrops / kills;
        }
    }

    public record RollInfo(String affixDisplay, String optionText, int sockets, int socketMax) {
        public static RollInfo empty() {
            return new RollInfo("", "", 0, 0);
        }
    }
}

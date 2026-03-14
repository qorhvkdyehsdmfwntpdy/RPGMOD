package qorhvkdy.qorhvkdy.rpgmod.combat.profile;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import qorhvkdy.qorhvkdy.rpgmod.combat.profile.data.CombatProfileJson;
import qorhvkdy.qorhvkdy.rpgmod.combat.profile.data.CombatProfileRepository;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 무기 전투 프로필 서비스.
 */
public final class CombatProfileService {
    private static volatile Map<String, CombatProfile> profiles = Map.of();

    private CombatProfileService() {
    }

    public static synchronized void bootstrap() {
        CombatProfileRepository.bootstrap();
        reload();
    }

    public static synchronized void reload() {
        CombatProfileRepository.reload();
        LinkedHashMap<String, CombatProfile> next = new LinkedHashMap<>();
        for (CombatProfileJson.Entry entry : CombatProfileRepository.get().profiles) {
            String itemId = normalize(entry.itemId);
            if (itemId.isBlank()) {
                continue;
            }
            next.put(itemId, new CombatProfile(
                    itemId,
                    Math.max(1, entry.comboWindowTicks),
                    Math.max(1, entry.recoveryTicks),
                    Math.max(0.1, entry.damageMultiplier)
            ));
        }
        profiles = Map.copyOf(next);
    }

    public static Optional<CombatProfile> find(ItemStack stack) {
        Identifier id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(profiles.get(normalize(id.toString())));
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    public record CombatProfile(String itemId, int comboWindowTicks, int recoveryTicks, double damageMultiplier) {
    }
}

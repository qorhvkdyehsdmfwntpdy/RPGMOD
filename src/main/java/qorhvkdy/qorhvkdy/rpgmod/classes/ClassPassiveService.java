package qorhvkdy.qorhvkdy.rpgmod.classes;

import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsHistoryService;

import java.util.List;
import java.util.Map;

/**
 * Default passive hook holder.
 * This maps class/advancement transitions to passive slot suggestions.
 */
public final class ClassPassiveService implements ClassLifecycleHook {
    public static final ClassPassiveService INSTANCE = new ClassPassiveService();

    // Slot names are intentionally stable for future UI/edit APIs.
    public static final List<String> DEFAULT_SLOTS = List.of("core", "offense", "utility");

    private ClassPassiveService() {
    }

    @Override
    public void onClassChanged(ServerPlayer player, PlayerStats stats, PlayerClassType before, PlayerClassType after, ClassAdvancement beforeAdv, ClassAdvancement afterAdv) {
        Map<String, String> suggested = suggestPassives(afterAdv);
        stats.applyPassiveSlots(suggested);
        StatsHistoryService.log(player, "Passive loadout reset suggested: " + suggested);
    }

    @Override
    public void onPromotion(ServerPlayer player, PlayerStats stats, ClassAdvancement before, ClassAdvancement after) {
        Map<String, String> suggested = suggestPassives(after);
        stats.applyPassiveSlots(suggested);
        StatsHistoryService.log(player, "Passive loadout updated suggested: " + suggested);
    }

    public Map<String, String> suggestPassives(ClassAdvancement advancement) {
        // 한글 주석: JSON 템플릿이 있으면 우선 적용하고, 없으면 자동 생성 규칙으로 폴백한다.
        Map<String, String> template = ClassPassiveTemplateService.resolveForAdvancement(advancement.id());
        if (!template.isEmpty()) {
            return template;
        }

        // Placeholder mapping. Replace ids with real passive ids when implemented.
        String key = advancement.baseClass().id();
        return Map.of(
                "core", key + "_core_" + advancement.tier().name().toLowerCase(),
                "offense", key + "_offense_" + advancement.tier().name().toLowerCase(),
                "utility", key + "_utility_" + advancement.tier().name().toLowerCase()
        );
    }
}

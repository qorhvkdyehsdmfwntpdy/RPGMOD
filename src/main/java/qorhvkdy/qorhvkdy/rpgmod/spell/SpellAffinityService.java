package qorhvkdy.qorhvkdy.rpgmod.spell;

import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatType;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsUtil;
import qorhvkdy.qorhvkdy.rpgmod.spell.data.SpellSchoolJson;
import qorhvkdy.qorhvkdy.rpgmod.spell.data.SpellSchoolRepository;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 원소/학파 친화도 계산 서비스.
 */
public final class SpellAffinityService {
    private SpellAffinityService() {
    }

    public static void bootstrap() {
        SpellSchoolRepository.bootstrap();
    }

    public static void reload() {
        SpellSchoolRepository.reload();
    }

    public static double affinity(ServerPlayer player, String school) {
        PlayerStats stats = StatsUtil.get(player);
        SpellSchoolJson.Entry entry = SpellSchoolRepository.get().schools.getOrDefault(
                normalize(school),
                new SpellSchoolJson.Entry()
        );
        double wis = stats.get(StatType.WIS);
        double luk = stats.get(StatType.LUK);
        return Math.max(0.1, entry.base + (wis * entry.wisScale / 100.0) + (luk * entry.lukScale / 100.0));
    }

    public static Map<String, Double> allAffinities(ServerPlayer player) {
        LinkedHashMap<String, Double> out = new LinkedHashMap<>();
        for (String school : SpellSchoolRepository.get().schools.keySet()) {
            out.put(school, affinity(player, school));
        }
        return out;
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}

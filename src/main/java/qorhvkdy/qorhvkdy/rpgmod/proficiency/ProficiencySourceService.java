package qorhvkdy.qorhvkdy.rpgmod.proficiency;

import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.data.ProficiencySourceJson;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.data.ProficiencySourceRepository;

import java.util.Locale;

/**
 * sourceId 기반 숙련도 경험치 지급 서비스.
 */
public final class ProficiencySourceService {
    private ProficiencySourceService() {
    }

    public static void bootstrap() {
        ProficiencySourceRepository.bootstrap();
    }

    public static void reload() {
        ProficiencySourceRepository.reload();
    }

    public static int grantBySource(ServerPlayer player, String sourceId, double externalMultiplier) {
        String key = normalize(sourceId);
        ProficiencySourceJson.SourceEntry source = ProficiencySourceRepository.get().sources.get(key);
        if (source == null) {
            return 0;
        }
        ProficiencyType type = ProficiencyType.fromKey(source.proficiencyType).orElse(null);
        if (type == null) {
            return 0;
        }
        int amount = Math.max(0, source.amount);
        double totalMultiplier = Math.max(0.0, source.multiplier) * Math.max(0.0, externalMultiplier);
        int finalAmount = (int) Math.round(amount * totalMultiplier);
        if (finalAmount <= 0) {
            return 0;
        }
        return ProficiencyProgressionService.grant(player, type, finalAmount);
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}

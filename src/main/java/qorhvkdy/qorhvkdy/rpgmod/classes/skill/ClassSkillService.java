package qorhvkdy.qorhvkdy.rpgmod.classes.skill;

import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.classes.skill.data.ClassSkillJson;
import qorhvkdy.qorhvkdy.rpgmod.classes.skill.data.ClassSkillRepository;
import qorhvkdy.qorhvkdy.rpgmod.classes.resource.ClassResourceService;
import qorhvkdy.qorhvkdy.rpgmod.passive.PassiveBonus;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 클래스 액티브 스킬 서비스.
 */
public final class ClassSkillService {
    private static volatile Map<String, ClassSkillDescriptor> skills = Map.of();
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, List<ActiveEffect>> ACTIVE_EFFECTS = new ConcurrentHashMap<>();

    private record ActiveEffect(String skillId, long expiresAtMs, PassiveBonus bonus) {
    }

    public record CastResult(boolean success, String message) {
        public static CastResult ok(String msg) {
            return new CastResult(true, msg);
        }

        public static CastResult fail(String msg) {
            return new CastResult(false, msg);
        }
    }

    private ClassSkillService() {
    }

    public static void bootstrap() {
        ClassSkillRepository.bootstrap();
        reload();
    }

    public static synchronized void reload() {
        ClassSkillRepository.reload();
        ClassSkillJson json = ClassSkillRepository.get();
        LinkedHashMap<String, ClassSkillDescriptor> next = new LinkedHashMap<>();
        for (ClassSkillJson.Entry entry : json.skills) {
            String id = normalize(entry.id);
            if (id.isBlank()) {
                continue;
            }
            next.put(id, new ClassSkillDescriptor(
                    id,
                    entry.displayName,
                    normalize(entry.requiredAdvancementId),
                    Math.max(1, entry.requiredLevel),
                    Math.max(0, entry.minStr),
                    Math.max(0, entry.minAgi),
                    Math.max(0, entry.minWis),
                    Math.max(0, entry.minLuk),
                    Math.max(0.0, entry.resourceCost),
                    (long) (Math.max(0.1, entry.cooldownSeconds) * 1000L),
                    Math.max(1, entry.durationTicks),
                    Math.max(1.0, entry.bonusAttackMultiplier),
                    Math.max(0.0, entry.bonusArmor),
                    Math.max(0.0, entry.bonusCritChance),
                    Math.max(0.0, entry.bonusCritDamage)
            ));
        }
        skills = Map.copyOf(next);
    }

    public static List<ClassSkillDescriptor> unlocked(ServerPlayer player, PlayerStats stats) {
        return unlockedFor(player.experienceLevel, stats);
    }

    /**
     * 레벨/스탯/전직 기준으로 해금된 액티브 스킬 목록을 계산한다.
     * 클라이언트 GUI에서도 재사용할 수 있도록 ServerPlayer 의존을 제거한 버전.
     */
    public static List<ClassSkillDescriptor> unlockedFor(int level, PlayerStats stats) {
        List<ClassSkillDescriptor> list = new ArrayList<>();
        String currentAdv = normalize(stats.getCurrentAdvancementId());
        for (ClassSkillDescriptor skill : skills.values()) {
            if (level < skill.requiredLevel()) {
                continue;
            }
            if (!skill.requiredAdvancementId().isBlank() && !currentAdv.equals(skill.requiredAdvancementId())) {
                continue;
            }
            if (stats.get(StatType.STR) < skill.minStr()) {
                continue;
            }
            if (stats.get(StatType.AGI) < skill.minAgi()) {
                continue;
            }
            if (stats.get(StatType.WIS) < skill.minWis()) {
                continue;
            }
            if (stats.get(StatType.LUK) < skill.minLuk()) {
                continue;
            }
            list.add(skill);
        }
        list.sort(Comparator.comparing(ClassSkillDescriptor::id));
        return list;
    }

    public static CastResult cast(ServerPlayer player, PlayerStats stats, String skillIdRaw) {
        clearExpired(player.getUUID());

        String skillId = normalize(skillIdRaw);
        ClassSkillDescriptor skill = skills.get(skillId);
        if (skill == null) {
            return CastResult.fail("Unknown skill: " + skillIdRaw);
        }
        if (unlocked(player, stats).stream().noneMatch(s -> s.id().equals(skill.id()))) {
            return CastResult.fail("Skill is locked: " + skill.id());
        }

        long now = System.currentTimeMillis();
        long remain = remainingCooldownMs(player.getUUID(), skill.id(), now);
        if (remain > 0) {
            return CastResult.fail("Cooldown: " + round1(remain / 1000.0) + "s");
        }

        if (!ClassResourceService.consume(stats, skill.resourceCost())) {
            return CastResult.fail("Not enough resource.");
        }

        COOLDOWNS.computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>())
                .put(skill.id(), now + skill.cooldownMs());
        long expireAt = now + (skill.durationTicks() * 50L);
        ActiveEffect effect = new ActiveEffect(
                skill.id(),
                expireAt,
                new PassiveBonus(
                        1.0,
                        skill.bonusAttackMultiplier(),
                        0.0,
                        skill.bonusArmor(),
                        skill.bonusCritChance(),
                        skill.bonusCritDamage()
                )
        );
        ACTIVE_EFFECTS.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>()).add(effect);
        return CastResult.ok("Skill cast: " + skill.id());
    }

    public static PassiveBonus activeBonus(ServerPlayer player) {
        clearExpired(player.getUUID());
        List<ActiveEffect> effects = ACTIVE_EFFECTS.getOrDefault(player.getUUID(), List.of());
        PassiveBonus bonus = PassiveBonus.none();
        for (ActiveEffect effect : effects) {
            bonus = bonus.combine(effect.bonus());
        }
        return bonus;
    }

    public static double remainingCooldownSeconds(ServerPlayer player, String skillId) {
        clearExpired(player.getUUID());
        long remain = remainingCooldownMs(player.getUUID(), normalize(skillId), System.currentTimeMillis());
        return remain <= 0 ? 0.0 : remain / 1000.0;
    }

    public static Optional<ClassSkillDescriptor> find(String idRaw) {
        return Optional.ofNullable(skills.get(normalize(idRaw)));
    }

    private static long remainingCooldownMs(UUID playerId, String skillId, long now) {
        long end = COOLDOWNS.getOrDefault(playerId, Map.of()).getOrDefault(skillId, 0L);
        return Math.max(0L, end - now);
    }

    private static void clearExpired(UUID playerId) {
        long now = System.currentTimeMillis();
        List<ActiveEffect> effects = ACTIVE_EFFECTS.get(playerId);
        if (effects != null) {
            effects.removeIf(effect -> effect.expiresAtMs() <= now);
            if (effects.isEmpty()) {
                ACTIVE_EFFECTS.remove(playerId);
            }
        }
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String round1(double value) {
        return String.valueOf(Math.round(value * 10.0) / 10.0);
    }
}

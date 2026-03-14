package qorhvkdy.qorhvkdy.rpgmod.classes.resource;

import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.classes.PlayerClassType;
import qorhvkdy.qorhvkdy.rpgmod.classes.resource.data.ClassResourceProfileJson;
import qorhvkdy.qorhvkdy.rpgmod.classes.resource.data.ClassResourceProfileRepository;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 클래스 전투 리소스 서비스.
 */
public final class ClassResourceService {
    private static volatile Map<String, ClassResourceProfile> profiles = Map.of();

    private ClassResourceService() {
    }

    public static void bootstrap() {
        ClassResourceProfileRepository.bootstrap();
        reload();
    }

    public static synchronized void reload() {
        ClassResourceProfileRepository.reload();
        ClassResourceProfileJson json = ClassResourceProfileRepository.get();
        LinkedHashMap<String, ClassResourceProfile> next = new LinkedHashMap<>();
        for (var entry : json.profiles.entrySet()) {
            String classId = normalize(entry.getKey());
            ClassResourceProfileJson.Profile p = entry.getValue();
            if (classId.isBlank() || p == null) {
                continue;
            }
            next.put(classId, new ClassResourceProfile(
                    normalize(p.resourceType),
                    Math.max(0.0, p.maxBase),
                    Math.max(0.0, p.maxPerLevel),
                    Math.max(0.0, p.regenPerSecond),
                    Math.max(0.0, p.gainOnHit),
                    Math.max(0.0, p.gainOnCrit),
                    Math.max(0.0, p.gainOnKill)
            ));
        }
        next.putIfAbsent("none", ClassResourceProfile.none());
        profiles = Map.copyOf(next);
    }

    public static ClassResourceProfile profileFor(PlayerClassType type) {
        return profiles.getOrDefault(normalize(type == null ? "none" : type.id()), ClassResourceProfile.none());
    }

    public static ClassResourceProfile profileFor(PlayerStats stats) {
        return profileFor(stats.getSelectedClass());
    }

    /**
     * 직업/레벨 기반 자원 프로파일을 적용한다.
     * 동기화는 호출자가 제어해 같은 틱의 중복 패킷 전송을 줄인다.
     *
     * @return 자원 타입/최대치/현재치 중 하나라도 실제로 바뀌었으면 {@code true}
     */
    public static boolean updateProfile(ServerPlayer player, PlayerStats stats, boolean refill) {
        String beforeType = stats.getClassResourceType();
        double beforeCurrent = stats.getClassResourceCurrent();
        double beforeMax = stats.getClassResourceMax();

        ClassResourceProfile profile = profileFor(stats);
        double max = profile.maxBase() + (profile.maxPerLevel() * Math.max(0, player.experienceLevel));
        stats.setClassResourceType(profile.resourceType());
        stats.setClassResourceMax(max);
        if (refill || !beforeType.equalsIgnoreCase(profile.resourceType())) {
            stats.setClassResourceCurrent(max);
        }

        return !beforeType.equalsIgnoreCase(stats.getClassResourceType())
                || Double.compare(beforeCurrent, stats.getClassResourceCurrent()) != 0
                || Double.compare(beforeMax, stats.getClassResourceMax()) != 0;
    }

    public static void syncProfile(ServerPlayer player, PlayerStats stats, boolean refill) {
        updateProfile(player, stats, refill);
        ModNetwork.syncToPlayer(player, stats);
    }

    public static void regenerate(ServerPlayer player, PlayerStats stats) {
        ClassResourceProfile profile = profileFor(stats);
        if (profile.regenPerSecond() <= 0.0 || stats.getClassResourceMax() <= 0.0) {
            return;
        }
        double before = stats.getClassResourceCurrent();
        stats.gainClassResource(profile.regenPerSecond());
        if (Double.compare(before, stats.getClassResourceCurrent()) != 0) {
            ModNetwork.syncToPlayer(player, stats);
        }
    }

    public static boolean consume(PlayerStats stats, double amount) {
        if (amount <= 0.0) {
            return true;
        }
        return stats.consumeClassResource(amount);
    }

    public static void onHit(ServerPlayer attacker, PlayerStats stats, boolean critical, boolean killedTarget) {
        ClassResourceProfile profile = profileFor(stats);
        if (stats.getClassResourceMax() <= 0.0) {
            return;
        }
        double gain = profile.gainOnHit();
        if (critical) {
            gain += profile.gainOnCrit();
        }
        if (killedTarget) {
            gain += profile.gainOnKill();
        }
        if (gain <= 0.0) {
            return;
        }
        double before = stats.getClassResourceCurrent();
        stats.gainClassResource(gain);
        if (Double.compare(before, stats.getClassResourceCurrent()) != 0) {
            ModNetwork.syncToPlayer(attacker, stats);
        }
    }

    private static String normalize(String value) {
        return value == null ? "none" : value.trim().toLowerCase(Locale.ROOT);
    }
}

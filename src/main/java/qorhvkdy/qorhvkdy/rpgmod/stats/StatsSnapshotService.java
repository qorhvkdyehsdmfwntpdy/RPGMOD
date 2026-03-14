package qorhvkdy.qorhvkdy.rpgmod.stats;

/*
 * [RPGMOD 파일 설명]
 * 역할: 스탯 스냅샷 저장/복구(롤백) 기능을 제공합니다.
 * 수정 예시: 롤백 단계 수를 늘리려면 스냅샷 큐 최대 크기를 올립니다.
 */


import net.minecraft.server.level.ServerPlayer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StatsSnapshotService {
    public record Snapshot(Map<StatType, Integer> values, int points, int trackedLevel, String reason, String timestamp) {
    }

    private static final int MAX_SNAPSHOTS = 50;
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<UUID, ArrayDeque<Snapshot>> SNAPSHOTS = new ConcurrentHashMap<>();

    private StatsSnapshotService() {
    }

    public static void snapshot(ServerPlayer player, PlayerStats stats, String reason) {
        EnumMap<StatType, Integer> copy = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) {
            copy.put(type, stats.get(type));
        }
        Snapshot snapshot = new Snapshot(copy, stats.getAvailableStatPoints(), stats.getLastTrackedLevel(), reason, TS_FORMAT.format(LocalDateTime.now()));
        ArrayDeque<Snapshot> queue = SNAPSHOTS.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>());
        synchronized (queue) {
            queue.addLast(snapshot);
            while (queue.size() > MAX_SNAPSHOTS) {
                queue.removeFirst();
            }
        }
    }

    public static Optional<Snapshot> popLast(ServerPlayer player) {
        ArrayDeque<Snapshot> queue = SNAPSHOTS.get(player.getUUID());
        if (queue == null) {
            return Optional.empty();
        }
        synchronized (queue) {
            return Optional.ofNullable(queue.pollLast());
        }
    }

    public static int count(ServerPlayer player) {
        ArrayDeque<Snapshot> queue = SNAPSHOTS.get(player.getUUID());
        return queue == null ? 0 : queue.size();
    }
}

package qorhvkdy.qorhvkdy.rpgmod.stats;

/*
 * [RPGMOD 파일 설명]
 * 역할: 운영자용 스탯 변경 로그 기록/조회 서비스를 제공합니다.
 * 수정 예시: 로그 보존 길이를 늘리려면 내부 최대 저장 개수를 상향합니다.
 */


import net.minecraft.server.level.ServerPlayer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StatsHistoryService {
    private static final int MAX_LOG_SIZE = 200;
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<UUID, ArrayDeque<String>> LOGS = new ConcurrentHashMap<>();

    private StatsHistoryService() {
    }

    public static void log(ServerPlayer player, String action) {
        log(player.getUUID(), action);
    }

    public static void log(UUID playerId, String action) {
        ArrayDeque<String> queue = LOGS.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        synchronized (queue) {
            queue.addLast("[" + TS_FORMAT.format(LocalDateTime.now()) + "] " + action);
            while (queue.size() > MAX_LOG_SIZE) {
                queue.removeFirst();
            }
        }
    }

    public static List<String> getRecent(UUID playerId, int limit) {
        ArrayDeque<String> queue = LOGS.get(playerId);
        if (queue == null || queue.isEmpty()) {
            return List.of();
        }

        int size = Math.max(1, Math.min(100, limit));
        List<String> result = new ArrayList<>(size);
        synchronized (queue) {
            int skip = Math.max(0, queue.size() - size);
            int index = 0;
            for (String line : queue) {
                if (index++ >= skip) {
                    result.add(line);
                }
            }
        }
        return result;
    }
}

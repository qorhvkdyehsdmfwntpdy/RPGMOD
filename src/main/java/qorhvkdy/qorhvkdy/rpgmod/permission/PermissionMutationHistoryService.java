package qorhvkdy.qorhvkdy.rpgmod.permission;

import qorhvkdy.qorhvkdy.rpgmod.permission.data.PermissionRepository;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 권한 설정 변경 이력(메모리) 관리.
 * LuckPerms의 대형 운영 흐름을 참고해, 빠른 롤백을 위한 undo 스택을 제공한다.
 */
public final class PermissionMutationHistoryService {
    private static final int MAX_HISTORY = 50;
    private static final Deque<Entry> HISTORY = new ArrayDeque<>();

    private PermissionMutationHistoryService() {
    }

    public static synchronized void pushSnapshot(String reason) {
        HISTORY.push(new Entry(System.currentTimeMillis(), reason == null ? "mutation" : reason, PermissionRepository.snapshotJson()));
        while (HISTORY.size() > MAX_HISTORY) {
            HISTORY.removeLast();
        }
    }

    public static synchronized boolean undoLast() {
        Entry entry = HISTORY.pollFirst();
        if (entry == null) {
            return false;
        }
        return PermissionRepository.restoreFromJson(entry.rawJson);
    }

    public static synchronized int size() {
        return HISTORY.size();
    }

    private record Entry(long timestampMs, String reason, String rawJson) {
    }
}

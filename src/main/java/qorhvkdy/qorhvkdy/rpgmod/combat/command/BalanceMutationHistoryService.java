package qorhvkdy.qorhvkdy.rpgmod.combat.command;

import qorhvkdy.qorhvkdy.rpgmod.classes.resource.data.ClassResourceProfileRepository;
import qorhvkdy.qorhvkdy.rpgmod.classes.skill.data.ClassSkillRepository;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 클래스 밸런스 변경 이력.
 * 운영 중 실수 복구를 위해 직전 스냅샷을 되돌릴 수 있다.
 */
public final class BalanceMutationHistoryService {
    private static final int MAX_HISTORY = 30;
    private static final Deque<Entry> HISTORY = new ArrayDeque<>();

    private BalanceMutationHistoryService() {
    }

    public static synchronized void pushSnapshot(String reason) {
        HISTORY.push(new Entry(
                System.currentTimeMillis(),
                reason == null ? "balance_change" : reason,
                ClassSkillRepository.snapshotJson(),
                ClassResourceProfileRepository.snapshotJson()
        ));
        while (HISTORY.size() > MAX_HISTORY) {
            HISTORY.removeLast();
        }
    }

    public static synchronized boolean undo() {
        Entry entry = HISTORY.pollFirst();
        if (entry == null) {
            return false;
        }
        boolean a = ClassSkillRepository.restoreFromJson(entry.skillJson);
        boolean b = ClassResourceProfileRepository.restoreFromJson(entry.resourceJson);
        return a && b;
    }

    public static synchronized int size() {
        return HISTORY.size();
    }

    private record Entry(long timestampMs, String reason, String skillJson, String resourceJson) {
    }
}

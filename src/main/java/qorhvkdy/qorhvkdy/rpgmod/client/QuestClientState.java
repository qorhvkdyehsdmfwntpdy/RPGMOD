package qorhvkdy.qorhvkdy.rpgmod.client;

import java.util.List;

/**
 * 클라이언트 퀘스트 상태 스냅샷.
 */
public final class QuestClientState {
    private static volatile Snapshot snapshot = Snapshot.empty();
    private static volatile String pinnedQuestId = "";

    private QuestClientState() {
    }

    public static Snapshot get() {
        return snapshot;
    }

    public static void apply(Snapshot next) {
        snapshot = next == null ? Snapshot.empty() : next;
    }

    public static String pinnedQuestId() {
        return pinnedQuestId;
    }

    public static void clearPinnedQuest() {
        pinnedQuestId = "";
    }

    public static void pinNextAccepted() {
        List<String> accepted = snapshot.accepted();
        if (accepted.isEmpty()) {
            pinnedQuestId = "";
            return;
        }
        int current = -1;
        for (int i = 0; i < accepted.size(); i++) {
            if (extractQuestId(accepted.get(i)).equalsIgnoreCase(pinnedQuestId)) {
                current = i;
                break;
            }
        }
        int next = (current + 1) % accepted.size();
        pinnedQuestId = extractQuestId(accepted.get(next));
    }

    private static String extractQuestId(String line) {
        if (line == null || line.isBlank()) {
            return "";
        }
        int idx = line.indexOf(" (");
        if (idx > 0) {
            return line.substring(0, idx).trim();
        }
        return line.trim();
    }

    public record Snapshot(List<String> available, List<String> accepted, List<String> completed, List<String> objectiveLines, List<String> rewardPreviewLines) {
        public Snapshot {
            available = available == null ? List.of() : List.copyOf(available);
            accepted = accepted == null ? List.of() : List.copyOf(accepted);
            completed = completed == null ? List.of() : List.copyOf(completed);
            objectiveLines = objectiveLines == null ? List.of() : List.copyOf(objectiveLines);
            rewardPreviewLines = rewardPreviewLines == null ? List.of() : List.copyOf(rewardPreviewLines);
        }

        public static Snapshot empty() {
            return new Snapshot(List.of(), List.of(), List.of(), List.of(), List.of());
        }
    }
}

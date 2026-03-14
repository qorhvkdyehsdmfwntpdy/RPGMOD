package qorhvkdy.qorhvkdy.rpgmod.quest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 플레이어 퀘스트 진행도(수락/완료) 데이터.
 */
public final class PlayerQuestProgress {
    private final LinkedHashSet<String> accepted = new LinkedHashSet<>();
    private final LinkedHashSet<String> completed = new LinkedHashSet<>();
    private final LinkedHashMap<String, LinkedHashMap<String, Integer>> objectiveProgress = new LinkedHashMap<>();
    private final LinkedHashMap<String, Long> completedAtEpochSec = new LinkedHashMap<>();
    private final LinkedHashMap<String, Long> failedAtEpochSec = new LinkedHashMap<>();

    public boolean accept(String questId) {
        String id = normalize(questId);
        return accepted.add(id);
    }

    public boolean unaccept(String questId) {
        return accepted.remove(normalize(questId));
    }

    public boolean complete(String questId) {
        String id = normalize(questId);
        boolean changed = completed.add(id);
        completedAtEpochSec.put(id, System.currentTimeMillis() / 1000L);
        return changed;
    }

    public boolean uncomplete(String questId) {
        return completed.remove(normalize(questId));
    }

    public boolean isAccepted(String questId) {
        return accepted.contains(normalize(questId));
    }

    public boolean isCompleted(String questId) {
        return completed.contains(normalize(questId));
    }

    public List<String> acceptedList() {
        return new ArrayList<>(accepted);
    }

    public List<String> completedList() {
        return new ArrayList<>(completed);
    }

    public long completedAt(String questId) {
        return completedAtEpochSec.getOrDefault(normalize(questId), 0L);
    }

    public void clearCompletedAt(String questId) {
        completedAtEpochSec.remove(normalize(questId));
    }

    public void clearQuestState(String questId) {
        String id = normalize(questId);
        accepted.remove(id);
        objectiveProgress.remove(id);
    }

    public void markFailed(String questId) {
        String id = normalize(questId);
        failedAtEpochSec.put(id, System.currentTimeMillis() / 1000L);
    }

    public long failedAt(String questId) {
        return failedAtEpochSec.getOrDefault(normalize(questId), 0L);
    }

    public void clearFailedAt(String questId) {
        failedAtEpochSec.remove(normalize(questId));
    }

    public void initObjective(String questId, String objectiveKey) {
        String q = normalize(questId);
        String o = normalize(objectiveKey);
        if (q.isBlank() || o.isBlank()) {
            return;
        }
        objectiveProgress.computeIfAbsent(q, ignored -> new LinkedHashMap<>()).putIfAbsent(o, 0);
    }

    public int getObjectiveProgress(String questId, String objectiveKey) {
        String q = normalize(questId);
        String o = normalize(objectiveKey);
        Map<String, Integer> perQuest = objectiveProgress.get(q);
        return perQuest == null ? 0 : perQuest.getOrDefault(o, 0);
    }

    public int addObjectiveProgress(String questId, String objectiveKey, int amount) {
        if (amount <= 0) {
            return getObjectiveProgress(questId, objectiveKey);
        }
        String q = normalize(questId);
        String o = normalize(objectiveKey);
        LinkedHashMap<String, Integer> perQuest = objectiveProgress.computeIfAbsent(q, ignored -> new LinkedHashMap<>());
        int next = Math.max(0, perQuest.getOrDefault(o, 0) + amount);
        perQuest.put(o, next);
        return next;
    }

    public Map<String, Integer> objectiveProgressOf(String questId) {
        String q = normalize(questId);
        Map<String, Integer> perQuest = objectiveProgress.get(q);
        return perQuest == null ? Map.of() : Map.copyOf(perQuest);
    }

    public void loadSnapshot(
            Set<String> acceptedSnapshot,
            Set<String> completedSnapshot,
            Map<String, Map<String, Integer>> objectiveSnapshot,
            Map<String, Long> completedAtSnapshot,
            Map<String, Long> failedAtSnapshot
    ) {
        accepted.clear();
        completed.clear();
        objectiveProgress.clear();
        completedAtEpochSec.clear();
        failedAtEpochSec.clear();
        if (acceptedSnapshot != null) {
            for (String id : acceptedSnapshot) {
                String normalized = normalize(id);
                if (!normalized.isBlank()) {
                    accepted.add(normalized);
                }
            }
        }
        if (completedSnapshot != null) {
            for (String id : completedSnapshot) {
                String normalized = normalize(id);
                if (!normalized.isBlank()) {
                    completed.add(normalized);
                }
            }
        }
        if (objectiveSnapshot != null) {
            for (var entry : objectiveSnapshot.entrySet()) {
                String quest = normalize(entry.getKey());
                if (quest.isBlank()) {
                    continue;
                }
                LinkedHashMap<String, Integer> perQuest = new LinkedHashMap<>();
                for (var objective : entry.getValue().entrySet()) {
                    String key = normalize(objective.getKey());
                    if (key.isBlank()) {
                        continue;
                    }
                    perQuest.put(key, Math.max(0, objective.getValue()));
                }
                objectiveProgress.put(quest, perQuest);
            }
        }
        if (completedAtSnapshot != null) {
            for (var entry : completedAtSnapshot.entrySet()) {
                String quest = normalize(entry.getKey());
                if (quest.isBlank()) {
                    continue;
                }
                completedAtEpochSec.put(quest, Math.max(0L, entry.getValue()));
            }
        }
        if (failedAtSnapshot != null) {
            for (var entry : failedAtSnapshot.entrySet()) {
                String quest = normalize(entry.getKey());
                if (quest.isBlank()) {
                    continue;
                }
                failedAtEpochSec.put(quest, Math.max(0L, entry.getValue()));
            }
        }
    }

    public Map<String, Map<String, Integer>> objectiveSnapshot() {
        LinkedHashMap<String, Map<String, Integer>> out = new LinkedHashMap<>();
        for (var entry : objectiveProgress.entrySet()) {
            out.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return out;
    }

    public Map<String, Long> completedAtSnapshot() {
        return Map.copyOf(completedAtEpochSec);
    }

    public Map<String, Long> failedAtSnapshot() {
        return Map.copyOf(failedAtEpochSec);
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase();
    }
}

package qorhvkdy.qorhvkdy.rpgmod.quest;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * 퀘스트 진행도 NBT 직렬화.
 */
public final class QuestStorage {
    private static final int DATA_VERSION = 1;

    private QuestStorage() {
    }

    public static CompoundTag save(PlayerQuestProgress progress, HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("dataVersion", DATA_VERSION);

        CompoundTag acceptedTag = new CompoundTag();
        for (String id : progress.acceptedList()) {
            acceptedTag.putBoolean(id, true);
        }
        tag.put("accepted", acceptedTag);

        CompoundTag completedTag = new CompoundTag();
        for (String id : progress.completedList()) {
            completedTag.putBoolean(id, true);
        }
        tag.put("completed", completedTag);

        CompoundTag objectiveTag = new CompoundTag();
        for (var entry : progress.objectiveSnapshot().entrySet()) {
            CompoundTag perQuest = new CompoundTag();
            for (var objective : entry.getValue().entrySet()) {
                perQuest.putInt(objective.getKey(), objective.getValue());
            }
            objectiveTag.put(entry.getKey(), perQuest);
        }
        tag.put("objectiveProgress", objectiveTag);

        CompoundTag completedAtTag = new CompoundTag();
        for (var entry : progress.completedAtSnapshot().entrySet()) {
            completedAtTag.putLong(entry.getKey(), entry.getValue());
        }
        tag.put("completedAt", completedAtTag);

        CompoundTag failedAtTag = new CompoundTag();
        for (var entry : progress.failedAtSnapshot().entrySet()) {
            failedAtTag.putLong(entry.getKey(), entry.getValue());
        }
        tag.put("failedAt", failedAtTag);
        return tag;
    }

    public static void load(PlayerQuestProgress progress, CompoundTag tag, HolderLookup.Provider provider) {
        LinkedHashSet<String> accepted = new LinkedHashSet<>();
        LinkedHashSet<String> completed = new LinkedHashSet<>();
        LinkedHashMap<String, java.util.Map<String, Integer>> objectiveProgress = new LinkedHashMap<>();
        LinkedHashMap<String, Long> completedAt = new LinkedHashMap<>();
        LinkedHashMap<String, Long> failedAt = new LinkedHashMap<>();

        tag.getCompound("accepted").ifPresent(value -> accepted.addAll(value.keySet()));
        tag.getCompound("completed").ifPresent(value -> completed.addAll(value.keySet()));
        tag.getCompound("objectiveProgress").ifPresent(objectiveTag -> {
            for (String quest : objectiveTag.keySet()) {
                LinkedHashMap<String, Integer> perQuest = new LinkedHashMap<>();
                objectiveTag.getCompound(quest).ifPresent(perQuestTag -> {
                    for (String objectiveKey : perQuestTag.keySet()) {
                        perQuestTag.getInt(objectiveKey).ifPresent(value -> perQuest.put(objectiveKey, Math.max(0, value)));
                    }
                });
                objectiveProgress.put(quest, perQuest);
            }
        });
        tag.getCompound("completedAt").ifPresent(completedAtTag -> {
            for (String quest : completedAtTag.keySet()) {
                completedAtTag.getLong(quest).ifPresent(value -> completedAt.put(quest, Math.max(0L, value)));
            }
        });
        tag.getCompound("failedAt").ifPresent(failedAtTag -> {
            for (String quest : failedAtTag.keySet()) {
                failedAtTag.getLong(quest).ifPresent(value -> failedAt.put(quest, Math.max(0L, value)));
            }
        });
        progress.loadSnapshot(accepted, completed, objectiveProgress, completedAt, failedAt);
    }
}

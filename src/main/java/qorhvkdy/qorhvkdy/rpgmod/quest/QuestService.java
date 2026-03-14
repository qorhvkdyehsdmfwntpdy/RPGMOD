package qorhvkdy.qorhvkdy.rpgmod.quest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassAdvancement;
import qorhvkdy.qorhvkdy.rpgmod.party.PartyService;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;
import qorhvkdy.qorhvkdy.rpgmod.quest.data.QuestJson;
import qorhvkdy.qorhvkdy.rpgmod.quest.data.QuestRepository;
import qorhvkdy.qorhvkdy.rpgmod.quest.content.QuestContentLinkService;
import qorhvkdy.qorhvkdy.rpgmod.quest.zone.QuestZoneService;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatType;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatsUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 퀘스트 실행 서비스.
 * Objective 진행도 + 반복 쿨다운(일일/주간/커스텀)을 지원한다.
 */
public final class QuestService {
    private static final Map<String, QuestJson.Entry> QUESTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> ACCEPTED = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> COMPLETED = new ConcurrentHashMap<>();

    private QuestService() {
    }

    public static synchronized void bootstrap() {
        QuestRepository.bootstrap();
        QuestZoneService.bootstrap();
        reload();
    }

    public static synchronized void reload() {
        QuestRepository.reload();
        QuestZoneService.reload();
        LinkedHashMap<String, QuestJson.Entry> next = new LinkedHashMap<>();
        for (QuestJson.Entry entry : QuestRepository.get().quests) {
            String id = normalize(entry.id);
            if (id.isBlank()) {
                continue;
            }
            next.put(id, entry);
        }
        QUESTS.clear();
        QUESTS.putAll(next);
    }

    public static List<String> listAvailable(ServerPlayer player) {
        PlayerStats stats = StatsUtil.get(player);
        List<String> out = new ArrayList<>();
        PlayerQuestProgress progress = QuestUtil.get(player);
        for (QuestJson.Entry entry : QUESTS.values()) {
            String id = normalize(entry.id);
            if (progress.isAccepted(id)) {
                continue;
            }
            if (!entry.repeatable && progress.isCompleted(id)) {
                continue;
            }
            if (entry.repeatable && cooldownRemainingSec(player, entry) > 0L) {
                continue;
            }
            if (checkConditions(player, stats, entry.conditions).passed()) {
                out.add(id);
            }
        }
        return out;
    }

    public static boolean existsQuest(String questId) {
        return QUESTS.containsKey(normalize(questId));
    }

    public static CheckResult canAccept(ServerPlayer player, String questId) {
        String id = normalize(questId);
        QuestJson.Entry entry = QUESTS.get(id);
        if (entry == null) {
            return CheckResult.fail("unknown_quest");
        }
        PlayerQuestProgress progress = QuestUtil.get(player);
        if (progress.isAccepted(id)) {
            return CheckResult.fail("already_accepted");
        }
        if (!entry.repeatable && progress.isCompleted(id)) {
            return CheckResult.fail("already_completed");
        }
        long retryRemain = retryCooldownRemainingSec(player, entry);
        if (retryRemain > 0L) {
            return CheckResult.fail("retry_cooldown_remaining_sec:" + retryRemain);
        }
        long remain = cooldownRemainingSec(player, entry);
        if (remain > 0L) {
            return CheckResult.fail("cooldown_remaining_sec:" + remain);
        }
        for (String prerequisite : entry.prerequisites) {
            if (!progress.isCompleted(prerequisite)) {
                return CheckResult.fail("missing_prerequisite:" + prerequisite);
            }
        }
        PlayerStats stats = StatsUtil.get(player);
        return checkConditions(player, stats, entry.conditions);
    }

    public static boolean accept(ServerPlayer player, String questId) {
        return acceptInternal(player, questId, true);
    }

    private static boolean acceptInternal(ServerPlayer player, String questId, boolean allowPartyShare) {
        String id = normalize(questId);
        CheckResult check = canAccept(player, id);
        if (!check.passed) {
            return false;
        }
        PlayerQuestProgress progress = QuestUtil.get(player);
        // 반복/재도전 시 이전 objective 누적값이 남지 않도록 초기화합니다.
        progress.clearQuestState(id);
        if (!progress.accept(id)) {
            return false;
        }
        QuestJson.Entry entry = QUESTS.get(id);
        if (entry != null) {
            for (QuestJson.Objective objective : entry.objectives) {
                progress.initObjective(id, objectiveKey(objective));
            }
        }
        updateMirror(player, progress);
        if (allowPartyShare && entry != null && entry.shareWithParty) {
            shareAcceptToPartyMembers(player, id);
        }
        return true;
    }

    public static boolean complete(ServerPlayer player, String questId) {
        String id = normalize(questId);
        PlayerQuestProgress progress = QuestUtil.get(player);
        if (!progress.isAccepted(id)) {
            return false;
        }
        QuestJson.Entry entry = QUESTS.get(id);
        if (entry == null || !isObjectiveDone(player, id)) {
            return false;
        }
        progress.unaccept(id);
        applyRewards(player, entry);
        QuestContentLinkService.triggerOnComplete(player, id);
        progress.complete(id);
        updateMirror(player, progress);
        return true;
    }

    public static boolean onKillMob(ServerPlayer player, String entityId) {
        return advanceObjective(player, "KILL_MOB", entityId, 1);
    }

    public static boolean onBreakBlock(ServerPlayer player, String blockId) {
        return advanceObjective(player, "BREAK_BLOCK", blockId, 1);
    }

    public static boolean onTalkNpc(ServerPlayer player, String npcId) {
        return advanceObjective(player, "TALK_NPC", npcId, 1);
    }

    public static int failDeathSensitiveQuests(ServerPlayer player) {
        PlayerQuestProgress progress = QuestUtil.get(player);
        int failed = 0;
        for (String questId : progress.acceptedList()) {
            QuestJson.Entry entry = QUESTS.get(normalize(questId));
            if (entry == null || !entry.failOnDeath) {
                continue;
            }
            if (fail(player, questId, "death")) {
                failed++;
            }
        }
        return failed;
    }

    public static boolean fail(ServerPlayer player, String questId, String reason) {
        String id = normalize(questId);
        PlayerQuestProgress progress = QuestUtil.get(player);
        if (!progress.isAccepted(id)) {
            return false;
        }
        progress.unaccept(id);
        progress.clearQuestState(id);
        progress.markFailed(id);
        updateMirror(player, progress);
        return true;
    }

    public static boolean adminComplete(ServerPlayer player, String questId, boolean grantRewards) {
        String id = normalize(questId);
        QuestJson.Entry entry = QUESTS.get(id);
        if (entry == null) {
            return false;
        }
        PlayerQuestProgress progress = QuestUtil.get(player);
        progress.unaccept(id);
        progress.clearQuestState(id);
        if (grantRewards) {
            applyRewards(player, entry);
            QuestContentLinkService.triggerOnComplete(player, id);
        }
        progress.complete(id);
        updateMirror(player, progress);
        return true;
    }

    public static boolean adminResetQuest(ServerPlayer player, String questId) {
        String id = normalize(questId);
        if (!existsQuest(id)) {
            return false;
        }
        PlayerQuestProgress progress = QuestUtil.get(player);
        progress.unaccept(id);
        progress.clearQuestState(id);
        progress.uncomplete(id);
        progress.clearCompletedAt(id);
        progress.clearFailedAt(id);
        updateMirror(player, progress);
        return true;
    }

    public static boolean adminResetCooldown(ServerPlayer player, String questId) {
        String id = normalize(questId);
        if (!existsQuest(id)) {
            return false;
        }
        PlayerQuestProgress progress = QuestUtil.get(player);
        progress.clearCompletedAt(id);
        progress.clearFailedAt(id);
        updateMirror(player, progress);
        return true;
    }

    public static int adminResetAll(ServerPlayer player) {
        PlayerQuestProgress progress = QuestUtil.get(player);
        int count = 0;
        for (String questId : new java.util.ArrayList<>(QUESTS.keySet())) {
            progress.unaccept(questId);
            progress.clearQuestState(questId);
            progress.uncomplete(questId);
            progress.clearCompletedAt(questId);
            progress.clearFailedAt(questId);
            count++;
        }
        updateMirror(player, progress);
        return count;
    }

    public static boolean isAccepted(UUID playerId, String questId) {
        return ACCEPTED.getOrDefault(playerId, Set.of()).contains(normalize(questId));
    }

    public static boolean isCompleted(UUID playerId, String questId) {
        return COMPLETED.getOrDefault(playerId, Set.of()).contains(normalize(questId));
    }

    public static boolean isAccepted(ServerPlayer player, String questId) {
        return QuestUtil.get(player).isAccepted(questId);
    }

    public static boolean isCompleted(ServerPlayer player, String questId) {
        return QuestUtil.get(player).isCompleted(questId);
    }

    /**
     * Capability에 들어 있는 실제 진행도로 UUID 기반 mirror cache를 갱신한다.
     * 다른 시스템이 UUID만으로 선행 퀘스트를 조회할 때 로그인 직후 값이 비지 않도록 보정한다.
     */
    public static void refreshMirror(ServerPlayer player) {
        updateMirror(player, QuestUtil.get(player));
    }

    public static void clearMirror(UUID playerId) {
        ACCEPTED.remove(playerId);
        COMPLETED.remove(playerId);
    }

    public static List<String> acceptedList(ServerPlayer player) {
        return QuestUtil.get(player).acceptedList();
    }

    public static List<String> completedList(ServerPlayer player) {
        return QuestUtil.get(player).completedList();
    }

    public static List<String> acceptedProgressSummaries(ServerPlayer player) {
        PlayerQuestProgress progress = QuestUtil.get(player);
        List<String> out = new ArrayList<>();
        for (String questId : progress.acceptedList()) {
            QuestJson.Entry entry = QUESTS.get(normalize(questId));
            if (entry == null) {
                continue;
            }
            int done = 0;
            int total = Math.max(1, entry.objectives.size());
            for (QuestJson.Objective objective : entry.objectives) {
                int current = progress.getObjectiveProgress(questId, objectiveKey(objective));
                if (current >= Math.max(1, objective.target)) {
                    done++;
                }
            }
            out.add(questId + " (" + done + "/" + total + ")");
        }
        return out;
    }

    public static List<String> objectiveStatusLines(ServerPlayer player, String questId) {
        String id = normalize(questId);
        QuestJson.Entry entry = QUESTS.get(id);
        if (entry == null) {
            return List.of();
        }
        PlayerQuestProgress progress = QuestUtil.get(player);
        List<String> out = new ArrayList<>();
        for (QuestJson.Objective objective : entry.objectives) {
            int current = progress.getObjectiveProgress(id, objectiveKey(objective));
            int target = Math.max(1, objective.target);
            out.add(objective.type + ":" + objective.key + " " + current + "/" + target);
        }
        return out;
    }

    public static List<String> acceptedObjectiveLines(ServerPlayer player, int maxLines) {
        List<String> out = new ArrayList<>();
        for (String questId : acceptedList(player)) {
            List<String> lines = objectiveStatusLines(player, questId);
            if (lines.isEmpty()) {
                continue;
            }
            out.add("[" + questId + "]");
            out.addAll(lines);
            if (out.size() >= maxLines) {
                break;
            }
        }
        if (out.size() > maxLines) {
            return out.subList(0, maxLines);
        }
        return out;
    }

    public static List<String> rewardPreviewLines(ServerPlayer player, int maxLines) {
        List<String> out = new ArrayList<>();
        for (String questId : listAvailable(player)) {
            QuestJson.Entry entry = QUESTS.get(normalize(questId));
            if (entry == null) {
                continue;
            }
            int commands = entry.rewards == null || entry.rewards.commands == null ? 0 : entry.rewards.commands.size();
            int guaranteed = entry.rewards == null || entry.rewards.guaranteedItems == null ? 0 : entry.rewards.guaranteedItems.size();
            int randomGroups = entry.rewards == null || entry.rewards.randomGroups == null ? 0 : entry.rewards.randomGroups.size();
            int classRewards = entry.rewards == null || entry.rewards.classRewards == null ? 0 : entry.rewards.classRewards.size();
            int xp = entry.rewards == null ? 0 : Math.max(0, entry.rewards.xpLevels);
            out.add(questId + " -> XP +" + xp
                    + ", fixed " + guaranteed
                    + ", rng " + randomGroups
                    + ", class " + classRewards
                    + ", cmd " + commands);
            if (out.size() >= maxLines) {
                break;
            }
        }
        return out;
    }

    /**
     * 퀘스트 UI 동기화 스냅샷을 한 번에 구성해 전송한다.
     */
    public static void sync(ServerPlayer player) {
        ModNetwork.syncQuestToPlayer(
                player,
                listAvailable(player),
                acceptedProgressSummaries(player),
                completedList(player),
                acceptedObjectiveLines(player, 18),
                rewardPreviewLines(player, 10)
        );
    }

    public static long cooldownRemainingSec(ServerPlayer player, QuestJson.Entry entry) {
        if (entry == null || !entry.repeatable) {
            return 0L;
        }
        long cooldown = cooldownSec(entry);
        if (cooldown <= 0L) {
            return 0L;
        }
        long last = QuestUtil.get(player).completedAt(entry.id);
        if (last <= 0L) {
            return 0L;
        }
        long now = System.currentTimeMillis() / 1000L;
        long remain = cooldown - (now - last);
        return Math.max(0L, remain);
    }

    public static long retryCooldownRemainingSec(ServerPlayer player, QuestJson.Entry entry) {
        if (entry == null) {
            return 0L;
        }
        long cooldown = Math.max(0, entry.retryCooldownSec);
        if (cooldown <= 0L) {
            return 0L;
        }
        long lastFailed = QuestUtil.get(player).failedAt(entry.id);
        if (lastFailed <= 0L) {
            return 0L;
        }
        long now = System.currentTimeMillis() / 1000L;
        long remain = cooldown - (now - lastFailed);
        return Math.max(0L, remain);
    }

    private static long cooldownSec(QuestJson.Entry entry) {
        String policy = normalize(entry.repeatPolicy);
        return switch (policy) {
            case "daily" -> 86_400L;
            case "weekly" -> 604_800L;
            case "custom" -> Math.max(0, entry.repeatCooldownSec);
            default -> 0L;
        };
    }

    private static boolean isObjectiveDone(ServerPlayer player, String questId) {
        String id = normalize(questId);
        QuestJson.Entry entry = QUESTS.get(id);
        if (entry == null || entry.objectives.isEmpty()) {
            return true;
        }
        PlayerQuestProgress progress = QuestUtil.get(player);
        for (QuestJson.Objective objective : entry.objectives) {
            int current = progress.getObjectiveProgress(id, objectiveKey(objective));
            if (current < Math.max(1, objective.target)) {
                return false;
            }
        }
        return true;
    }

    private static boolean advanceObjective(ServerPlayer player, String type, String key, int amount) {
        String normalizedType = normalize(type);
        String normalizedKey = normalize(key);
        PlayerQuestProgress progress = QuestUtil.get(player);
        boolean changed = false;
        for (String questId : progress.acceptedList()) {
            QuestJson.Entry entry = QUESTS.get(normalize(questId));
            if (entry == null) {
                continue;
            }
            for (QuestJson.Objective objective : entry.objectives) {
                String objectiveType = normalize(objective.type);
                String objectiveKey = normalize(objective.key);
                if (!objectiveType.equals(normalizedType)) {
                    continue;
                }
                if (!objectiveKey.equals("*") && !objectiveKey.equals(normalizedKey)) {
                    continue;
                }
                progress.addObjectiveProgress(questId, objectiveKey(objective), amount);
                changed = true;
            }
        }
        if (changed) {
            updateMirror(player, progress);
        }
        return changed;
    }

    private static void updateMirror(ServerPlayer player, PlayerQuestProgress progress) {
        ACCEPTED.put(player.getUUID(), new LinkedHashSet<>(progress.acceptedList()));
        COMPLETED.put(player.getUUID(), new LinkedHashSet<>(progress.completedList()));
    }

    private static void shareAcceptToPartyMembers(ServerPlayer player, String questId) {
        var server = player.level().getServer();
        if (server == null) {
            return;
        }
        String partyId = PartyService.getPartyId(player.getUUID()).orElse("");
        if (partyId.isBlank()) {
            return;
        }
        for (ServerPlayer member : PartyService.onlineMembers(server, partyId)) {
            if (member.getUUID().equals(player.getUUID())) {
                continue;
            }
            acceptInternal(member, questId, false);
        }
    }

    private static String objectiveKey(QuestJson.Objective objective) {
        return normalize(objective.type) + ":" + normalize(objective.key);
    }

    private static void applyRewards(ServerPlayer player, QuestJson.Entry entry) {
        if (entry == null || entry.rewards == null) {
            return;
        }
        QuestJson.Reward rewards = entry.rewards;
        List<String> commands = new ArrayList<>();

        int totalXp = Math.max(0, rewards.xpLevels);
        String classId = normalize(StatsUtil.get(player).getSelectedClass().id());
        for (QuestJson.ClassReward classReward : safeClassRewards(rewards.classRewards)) {
            String targetClass = normalize(classReward.classId);
            if (!targetClass.equals("*") && !targetClass.equals(classId)) {
                continue;
            }
            totalXp += Math.max(0, classReward.xpLevelsBonus);
            commands.addAll(safeCommands(classReward.commands));
            commands.addAll(itemRewardsToCommands(classReward.guaranteedItems));
        }
        commands.addAll(safeCommands(rewards.commands));
        commands.addAll(itemRewardsToCommands(rewards.guaranteedItems));
        commands.addAll(randomRewardsToCommands(rewards.randomGroups));

        if (totalXp > 0) {
            player.giveExperienceLevels(totalXp);
        }
        runRewardCommands(player, commands);
    }

    private static List<String> safeCommands(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static List<QuestJson.ClassReward> safeClassRewards(List<QuestJson.ClassReward> values) {
        return values == null ? List.of() : values;
    }

    private static List<String> itemRewardsToCommands(List<QuestJson.ItemReward> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        for (QuestJson.ItemReward itemReward : rewards) {
            String command = toGiveCommand(itemReward == null ? null : itemReward.itemId, itemReward == null ? 0 : itemReward.count);
            if (!command.isBlank()) {
                out.add(command);
            }
        }
        return out;
    }

    private static List<String> randomRewardsToCommands(List<QuestJson.RandomRewardGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        for (QuestJson.RandomRewardGroup group : groups) {
            if (group == null || group.entries == null || group.entries.isEmpty()) {
                continue;
            }
            if (group.pickOne) {
                QuestJson.RandomRewardEntry picked = pickWeighted(group.entries);
                if (picked == null) {
                    continue;
                }
                if (ThreadLocalRandom.current().nextDouble() <= normalizeChance(picked.chance)) {
                    String command = toGiveCommand(picked.itemId, picked.count);
                    if (!command.isBlank()) {
                        out.add(command);
                    }
                }
                continue;
            }
            for (QuestJson.RandomRewardEntry entry : group.entries) {
                if (entry == null) {
                    continue;
                }
                if (ThreadLocalRandom.current().nextDouble() > normalizeChance(entry.chance)) {
                    continue;
                }
                String command = toGiveCommand(entry.itemId, entry.count);
                if (!command.isBlank()) {
                    out.add(command);
                }
            }
        }
        return out;
    }

    private static QuestJson.RandomRewardEntry pickWeighted(List<QuestJson.RandomRewardEntry> entries) {
        int total = 0;
        for (QuestJson.RandomRewardEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            total += Math.max(0, entry.weight);
        }
        if (total <= 0) {
            return null;
        }
        int roll = ThreadLocalRandom.current().nextInt(total) + 1;
        int sum = 0;
        for (QuestJson.RandomRewardEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            sum += Math.max(0, entry.weight);
            if (roll <= sum) {
                return entry;
            }
        }
        return null;
    }

    private static double normalizeChance(double raw) {
        if (raw <= 0.0) {
            return 0.0;
        }
        if (raw > 1.0) {
            return Math.min(1.0, raw / 100.0);
        }
        return Math.min(1.0, raw);
    }

    private static String toGiveCommand(String itemIdRaw, int countRaw) {
        String itemId = normalize(itemIdRaw);
        int count = Math.max(1, countRaw);
        if (itemId.isBlank() || "minecraft:air".equals(itemId)) {
            return "";
        }
        return "give %player% " + itemId + " " + count;
    }

    // 퀘스트 보상 커맨드를 서버 콘텍스트에서 실행합니다.
    // %player% 치환을 지원해 JSON에서 플레이어 대상 보상을 쉽게 작성할 수 있습니다.
    private static void runRewardCommands(ServerPlayer player, List<String> commands) {
        var server = player.level().getServer();
        if (server == null) {
            return;
        }
        if (commands == null || commands.isEmpty()) {
            return;
        }
        for (String raw : commands) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String resolved = raw.replace("%player%", player.getScoreboardName()).trim();
            if (resolved.isEmpty()) {
                continue;
            }
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withSuppressedOutput(),
                    resolved
            );
        }
    }

    private static CheckResult checkConditions(ServerPlayer player, PlayerStats stats, List<QuestJson.Condition> conditions) {
        List<String> reasons = new ArrayList<>();
        for (QuestJson.Condition condition : conditions) {
            String type = normalize(condition.type);
            switch (type) {
                case "level" -> {
                    if (player.experienceLevel < Math.max(0, condition.min)) {
                        reasons.add("LEVEL<" + condition.min);
                    }
                }
                case "base_class" -> {
                    String required = normalize(condition.value);
                    if (!normalize(stats.getSelectedClass().id()).equals(required)) {
                        reasons.add("BASE_CLASS!=" + required);
                    }
                }
                case "advancement" -> {
                    ClassAdvancement current = stats.getCurrentAdvancement();
                    if (!normalize(current.id()).equals(normalize(condition.value))) {
                        reasons.add("ADVANCEMENT!=" + normalize(condition.value));
                    }
                }
                case "stat" -> {
                    StatType stat = parseStat(condition.key);
                    if (stat == null || stats.get(stat) < Math.max(0, condition.min)) {
                        reasons.add("STAT_FAIL:" + condition.key + "<" + condition.min);
                    }
                }
                case "permission" -> {
                    if (!RpgPermissionService.has(player, condition.value)) {
                        reasons.add("PERMISSION_MISSING:" + condition.value);
                    }
                }
                case "dimension" -> {
                    String required = normalize(condition.value);
                    String current = normalizeDimension(player.level().dimension().toString());
                    if (!required.equals(current)) {
                        reasons.add("DIMENSION!=" + required);
                    }
                }
                case "zone" -> {
                    if (!QuestZoneService.isPlayerInZone(player, condition.value)) {
                        reasons.add("ZONE_MISMATCH:" + normalize(condition.value));
                    }
                }
                default -> reasons.add("UNSUPPORTED:" + condition.type);
            }
        }
        return reasons.isEmpty() ? CheckResult.pass() : CheckResult.fail(String.join(", ", reasons));
    }

    private static StatType parseStat(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT);
        for (StatType type : StatType.values()) {
            if (type.key().equals(key) || type.name().equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeDimension(String raw) {
        String value = normalize(raw);
        int slash = value.lastIndexOf('/');
        int end = value.lastIndexOf(']');
        if (slash >= 0 && end > slash) {
            return value.substring(slash + 1, end).trim();
        }
        return value;
    }

    public static String normalizeEntityId(net.minecraft.world.entity.EntityType<?> entityType) {
        var id = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        return id == null ? "" : normalize(id.toString());
    }

    public static String normalizeBlockId(net.minecraft.world.level.block.Block block) {
        var id = ForgeRegistries.BLOCKS.getKey(block);
        return id == null ? "" : normalize(id.toString());
    }

    public record CheckResult(boolean passed, String reason) {
        public static CheckResult pass() {
            return new CheckResult(true, "");
        }

        public static CheckResult fail(String reason) {
            return new CheckResult(false, reason);
        }
    }
}

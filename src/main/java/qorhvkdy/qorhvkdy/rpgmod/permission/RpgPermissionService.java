package qorhvkdy.qorhvkdy.rpgmod.permission;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import qorhvkdy.qorhvkdy.rpgmod.permission.data.PermissionConfigJson;
import qorhvkdy.qorhvkdy.rpgmod.permission.data.PermissionRepository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 경량 권한 서비스.
 * 그룹 상속 + 유저별 노드 오버라이드만 제공한다.
 */
public final class RpgPermissionService {
    private static final Map<UUID, Set<String>> RESOLVED_CACHE = new ConcurrentHashMap<>();

    private RpgPermissionService() {
    }

    public static void bootstrap() {
        PermissionRepository.bootstrap();
        purgeExpiredTempNodes(true);
        clearCache();
    }

    public static void reload() {
        PermissionRepository.reload();
        purgeExpiredTempNodes(true);
        clearCache();
    }

    public static void clearCache() {
        RESOLVED_CACHE.clear();
    }

    public static boolean hasOrOp(CommandSourceStack source, String node) {
        if (source.getPlayer() == null) {
            return true;
        }
        if (source.permissions() != PermissionSet.NO_PERMISSIONS) {
            return true;
        }
        return has(source.getPlayer(), node);
    }

    public static boolean has(ServerPlayer player, String node) {
        return has(player.getUUID(), node, PermissionContext.fromPlayer(player));
    }

    public static boolean has(UUID playerId, String node) {
        return has(playerId, node, PermissionContext.empty());
    }

    public static boolean has(UUID playerId, String node, PermissionContext context) {
        String normalized = normalizeNode(node);
        if (normalized.isBlank()) {
            return false;
        }
        Set<String> granted = resolveEffectiveNodes(playerId, context == null ? PermissionContext.empty() : context);
        return evaluate(granted, normalized).allowed();
    }

    public static PermissionEvaluation explain(UUID playerId, String node) {
        String normalized = normalizeNode(node);
        if (normalized.isBlank()) {
            return new PermissionEvaluation(normalized, false, "-", "invalid_node");
        }
        Set<String> granted = resolveEffectiveNodes(playerId, PermissionContext.empty());
        return evaluate(granted, normalized);
    }

    public static PermissionEvaluation explain(UUID playerId, String node, PermissionContext context) {
        String normalized = normalizeNode(node);
        if (normalized.isBlank()) {
            return new PermissionEvaluation(normalized, false, "-", "invalid_node");
        }
        Set<String> granted = resolveEffectiveNodes(playerId, context == null ? PermissionContext.empty() : context);
        return evaluate(granted, normalized);
    }

    public static PermissionView viewOf(ServerPlayer player) {
        UUID id = player.getUUID();
        PermissionContext context = PermissionContext.fromPlayer(player);
        GroupMetaView meta = metaOf(id);
        return new PermissionView(
                groupOf(id),
                meta.prefix(),
                meta.weight(),
                has(id, PermissionNodes.PERMISSION_ADMIN, context),
                has(id, PermissionNodes.CLASS_ADMIN, context),
                has(id, PermissionNodes.STATS_ADMIN, context),
                has(id, PermissionNodes.PROFICIENCY_ADMIN, context),
                has(id, PermissionNodes.DEBUG_ADMIN, context),
                has(id, PermissionNodes.UI_PERM_OPEN, context),
                has(id, PermissionNodes.UI_SKILL_TREE_OPEN, context),
                has(id, PermissionNodes.PARTY_MANAGE, context),
                has(id, PermissionNodes.PARTY_FORCE_KICK, context),
                has(id, PermissionNodes.GUILD_MANAGE, context),
                has(id, PermissionNodes.TITLE_MANAGE, context)
        );
    }

    public static List<String> listGroups() {
        return new ArrayList<>(PermissionRepository.get().groups.keySet());
    }

    public static String groupOf(UUID playerId) {
        String uuid = normalizePlain(playerId.toString());
        return PermissionRepository.get().users.getOrDefault(uuid, "player");
    }

    public static boolean setUserGroup(UUID playerId, String groupId) {
        String group = normalizePlain(groupId);
        PermissionConfigJson data = PermissionRepository.get();
        if (!data.groups.containsKey(group)) {
            return false;
        }
        PermissionMutationHistoryService.pushSnapshot("setUserGroup");
        data.users.put(normalizePlain(playerId.toString()), group);
        PermissionRepository.save();
        clearCache();
        return true;
    }

    public static boolean addUserNode(UUID playerId, String node) {
        String normalizedNode = normalizeNode(node);
        if (normalizedNode.isBlank()) {
            return false;
        }
        PermissionConfigJson data = PermissionRepository.get();
        String uuid = normalizePlain(playerId.toString());
        List<String> list = data.userNodes.computeIfAbsent(uuid, key -> new ArrayList<>());
        if (list.contains(normalizedNode)) {
            return false;
        }
        PermissionMutationHistoryService.pushSnapshot("addUserNode");
        list.add(normalizedNode);
        PermissionRepository.save();
        clearCache();
        return true;
    }

    public static boolean removeUserNode(UUID playerId, String node) {
        String normalizedNode = normalizeNode(node);
        PermissionConfigJson data = PermissionRepository.get();
        String uuid = normalizePlain(playerId.toString());
        List<String> list = data.userNodes.get(uuid);
        if (list == null || !list.remove(normalizedNode)) {
            return false;
        }
        PermissionMutationHistoryService.pushSnapshot("removeUserNode");
        PermissionRepository.save();
        clearCache();
        return true;
    }

    public static boolean addUserTempNode(UUID playerId, String node, long durationSeconds) {
        String normalizedNode = normalizeNode(node);
        if (normalizedNode.isBlank() || durationSeconds <= 0L) {
            return false;
        }
        PermissionConfigJson data = PermissionRepository.get();
        String uuid = normalizePlain(playerId.toString());
        long expiresAt = (System.currentTimeMillis() / 1000L) + durationSeconds;
        PermissionConfigJson.TempNodeJson temp = new PermissionConfigJson.TempNodeJson();
        temp.node = normalizedNode;
        temp.expiresAtEpochSec = expiresAt;
        PermissionMutationHistoryService.pushSnapshot("addUserTempNode");
        data.userTempNodes.computeIfAbsent(uuid, ignored -> new ArrayList<>()).add(temp);
        PermissionRepository.save();
        clearCache();
        return true;
    }

    public static int purgeExpiredTempNodes(boolean persistIfChanged) {
        PermissionConfigJson data = PermissionRepository.get();
        long nowSec = System.currentTimeMillis() / 1000L;
        int removed = 0;
        for (var entry : data.userTempNodes.entrySet()) {
            List<PermissionConfigJson.TempNodeJson> list = entry.getValue();
            if (list == null) {
                continue;
            }
            int before = list.size();
            list.removeIf(node -> node == null || node.expiresAtEpochSec > 0L && node.expiresAtEpochSec <= nowSec);
            removed += (before - list.size());
        }
        if (removed > 0) {
            PermissionMutationHistoryService.pushSnapshot("purgeExpiredTempNodes");
            data.userTempNodes.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isEmpty());
            if (persistIfChanged) {
                PermissionRepository.save();
            }
            clearCache();
        }
        return removed;
    }

    public static boolean addGroupNode(String groupId, String node) {
        String group = normalizePlain(groupId);
        String normalizedNode = normalizeNode(node);
        if (normalizedNode.isBlank()) {
            return false;
        }
        PermissionConfigJson data = PermissionRepository.get();
        PermissionConfigJson.GroupJson groupJson = data.groups.get(group);
        if (groupJson == null) {
            return false;
        }
        if (groupJson.nodes.contains(normalizedNode)) {
            return false;
        }
        PermissionMutationHistoryService.pushSnapshot("addGroupNode");
        groupJson.nodes.add(normalizedNode);
        PermissionRepository.save();
        clearCache();
        return true;
    }

    public static boolean removeGroupNode(String groupId, String node) {
        String group = normalizePlain(groupId);
        String normalizedNode = normalizeNode(node);
        PermissionConfigJson data = PermissionRepository.get();
        PermissionConfigJson.GroupJson groupJson = data.groups.get(group);
        if (groupJson == null) {
            return false;
        }
        boolean removed = groupJson.nodes.remove(normalizedNode);
        if (!removed) {
            return false;
        }
        PermissionMutationHistoryService.pushSnapshot("removeGroupNode");
        PermissionRepository.save();
        clearCache();
        return true;
    }

    public static boolean addGroupContextNode(String groupId, String selector, String node) {
        String group = normalizePlain(groupId);
        String normalizedSelector = normalizeSelector(selector);
        String normalizedNode = normalizeNode(node);
        if (normalizedSelector.isBlank() || normalizedNode.isBlank()) {
            return false;
        }
        PermissionConfigJson.GroupJson groupJson = PermissionRepository.get().groups.get(group);
        if (groupJson == null) {
            return false;
        }
        List<String> list = groupJson.contextNodes.computeIfAbsent(normalizedSelector, key -> new ArrayList<>());
        if (list.contains(normalizedNode)) {
            return false;
        }
        PermissionMutationHistoryService.pushSnapshot("addGroupContextNode");
        list.add(normalizedNode);
        PermissionRepository.save();
        clearCache();
        return true;
    }

    public static boolean removeGroupContextNode(String groupId, String selector, String node) {
        String group = normalizePlain(groupId);
        String normalizedSelector = normalizeSelector(selector);
        String normalizedNode = normalizeNode(node);
        PermissionConfigJson.GroupJson groupJson = PermissionRepository.get().groups.get(group);
        if (groupJson == null) {
            return false;
        }
        List<String> list = groupJson.contextNodes.get(normalizedSelector);
        if (list == null || !list.remove(normalizedNode)) {
            return false;
        }
        PermissionMutationHistoryService.pushSnapshot("removeGroupContextNode");
        if (list.isEmpty()) {
            groupJson.contextNodes.remove(normalizedSelector);
        }
        PermissionRepository.save();
        clearCache();
        return true;
    }

    public static boolean addUserContextNode(UUID playerId, String selector, String node) {
        String uuid = normalizePlain(playerId.toString());
        String normalizedSelector = normalizeSelector(selector);
        String normalizedNode = normalizeNode(node);
        if (normalizedSelector.isBlank() || normalizedNode.isBlank()) {
            return false;
        }
        PermissionConfigJson data = PermissionRepository.get();
        Map<String, List<String>> contexts = data.userContextNodes.computeIfAbsent(uuid, ignored -> new LinkedHashMap<>());
        List<String> list = contexts.computeIfAbsent(normalizedSelector, ignored -> new ArrayList<>());
        if (list.contains(normalizedNode)) {
            return false;
        }
        PermissionMutationHistoryService.pushSnapshot("addUserContextNode");
        list.add(normalizedNode);
        PermissionRepository.save();
        clearCache();
        return true;
    }

    public static boolean removeUserContextNode(UUID playerId, String selector, String node) {
        String uuid = normalizePlain(playerId.toString());
        String normalizedSelector = normalizeSelector(selector);
        String normalizedNode = normalizeNode(node);
        PermissionConfigJson data = PermissionRepository.get();
        Map<String, List<String>> contexts = data.userContextNodes.get(uuid);
        if (contexts == null) {
            return false;
        }
        List<String> list = contexts.get(normalizedSelector);
        if (list == null || !list.remove(normalizedNode)) {
            return false;
        }
        PermissionMutationHistoryService.pushSnapshot("removeUserContextNode");
        if (list.isEmpty()) {
            contexts.remove(normalizedSelector);
        }
        if (contexts.isEmpty()) {
            data.userContextNodes.remove(uuid);
        }
        PermissionRepository.save();
        clearCache();
        return true;
    }

    public static boolean setGroupWeight(String groupId, int weight) {
        String group = normalizePlain(groupId);
        PermissionConfigJson.GroupJson groupJson = PermissionRepository.get().groups.get(group);
        if (groupJson == null) {
            return false;
        }
        PermissionMutationHistoryService.pushSnapshot("setGroupWeight");
        groupJson.weight = weight;
        PermissionRepository.save();
        clearCache();
        return true;
    }

    public static boolean setGroupPrefix(String groupId, String prefix) {
        String group = normalizePlain(groupId);
        PermissionConfigJson.GroupJson groupJson = PermissionRepository.get().groups.get(group);
        if (groupJson == null) {
            return false;
        }
        PermissionMutationHistoryService.pushSnapshot("setGroupPrefix");
        groupJson.prefix = prefix == null ? "" : prefix.trim();
        PermissionRepository.save();
        clearCache();
        return true;
    }

    public static boolean setGroupMeta(String groupId, String key, String value) {
        String group = normalizePlain(groupId);
        String normalizedKey = normalizePlain(key);
        if (normalizedKey.isBlank()) {
            return false;
        }
        PermissionConfigJson.GroupJson groupJson = PermissionRepository.get().groups.get(group);
        if (groupJson == null) {
            return false;
        }
        PermissionMutationHistoryService.pushSnapshot("setGroupMeta");
        groupJson.meta.put(normalizedKey, value == null ? "" : value.trim());
        PermissionRepository.save();
        clearCache();
        return true;
    }

    public static boolean undoLastMutation() {
        boolean restored = PermissionMutationHistoryService.undoLast();
        if (restored) {
            clearCache();
        }
        return restored;
    }

    public static int mutationHistorySize() {
        return PermissionMutationHistoryService.size();
    }

    private static Set<String> resolveNodes(UUID playerId) {
        PermissionConfigJson data = PermissionRepository.get();
        String uuid = normalizePlain(playerId.toString());
        String group = data.users.getOrDefault(uuid, "player");

        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        resolveGroupNodes(group, data.groups, resolved, new LinkedHashSet<>());
        for (String node : data.userNodes.getOrDefault(uuid, List.of())) {
            String normalized = normalizeNode(node);
            if (!normalized.isBlank()) {
                resolved.add(normalized);
            }
        }
        long nowSec = System.currentTimeMillis() / 1000L;
        for (PermissionConfigJson.TempNodeJson temp : data.userTempNodes.getOrDefault(uuid, List.of())) {
            if (temp == null) {
                continue;
            }
            if (temp.expiresAtEpochSec > 0L && temp.expiresAtEpochSec <= nowSec) {
                continue;
            }
            String normalized = normalizeNode(temp.node);
            if (!normalized.isBlank()) {
                resolved.add(normalized);
            }
        }
        return resolved;
    }

    private static Set<String> resolveEffectiveNodes(UUID playerId, PermissionContext context) {
        LinkedHashSet<String> out = new LinkedHashSet<>(RESOLVED_CACHE.computeIfAbsent(playerId, RpgPermissionService::resolveNodes));
        out.addAll(resolveContextNodes(playerId, context));
        return out;
    }

    private static Set<String> resolveContextNodes(UUID playerId, PermissionContext context) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        PermissionConfigJson data = PermissionRepository.get();
        String uuid = normalizePlain(playerId.toString());
        String group = data.users.getOrDefault(uuid, "player");
        resolveGroupContextNodes(group, data.groups, out, new LinkedHashSet<>(), context);

        Map<String, List<String>> userContexts = data.userContextNodes.getOrDefault(uuid, Map.of());
        for (var entry : userContexts.entrySet()) {
            if (!context.matchesSelector(entry.getKey())) {
                continue;
            }
            for (String node : entry.getValue() == null ? List.<String>of() : entry.getValue()) {
                String normalized = normalizeNode(node);
                if (!normalized.isBlank()) {
                    out.add(normalized);
                }
            }
        }
        return out;
    }

    private static void resolveGroupNodes(
            String groupId,
            Map<String, PermissionConfigJson.GroupJson> groups,
            Set<String> out,
            Set<String> seen
    ) {
        String normalizedGroup = normalizePlain(groupId);
        if (normalizedGroup.isBlank() || !seen.add(normalizedGroup)) {
            return;
        }

        PermissionConfigJson.GroupJson group = groups.get(normalizedGroup);
        if (group == null) {
            return;
        }

        if (group.parent != null && !group.parent.isBlank()) {
            resolveGroupNodes(group.parent, groups, out, seen);
        }
        for (String node : group.nodes) {
            String normalized = normalizeNode(node);
            if (!normalized.isBlank()) {
                out.add(normalized);
            }
        }
    }

    private static void resolveGroupContextNodes(
            String groupId,
            Map<String, PermissionConfigJson.GroupJson> groups,
            Set<String> out,
            Set<String> seen,
            PermissionContext context
    ) {
        String normalizedGroup = normalizePlain(groupId);
        if (normalizedGroup.isBlank() || !seen.add(normalizedGroup)) {
            return;
        }
        PermissionConfigJson.GroupJson group = groups.get(normalizedGroup);
        if (group == null) {
            return;
        }
        if (group.parent != null && !group.parent.isBlank()) {
            resolveGroupContextNodes(group.parent, groups, out, seen, context);
        }
        for (var entry : group.contextNodes.entrySet()) {
            if (!context.matchesSelector(entry.getKey())) {
                continue;
            }
            for (String node : entry.getValue() == null ? List.<String>of() : entry.getValue()) {
                String normalized = normalizeNode(node);
                if (!normalized.isBlank()) {
                    out.add(normalized);
                }
            }
        }
    }

    private static List<String> expandCandidates(String node) {
        ArrayDeque<String> candidates = new ArrayDeque<>();
        candidates.add(node);

        String cursor = node;
        while (cursor.contains(".")) {
            cursor = cursor.substring(0, cursor.lastIndexOf('.'));
            candidates.add(cursor + ".*");
        }
        candidates.add("*");
        return new ArrayList<>(candidates);
    }

    private static PermissionEvaluation evaluate(Set<String> granted, String requestedNode) {
        for (String candidate : expandCandidates(requestedNode)) {
            String denyCandidate = "-" + candidate;
            if (granted.contains(denyCandidate)) {
                return new PermissionEvaluation(requestedNode, false, candidate, "negative");
            }
            if (granted.contains(candidate)) {
                return new PermissionEvaluation(requestedNode, true, candidate, "positive");
            }
        }
        return new PermissionEvaluation(requestedNode, false, "-", "none");
    }

    private static String normalizePlain(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 권한 노드 정규화.
     * - 선행 '-' 는 음수 노드(거부) 표기로 허용한다.
     * - 내부 공백 제거로 운영자 입력 실수를 줄인다.
     */
    private static String normalizeNode(String value) {
        String raw = normalizePlain(value).replace(" ", "");
        if (raw.isBlank()) {
            return "";
        }
        boolean negative = raw.startsWith("-");
        String node = negative ? raw.substring(1) : raw;
        if (node.isBlank()) {
            return "";
        }
        return negative ? "-" + node : node;
    }

    private static String normalizeSelector(String selector) {
        String normalized = normalizePlain(selector).replace(" ", "");
        return normalized.contains("=") ? normalized : "";
    }

    public static GroupMetaView metaOf(UUID playerId) {
        PermissionConfigJson data = PermissionRepository.get();
        String groupId = groupOf(playerId);
        LinkedHashMap<String, String> mergedMeta = new LinkedHashMap<>();
        String prefix = "";
        int bestWeight = Integer.MIN_VALUE;
        for (PermissionConfigJson.GroupJson group : groupChain(groupId, data.groups)) {
            if (group.weight >= bestWeight && group.prefix != null && !group.prefix.isBlank()) {
                bestWeight = group.weight;
                prefix = group.prefix;
            }
            for (var meta : group.meta.entrySet()) {
                mergedMeta.put(meta.getKey(), meta.getValue());
            }
        }
        Map<String, String> userMeta = data.userMeta.getOrDefault(normalizePlain(playerId.toString()), Map.of());
        mergedMeta.putAll(userMeta);
        return new GroupMetaView(prefix, bestWeight == Integer.MIN_VALUE ? 0 : bestWeight, Map.copyOf(mergedMeta));
    }

    private static List<PermissionConfigJson.GroupJson> groupChain(String startGroup, Map<String, PermissionConfigJson.GroupJson> groups) {
        ArrayList<PermissionConfigJson.GroupJson> chain = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        String cursor = normalizePlain(startGroup);
        while (!cursor.isBlank() && seen.add(cursor)) {
            PermissionConfigJson.GroupJson group = groups.get(cursor);
            if (group == null) {
                break;
            }
            chain.add(group);
            cursor = normalizePlain(group.parent);
        }
        java.util.Collections.reverse(chain);
        return chain;
    }

    public record PermissionEvaluation(String requestedNode, boolean allowed, String matchedCandidate, String reason) {
    }

    public record GroupMetaView(String prefix, int weight, Map<String, String> meta) {
    }

    public record PermissionContext(Map<String, String> values) {
        public PermissionContext {
            values = values == null ? Map.of() : Map.copyOf(values);
        }

        public static PermissionContext empty() {
            return new PermissionContext(Map.of());
        }

        public static PermissionContext fromPlayer(ServerPlayer player) {
            return new PermissionContext(PermissionContextResolver.resolve(player));
        }

        public boolean matchesSelector(String selectorRaw) {
            String selector = selectorRaw == null ? "" : selectorRaw.trim().toLowerCase(Locale.ROOT);
            int sep = selector.indexOf('=');
            if (sep <= 0 || sep >= selector.length() - 1) {
                return false;
            }
            String key = selector.substring(0, sep);
            String expected = selector.substring(sep + 1);
            String actual = values.getOrDefault(key, "");
            return "*".equals(expected) || expected.equals(actual);
        }
    }

    public record PermissionView(
            String groupId,
            String prefix,
            int weight,
            boolean permissionAdmin,
            boolean classAdmin,
            boolean statsAdmin,
            boolean proficiencyAdmin,
            boolean debugAdmin,
            boolean permUiOpen,
            boolean skillTreeUiOpen,
            boolean partyManage,
            boolean partyForceKick,
            boolean guildManage,
            boolean titleManage
    ) {
    }
}

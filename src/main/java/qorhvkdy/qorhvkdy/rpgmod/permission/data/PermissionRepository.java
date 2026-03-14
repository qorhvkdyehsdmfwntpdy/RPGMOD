package qorhvkdy.qorhvkdy.rpgmod.permission.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * permissions.json 저장소.
 */
public final class PermissionRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("permissions.json");

    private static PermissionConfigJson cached = new PermissionConfigJson();

    private PermissionRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize permissions.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload permissions.json", e);
        }
    }

    public static PermissionConfigJson get() {
        return cached;
    }

    public static synchronized void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(cached, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save permissions.json", e);
        }
    }

    public static synchronized String snapshotJson() {
        return GSON.toJson(cached);
    }

    public static synchronized boolean restoreFromJson(String rawJson) {
        try {
            PermissionConfigJson data = GSON.fromJson(rawJson, PermissionConfigJson.class);
            if (data == null) {
                return false;
            }
            sanitize(data);
            cached = data;
            save();
            return true;
        } catch (RuntimeException e) {
            LOGGER.error("Failed to restore permissions snapshot", e);
            return false;
        }
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            PermissionConfigJson data = GSON.fromJson(reader, PermissionConfigJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty permissions.json");
            }
            sanitize(data);
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid permissions.json. Falling back to defaults.", e);
            cached = new PermissionConfigJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new PermissionConfigJson(), writer);
        }
    }

    private static void sanitize(PermissionConfigJson data) {
        if (data.groups == null) {
            data.groups = new LinkedHashMap<>();
        }
        if (!data.groups.containsKey("player")) {
            data.groups.put("player", new PermissionConfigJson.GroupJson());
        }
        data.groups = normalizedGroups(data.groups);

        if (data.users == null) {
            data.users = new LinkedHashMap<>();
        }
        LinkedHashMap<String, String> normalizedUsers = new LinkedHashMap<>();
        for (var entry : data.users.entrySet()) {
            String uuid = normalize(entry.getKey());
            String group = normalize(entry.getValue());
            if (uuid.isBlank()) {
                continue;
            }
            normalizedUsers.put(uuid, data.groups.containsKey(group) ? group : "player");
        }
        data.users = normalizedUsers;

        if (data.userNodes == null) {
            data.userNodes = new LinkedHashMap<>();
        }
        LinkedHashMap<String, List<String>> normalizedUserNodes = new LinkedHashMap<>();
        for (var entry : data.userNodes.entrySet()) {
            String uuid = normalize(entry.getKey());
            if (uuid.isBlank()) {
                continue;
            }
            ArrayList<String> nodes = new ArrayList<>();
            for (String node : entry.getValue() == null ? List.<String>of() : entry.getValue()) {
                String normalizedNode = normalize(node);
                if (!normalizedNode.isBlank()) {
                    nodes.add(normalizedNode);
                }
            }
            normalizedUserNodes.put(uuid, nodes);
        }
        data.userNodes = normalizedUserNodes;

        if (data.userTempNodes == null) {
            data.userTempNodes = new LinkedHashMap<>();
        }
        LinkedHashMap<String, List<PermissionConfigJson.TempNodeJson>> normalizedTempNodes = new LinkedHashMap<>();
        for (var entry : data.userTempNodes.entrySet()) {
            String uuid = normalize(entry.getKey());
            if (uuid.isBlank()) {
                continue;
            }
            ArrayList<PermissionConfigJson.TempNodeJson> list = new ArrayList<>();
            long nowSec = System.currentTimeMillis() / 1000L;
            for (PermissionConfigJson.TempNodeJson temp : entry.getValue() == null ? List.<PermissionConfigJson.TempNodeJson>of() : entry.getValue()) {
                if (temp == null) {
                    continue;
                }
                String node = normalize(temp.node);
                if (node.isBlank()) {
                    continue;
                }
                if (temp.expiresAtEpochSec > 0 && temp.expiresAtEpochSec <= nowSec) {
                    continue;
                }
                PermissionConfigJson.TempNodeJson copy = new PermissionConfigJson.TempNodeJson();
                copy.node = node;
                copy.expiresAtEpochSec = Math.max(0L, temp.expiresAtEpochSec);
                list.add(copy);
            }
            normalizedTempNodes.put(uuid, list);
        }
        data.userTempNodes = normalizedTempNodes;

        if (data.userContextNodes == null) {
            data.userContextNodes = new LinkedHashMap<>();
        }
        LinkedHashMap<String, Map<String, List<String>>> normalizedContextNodes = new LinkedHashMap<>();
        for (var entry : data.userContextNodes.entrySet()) {
            String uuid = normalize(entry.getKey());
            if (uuid.isBlank()) {
                continue;
            }
            LinkedHashMap<String, List<String>> perContext = new LinkedHashMap<>();
            Map<String, List<String>> source = entry.getValue() == null ? Map.of() : entry.getValue();
            for (var contextEntry : source.entrySet()) {
                String selector = normalize(contextEntry.getKey());
                if (selector.isBlank()) {
                    continue;
                }
                ArrayList<String> nodes = new ArrayList<>();
                for (String node : contextEntry.getValue() == null ? List.<String>of() : contextEntry.getValue()) {
                    String normalizedNode = normalize(node);
                    if (!normalizedNode.isBlank()) {
                        nodes.add(normalizedNode);
                    }
                }
                perContext.put(selector, nodes);
            }
            normalizedContextNodes.put(uuid, perContext);
        }
        data.userContextNodes = normalizedContextNodes;

        if (data.userMeta == null) {
            data.userMeta = new LinkedHashMap<>();
        }
        LinkedHashMap<String, Map<String, String>> normalizedUserMeta = new LinkedHashMap<>();
        for (var entry : data.userMeta.entrySet()) {
            String uuid = normalize(entry.getKey());
            if (uuid.isBlank()) {
                continue;
            }
            LinkedHashMap<String, String> meta = new LinkedHashMap<>();
            Map<String, String> source = entry.getValue() == null ? Map.of() : entry.getValue();
            for (var metaEntry : source.entrySet()) {
                String key = normalize(metaEntry.getKey());
                String value = normalize(metaEntry.getValue());
                if (!key.isBlank()) {
                    meta.put(key, value);
                }
            }
            normalizedUserMeta.put(uuid, meta);
        }
        data.userMeta = normalizedUserMeta;
    }

    private static LinkedHashMap<String, PermissionConfigJson.GroupJson> normalizedGroups(
            java.util.Map<String, PermissionConfigJson.GroupJson> source
    ) {
        LinkedHashMap<String, PermissionConfigJson.GroupJson> result = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            String groupId = normalize(entry.getKey());
            if (groupId.isBlank()) {
                continue;
            }
            PermissionConfigJson.GroupJson in = entry.getValue();
            PermissionConfigJson.GroupJson out = new PermissionConfigJson.GroupJson();
            out.parent = normalize(in == null ? "" : in.parent);
            out.weight = in == null ? 0 : in.weight;
            out.prefix = normalize(in == null ? "" : in.prefix);
            out.meta = new LinkedHashMap<>();
            if (in != null && in.meta != null) {
                for (var meta : in.meta.entrySet()) {
                    String key = normalize(meta.getKey());
                    String value = normalize(meta.getValue());
                    if (!key.isBlank()) {
                        out.meta.put(key, value);
                    }
                }
            }
            out.nodes = new ArrayList<>();
            if (in != null && in.nodes != null) {
                for (String node : in.nodes) {
                    String normalizedNode = normalize(node);
                    if (!normalizedNode.isBlank()) {
                        out.nodes.add(normalizedNode);
                    }
                }
            }
            out.contextNodes = new LinkedHashMap<>();
            if (in != null && in.contextNodes != null) {
                for (var contextEntry : in.contextNodes.entrySet()) {
                    String selector = normalize(contextEntry.getKey());
                    if (selector.isBlank()) {
                        continue;
                    }
                    ArrayList<String> nodes = new ArrayList<>();
                    for (String node : contextEntry.getValue() == null ? List.<String>of() : contextEntry.getValue()) {
                        String normalizedNode = normalize(node);
                        if (!normalizedNode.isBlank()) {
                            nodes.add(normalizedNode);
                        }
                    }
                    out.contextNodes.put(selector, nodes);
                }
            }
            result.put(groupId, out);
        }
        return result;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

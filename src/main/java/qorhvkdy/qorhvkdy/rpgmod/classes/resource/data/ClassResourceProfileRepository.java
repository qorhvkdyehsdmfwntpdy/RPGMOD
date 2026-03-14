package qorhvkdy.qorhvkdy.rpgmod.classes.resource.data;

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
import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * class-resource-profiles.json 저장소.
 */
public final class ClassResourceProfileRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassResourceProfileRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("class-resource-profiles.json");

    private static ClassResourceProfileJson cached = new ClassResourceProfileJson();

    private ClassResourceProfileRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize class-resource-profiles.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload class-resource-profiles.json", e);
        }
    }

    public static ClassResourceProfileJson get() {
        return cached;
    }

    public static synchronized boolean setProfileNumeric(String classIdRaw, String fieldRaw, double value) {
        String classId = normalize(classIdRaw);
        String field = normalize(fieldRaw);
        if (classId.isBlank() || field.isBlank()) {
            return false;
        }
        ClassResourceProfileJson.Profile profile = cached.profiles.get(classId);
        if (profile == null) {
            return false;
        }
        switch (field) {
            case "maxbase" -> profile.maxBase = Math.max(0.0, value);
            case "maxperlevel" -> profile.maxPerLevel = Math.max(0.0, value);
            case "regenpersecond" -> profile.regenPerSecond = Math.max(0.0, value);
            case "gainonhit" -> profile.gainOnHit = Math.max(0.0, value);
            case "gainoncrit" -> profile.gainOnCrit = Math.max(0.0, value);
            case "gainonkill" -> profile.gainOnKill = Math.max(0.0, value);
            default -> {
                return false;
            }
        }
        save();
        return true;
    }

    public static synchronized Double getProfileNumeric(String classIdRaw, String fieldRaw) {
        String classId = normalize(classIdRaw);
        String field = normalize(fieldRaw);
        if (classId.isBlank() || field.isBlank()) {
            return null;
        }
        ClassResourceProfileJson.Profile profile = cached.profiles.get(classId);
        if (profile == null) {
            return null;
        }
        return switch (field) {
            case "maxbase" -> profile.maxBase;
            case "maxperlevel" -> profile.maxPerLevel;
            case "regenpersecond" -> profile.regenPerSecond;
            case "gainonhit" -> profile.gainOnHit;
            case "gainoncrit" -> profile.gainOnCrit;
            case "gainonkill" -> profile.gainOnKill;
            default -> null;
        };
    }

    public static synchronized void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(cached, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save class-resource-profiles.json", e);
        }
    }

    public static synchronized String snapshotJson() {
        return GSON.toJson(cached);
    }

    public static synchronized boolean restoreFromJson(String rawJson) {
        try {
            ClassResourceProfileJson data = GSON.fromJson(rawJson, ClassResourceProfileJson.class);
            if (data == null || data.profiles == null) {
                return false;
            }
            cached = data;
            save();
            return true;
        } catch (RuntimeException e) {
            LOGGER.error("Failed to restore class-resource snapshot", e);
            return false;
        }
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            ClassResourceProfileJson data = GSON.fromJson(reader, ClassResourceProfileJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty class-resource-profiles.json");
            }
            if (data.profiles == null) {
                data.profiles = new LinkedHashMap<>();
            }
            if (!data.profiles.containsKey("none")) {
                data.profiles.put("none", new ClassResourceProfileJson.Profile());
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid class-resource-profiles.json. Falling back to defaults.", e);
            cached = new ClassResourceProfileJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new ClassResourceProfileJson(), writer);
        }
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace("_", "");
    }
}

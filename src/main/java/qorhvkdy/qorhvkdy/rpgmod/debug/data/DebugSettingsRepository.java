package qorhvkdy.qorhvkdy.rpgmod.debug.data;

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

/**
 * debug-settings.json 저장소.
 */
public final class DebugSettingsRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebugSettingsRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("debug-settings.json");

    private static DebugSettingsJson cached = new DebugSettingsJson();

    private DebugSettingsRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize debug-settings.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload debug-settings.json", e);
        }
    }

    public static synchronized void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(cached, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save debug-settings.json", e);
        }
    }

    public static DebugSettingsJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            DebugSettingsJson data = GSON.fromJson(reader, DebugSettingsJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty debug-settings.json");
            }
            sanitize(data);
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid debug-settings.json. Falling back to defaults.", e);
            cached = new DebugSettingsJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new DebugSettingsJson(), writer);
        }
    }

    private static void sanitize(DebugSettingsJson data) {
        if (data.progressionLogLevel == null) {
            data.progressionLogLevel = "info";
        }
        String normalized = data.progressionLogLevel.trim().toLowerCase();
        if (!normalized.equals("off") && !normalized.equals("info") && !normalized.equals("debug")) {
            data.progressionLogLevel = "info";
        } else {
            data.progressionLogLevel = normalized;
        }
    }
}

package qorhvkdy.qorhvkdy.rpgmod.core.runtime;

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
 * runtime-features.json 저장소.
 */
public final class RuntimeFeatureRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeFeatureRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("runtime-features.json");

    private static RuntimeFeatureJson cached = new RuntimeFeatureJson();

    private RuntimeFeatureRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize runtime-features.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload runtime-features.json", e);
        }
    }

    public static RuntimeFeatureJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            RuntimeFeatureJson data = GSON.fromJson(reader, RuntimeFeatureJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty runtime-features.json");
            }
            if (data.moduleEnabled == null) {
                data.moduleEnabled = new RuntimeFeatureJson().moduleEnabled;
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid runtime-features.json. Falling back to defaults.", e);
            cached = new RuntimeFeatureJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new RuntimeFeatureJson(), writer);
        }
    }
}

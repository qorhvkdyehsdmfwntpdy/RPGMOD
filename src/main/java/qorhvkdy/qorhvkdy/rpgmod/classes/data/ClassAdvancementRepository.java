package qorhvkdy.qorhvkdy.rpgmod.classes.data;

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
 * Repository for class advancement tree configuration.
 */
public final class ClassAdvancementRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassAdvancementRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("class-advancements.json");

    private static ClassAdvancementJson cached = new ClassAdvancementJson();

    private ClassAdvancementRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize class-advancements.json", e);
        }
    }

    public static ClassAdvancementJson get() {
        return cached;
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload class-advancements.json", e);
        }
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            ClassAdvancementJson data = GSON.fromJson(reader, ClassAdvancementJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty class-advancements.json");
            }
            validate(data);
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid class-advancements.json. Falling back to defaults.", e);
            cached = new ClassAdvancementJson();
            writeDefault();
        }
    }

    private static void validate(ClassAdvancementJson data) {
        if (data.dataVersion < 1) {
            throw new IllegalStateException("Invalid dataVersion in class-advancements.json");
        }
        if (data.advancements == null || data.advancements.isEmpty()) {
            throw new IllegalStateException("No advancement entries found.");
        }
        for (ClassAdvancementJson.Entry entry : data.advancements) {
            if (entry.id == null || entry.id.isBlank()) {
                throw new IllegalStateException("Advancement id cannot be empty.");
            }
            if (entry.baseClass == null || entry.baseClass.isBlank()) {
                throw new IllegalStateException("baseClass missing for " + entry.id);
            }
            if (entry.tier == null || entry.tier.isBlank()) {
                throw new IllegalStateException("tier missing for " + entry.id);
            }
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new ClassAdvancementJson(), writer);
        }
    }
}

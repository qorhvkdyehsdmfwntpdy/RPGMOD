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
 * 클래스 패시브 템플릿 로더.
 */
public final class ClassPassiveRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassPassiveRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("class-passives.json");

    private static ClassPassiveJson cached = new ClassPassiveJson();

    private ClassPassiveRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize class-passives.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload class-passives.json", e);
        }
    }

    public static ClassPassiveJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            ClassPassiveJson data = GSON.fromJson(reader, ClassPassiveJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty class-passives.json");
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid class-passives.json. Falling back to defaults.", e);
            cached = new ClassPassiveJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new ClassPassiveJson(), writer);
        }
    }
}


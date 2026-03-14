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
 * 클래스 패시브 효과 로더.
 */
public final class ClassPassiveEffectRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassPassiveEffectRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("class-passive-effects.json");

    private static ClassPassiveEffectJson cached = new ClassPassiveEffectJson();

    private ClassPassiveEffectRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize class-passive-effects.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload class-passive-effects.json", e);
        }
    }

    public static ClassPassiveEffectJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            ClassPassiveEffectJson data = GSON.fromJson(reader, ClassPassiveEffectJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty class-passive-effects.json");
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid class-passive-effects.json. Falling back to defaults.", e);
            cached = new ClassPassiveEffectJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new ClassPassiveEffectJson(), writer);
        }
    }
}


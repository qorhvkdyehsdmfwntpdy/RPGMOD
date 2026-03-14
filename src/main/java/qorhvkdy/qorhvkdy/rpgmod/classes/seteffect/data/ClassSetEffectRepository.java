package qorhvkdy.qorhvkdy.rpgmod.classes.seteffect.data;

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
 * class-set-effects.json 저장소.
 */
public final class ClassSetEffectRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassSetEffectRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("class-set-effects.json");

    private static ClassSetEffectJson cached = new ClassSetEffectJson();

    private ClassSetEffectRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize class-set-effects.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload class-set-effects.json", e);
        }
    }

    public static ClassSetEffectJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            ClassSetEffectJson data = GSON.fromJson(reader, ClassSetEffectJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty class-set-effects.json");
            }
            if (data.sets == null) {
                data.sets = new java.util.ArrayList<>();
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid class-set-effects.json. Falling back to defaults.", e);
            cached = new ClassSetEffectJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new ClassSetEffectJson(), writer);
        }
    }
}

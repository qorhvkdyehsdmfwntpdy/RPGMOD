package qorhvkdy.qorhvkdy.rpgmod.skill.data;

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
 * skill-mechanics.json 저장소.
 */
public final class SkillMechanicRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillMechanicRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("skill-mechanics.json");

    private static SkillMechanicJson cached = new SkillMechanicJson();

    private SkillMechanicRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize skill-mechanics.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload skill-mechanics.json", e);
        }
    }

    public static SkillMechanicJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            SkillMechanicJson data = GSON.fromJson(reader, SkillMechanicJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty skill-mechanics.json");
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid skill-mechanics.json. Falling back to defaults.", e);
            cached = new SkillMechanicJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new SkillMechanicJson(), writer);
        }
    }
}

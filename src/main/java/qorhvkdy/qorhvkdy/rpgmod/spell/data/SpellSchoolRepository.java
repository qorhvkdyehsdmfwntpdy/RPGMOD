package qorhvkdy.qorhvkdy.rpgmod.spell.data;

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
 * spell-schools.json 저장소.
 */
public final class SpellSchoolRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpellSchoolRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("spell-schools.json");

    private static SpellSchoolJson cached = new SpellSchoolJson();

    private SpellSchoolRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize spell-schools.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload spell-schools.json", e);
        }
    }

    public static SpellSchoolJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            SpellSchoolJson data = GSON.fromJson(reader, SpellSchoolJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty spell-schools.json");
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid spell-schools.json. Falling back to defaults.", e);
            cached = new SpellSchoolJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new SpellSchoolJson(), writer);
        }
    }
}

package qorhvkdy.qorhvkdy.rpgmod.proficiency.data;

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
 * proficiency-sources.json 저장소.
 */
public final class ProficiencySourceRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProficiencySourceRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("proficiency-sources.json");

    private static ProficiencySourceJson cached = new ProficiencySourceJson();

    private ProficiencySourceRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize proficiency-sources.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload proficiency-sources.json", e);
        }
    }

    public static ProficiencySourceJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            ProficiencySourceJson data = GSON.fromJson(reader, ProficiencySourceJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty proficiency-sources.json");
            }
            if (data.sources == null) {
                data.sources = new ProficiencySourceJson().sources;
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid proficiency-sources.json. Falling back to defaults.", e);
            cached = new ProficiencySourceJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new ProficiencySourceJson(), writer);
        }
    }
}

package qorhvkdy.qorhvkdy.rpgmod.combat.profile.data;

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
 * combat-profiles.json 저장소.
 */
public final class CombatProfileRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(CombatProfileRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("combat-profiles.json");

    private static CombatProfileJson cached = new CombatProfileJson();

    private CombatProfileRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize combat-profiles.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload combat-profiles.json", e);
        }
    }

    public static CombatProfileJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            CombatProfileJson data = GSON.fromJson(reader, CombatProfileJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty combat-profiles.json");
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid combat-profiles.json. Falling back to defaults.", e);
            cached = new CombatProfileJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new CombatProfileJson(), writer);
        }
    }
}

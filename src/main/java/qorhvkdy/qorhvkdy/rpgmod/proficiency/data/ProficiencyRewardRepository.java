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
 * 숙련도 획득량 설정 로더.
 */
public final class ProficiencyRewardRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProficiencyRewardRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("proficiency-rewards.json");

    private static ProficiencyRewardJson cached = new ProficiencyRewardJson();

    private ProficiencyRewardRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize proficiency-rewards.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload proficiency-rewards.json", e);
        }
    }

    public static ProficiencyRewardJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            ProficiencyRewardJson data = GSON.fromJson(reader, ProficiencyRewardJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty proficiency-rewards.json");
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid proficiency-rewards.json. Falling back to defaults.", e);
            cached = new ProficiencyRewardJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new ProficiencyRewardJson(), writer);
        }
    }
}


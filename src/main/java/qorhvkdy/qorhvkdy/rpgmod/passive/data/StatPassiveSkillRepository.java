package qorhvkdy.qorhvkdy.rpgmod.passive.data;

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
 * 스탯 패시브 스킬 테이블 로더.
 */
public final class StatPassiveSkillRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatPassiveSkillRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("stat-passive-skills.json");

    private static StatPassiveSkillJson cached = new StatPassiveSkillJson();

    private StatPassiveSkillRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize stat-passive-skills.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload stat-passive-skills.json", e);
        }
    }

    public static StatPassiveSkillJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            StatPassiveSkillJson data = GSON.fromJson(reader, StatPassiveSkillJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty stat-passive-skills.json");
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid stat-passive-skills.json. Falling back to defaults.", e);
            cached = new StatPassiveSkillJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new StatPassiveSkillJson(), writer);
        }
    }
}


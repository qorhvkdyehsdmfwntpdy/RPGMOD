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
 * 블록별 숙련도 보정치 설정 로더.
 */
public final class ProficiencyBlockRuleRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProficiencyBlockRuleRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("proficiency-block-rules.json");

    private static ProficiencyBlockRuleJson cached = new ProficiencyBlockRuleJson();

    private ProficiencyBlockRuleRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize proficiency-block-rules.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload proficiency-block-rules.json", e);
        }
    }

    public static ProficiencyBlockRuleJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            ProficiencyBlockRuleJson data = GSON.fromJson(reader, ProficiencyBlockRuleJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty proficiency-block-rules.json");
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid proficiency-block-rules.json. Falling back to defaults.", e);
            cached = new ProficiencyBlockRuleJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new ProficiencyBlockRuleJson(), writer);
        }
    }
}


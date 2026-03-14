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
 * 숙련도 밸런스 로더.
 * JSON 수정 즉시 재로드할 수 있도록 별도 저장소로 분리한다.
 */
public final class ProficiencyBalanceRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProficiencyBalanceRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("proficiency-balance.json");

    private static ProficiencyBalanceJson cached = new ProficiencyBalanceJson();

    private ProficiencyBalanceRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize proficiency-balance.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload proficiency-balance.json", e);
        }
    }

    public static ProficiencyBalanceJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            ProficiencyBalanceJson data = GSON.fromJson(reader, ProficiencyBalanceJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty proficiency-balance.json");
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid proficiency-balance.json. Falling back to defaults.", e);
            cached = new ProficiencyBalanceJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new ProficiencyBalanceJson(), writer);
        }
    }
}


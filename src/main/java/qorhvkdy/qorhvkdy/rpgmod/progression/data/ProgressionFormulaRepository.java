package qorhvkdy.qorhvkdy.rpgmod.progression.data;

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
 * progression-formulas.json 로더.
 */
public final class ProgressionFormulaRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionFormulaRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("progression-formulas.json");

    private static ProgressionFormulaJson cached = new ProgressionFormulaJson();

    private ProgressionFormulaRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize progression-formulas.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload progression-formulas.json", e);
        }
    }

    public static ProgressionFormulaJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            ProgressionFormulaJson data = GSON.fromJson(reader, ProgressionFormulaJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty progression-formulas.json");
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid progression-formulas.json. Falling back to defaults.", e);
            cached = new ProgressionFormulaJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new ProgressionFormulaJson(), writer);
        }
    }
}

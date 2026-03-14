package qorhvkdy.qorhvkdy.rpgmod.permission.data;

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
 * permission-context-rules.json 저장소.
 */
public final class PermissionContextRuleRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionContextRuleRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("permission-context-rules.json");

    private static PermissionContextRuleJson cached = new PermissionContextRuleJson();

    private PermissionContextRuleRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize permission-context-rules.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload permission-context-rules.json", e);
        }
    }

    public static PermissionContextRuleJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            PermissionContextRuleJson data = GSON.fromJson(reader, PermissionContextRuleJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty permission-context-rules.json");
            }
            if (data.regionRules == null || data.regionRules.isEmpty()) {
                data.regionRules = new PermissionContextRuleJson().regionRules;
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid permission-context-rules.json. Falling back to defaults.", e);
            cached = new PermissionContextRuleJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new PermissionContextRuleJson(), writer);
        }
    }
}

package qorhvkdy.qorhvkdy.rpgmod.weapon.data;

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
 * 무기 데이터 로더.
 */
public final class WeaponDataRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeaponDataRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("weapons.json");

    private static WeaponDataJson cached = new WeaponDataJson();

    private WeaponDataRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize weapons.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload weapons.json", e);
        }
    }

    public static WeaponDataJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            WeaponDataJson data = GSON.fromJson(reader, WeaponDataJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty weapons.json");
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid weapons.json. Falling back to defaults.", e);
            cached = new WeaponDataJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new WeaponDataJson(), writer);
        }
    }
}


package qorhvkdy.qorhvkdy.rpgmod.weapon.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qorhvkdy.qorhvkdy.rpgmod.debug.RpgDebugSettings;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

/**
 * 무기 드랍 룰 설정 로더.
 */
public final class WeaponDropRuleRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeaponDropRuleRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("weapon-drop-rules.json");

    private static WeaponDropRuleJson cached = new WeaponDropRuleJson();

    private WeaponDropRuleRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize weapon-drop-rules.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload weapon-drop-rules.json", e);
        }
    }

    public static WeaponDropRuleJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            WeaponDropRuleJson data = GSON.fromJson(reader, WeaponDropRuleJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty weapon-drop-rules.json");
            }
            validateAndSanitize(data);
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid weapon-drop-rules.json. Falling back to defaults.", e);
            cached = new WeaponDropRuleJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new WeaponDropRuleJson(), writer);
        }
    }

    private static void validateAndSanitize(WeaponDropRuleJson data) {
        if (data.maxDropsPerKill < 1) {
            logValidation("maxDropsPerKill < 1, clamped to 1");
            data.maxDropsPerKill = 1;
        }
        if (data.globalDropChance < 0.0 || data.globalDropChance > 1.0) {
            logValidation("globalDropChance out of range, clamped to [0,1]");
            data.globalDropChance = clamp01(data.globalDropChance);
        }
        if (data.optionRollMin < 0.1) {
            logValidation("optionRollMin < 0.1, clamped to 0.1");
            data.optionRollMin = 0.1;
        }
        if (data.optionRollMax < data.optionRollMin) {
            logValidation("optionRollMax < optionRollMin, swapped");
            double temp = data.optionRollMin;
            data.optionRollMin = data.optionRollMax;
            data.optionRollMax = temp;
            if (data.optionRollMin < 0.1) {
                data.optionRollMin = 0.1;
            }
            if (data.optionRollMax < data.optionRollMin) {
                data.optionRollMax = data.optionRollMin;
            }
        }
        if (data.rarityWeight == null) {
            logValidation("rarityWeight missing, reset default");
            data.rarityWeight = new WeaponDropRuleJson().rarityWeight;
        } else {
            data.rarityWeight = new LinkedHashMap<>(data.rarityWeight);
            data.rarityWeight.replaceAll((key, value) -> value == null || value <= 0.0 ? 1.0 : value);
        }
        if (data.gradeWeight == null) {
            logValidation("gradeWeight missing, reset default");
            data.gradeWeight = new WeaponDropRuleJson().gradeWeight;
        } else {
            data.gradeWeight = new LinkedHashMap<>(data.gradeWeight);
            data.gradeWeight.replaceAll((key, value) -> value == null || value <= 0.0 ? 1.0 : value);
        }
    }

    private static void logValidation(String message) {
        if (RpgDebugSettings.jsonValidationLog()) {
            LOGGER.warn("weapon-drop-rules.json sanitized: {}", message);
        }
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}

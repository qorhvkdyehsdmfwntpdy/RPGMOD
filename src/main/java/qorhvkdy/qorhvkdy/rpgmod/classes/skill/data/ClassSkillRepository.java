package qorhvkdy.qorhvkdy.rpgmod.classes.skill.data;

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
import java.util.Locale;

/**
 * class-skills.json 저장소.
 */
public final class ClassSkillRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassSkillRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("class-skills.json");

    private static ClassSkillJson cached = new ClassSkillJson();

    private ClassSkillRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize class-skills.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload class-skills.json", e);
        }
    }

    public static ClassSkillJson get() {
        return cached;
    }

    public static synchronized boolean setSkillNumeric(String skillIdRaw, String fieldRaw, double value) {
        String skillId = normalize(skillIdRaw);
        String field = normalize(fieldRaw);
        if (skillId.isBlank() || field.isBlank()) {
            return false;
        }
        for (ClassSkillJson.Entry entry : cached.skills) {
            if (entry == null || !normalize(entry.id).equals(skillId)) {
                continue;
            }
            boolean applied = applyNumeric(entry, field, value);
            if (!applied) {
                return false;
            }
            save();
            return true;
        }
        return false;
    }

    public static synchronized Double getSkillNumeric(String skillIdRaw, String fieldRaw) {
        String skillId = normalize(skillIdRaw);
        String field = normalize(fieldRaw);
        if (skillId.isBlank() || field.isBlank()) {
            return null;
        }
        for (ClassSkillJson.Entry entry : cached.skills) {
            if (entry == null || !normalize(entry.id).equals(skillId)) {
                continue;
            }
            return readNumeric(entry, field);
        }
        return null;
    }

    public static synchronized void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(cached, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save class-skills.json", e);
        }
    }

    public static synchronized String snapshotJson() {
        return GSON.toJson(cached);
    }

    public static synchronized boolean restoreFromJson(String rawJson) {
        try {
            ClassSkillJson data = GSON.fromJson(rawJson, ClassSkillJson.class);
            if (data == null || data.skills == null) {
                return false;
            }
            cached = data;
            save();
            return true;
        } catch (RuntimeException e) {
            LOGGER.error("Failed to restore class-skills snapshot", e);
            return false;
        }
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            ClassSkillJson data = GSON.fromJson(reader, ClassSkillJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty class-skills.json");
            }
            if (data.skills == null) {
                data.skills = new java.util.ArrayList<>();
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid class-skills.json. Falling back to defaults.", e);
            cached = new ClassSkillJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new ClassSkillJson(), writer);
        }
    }

    private static boolean applyNumeric(ClassSkillJson.Entry entry, String field, double value) {
        switch (field) {
            case "requiredlevel" -> entry.requiredLevel = Math.max(1, (int) Math.round(value));
            case "minstr" -> entry.minStr = Math.max(0, (int) Math.round(value));
            case "minagi" -> entry.minAgi = Math.max(0, (int) Math.round(value));
            case "minwis" -> entry.minWis = Math.max(0, (int) Math.round(value));
            case "minluk" -> entry.minLuk = Math.max(0, (int) Math.round(value));
            case "resourcecost" -> entry.resourceCost = Math.max(0.0, value);
            case "cooldownseconds" -> entry.cooldownSeconds = Math.max(0.1, value);
            case "durationticks" -> entry.durationTicks = Math.max(1, (int) Math.round(value));
            case "bonusattackmultiplier" -> entry.bonusAttackMultiplier = Math.max(1.0, value);
            case "bonusarmor" -> entry.bonusArmor = Math.max(0.0, value);
            case "bonuscritchance" -> entry.bonusCritChance = Math.max(0.0, value);
            case "bonuscritdamage" -> entry.bonusCritDamage = Math.max(0.0, value);
            default -> {
                return false;
            }
        }
        return true;
    }

    private static Double readNumeric(ClassSkillJson.Entry entry, String field) {
        return switch (field) {
            case "requiredlevel" -> (double) entry.requiredLevel;
            case "minstr" -> (double) entry.minStr;
            case "minagi" -> (double) entry.minAgi;
            case "minwis" -> (double) entry.minWis;
            case "minluk" -> (double) entry.minLuk;
            case "resourcecost" -> entry.resourceCost;
            case "cooldownseconds" -> entry.cooldownSeconds;
            case "durationticks" -> (double) entry.durationTicks;
            case "bonusattackmultiplier" -> entry.bonusAttackMultiplier;
            case "bonusarmor" -> entry.bonusArmor;
            case "bonuscritchance" -> entry.bonusCritChance;
            case "bonuscritdamage" -> entry.bonusCritDamage;
            default -> null;
        };
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace("_", "");
    }
}

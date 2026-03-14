package qorhvkdy.qorhvkdy.rpgmod.party.data;

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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 파티 멤버십 저장소.
 * 추후 길드 시스템에서도 동일 패턴으로 확장하기 쉽게 별도 분리한다.
 */
public final class PartyMemberRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyMemberRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("party-members.json");

    private static Map<String, String> cached = new LinkedHashMap<>();

    private PartyMemberRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                save();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize party-members.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload party-members.json", e);
        }
    }

    public static Map<String, String> get() {
        return cached;
    }

    public static synchronized void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(cached, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save party-members.json", e);
        }
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            Map<?, ?> data = GSON.fromJson(reader, Map.class);
            if (data == null) {
                cached = new LinkedHashMap<>();
                return;
            }
            LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : data.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                normalized.put(entry.getKey().toString().trim().toLowerCase(), entry.getValue().toString().trim().toLowerCase());
            }
            cached = normalized;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid party-members.json. Resetting to empty map.", e);
            cached = new LinkedHashMap<>();
            save();
        }
    }
}

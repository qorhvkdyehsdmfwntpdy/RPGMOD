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
 * 파티 리더 저장소.
 * key=partyId, value=leaderUuid
 */
public final class PartyLeaderRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyLeaderRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("party-leaders.json");

    private static Map<String, String> cached = new LinkedHashMap<>();

    private PartyLeaderRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                save();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize party-leaders.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload party-leaders.json", e);
        }
    }

    public static Map<String, String> get() {
        return cached;
    }

    public static synchronized void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(cached, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save party-leaders.json", e);
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
                String partyId = entry.getKey().toString().trim().toLowerCase();
                String leaderUuid = entry.getValue().toString().trim().toLowerCase();
                if (!partyId.isBlank() && !leaderUuid.isBlank()) {
                    normalized.put(partyId, leaderUuid);
                }
            }
            cached = normalized;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid party-leaders.json. Resetting to empty map.", e);
            cached = new LinkedHashMap<>();
            save();
        }
    }
}

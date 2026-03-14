package qorhvkdy.qorhvkdy.rpgmod.npc.data;

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
 * npc-dialogues.json 저장소.
 */
public final class NpcDialogueRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(NpcDialogueRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("npc-dialogues.json");

    private static NpcDialogueJson cached = new NpcDialogueJson();

    private NpcDialogueRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize npc-dialogues.json", e);
        }
    }

    public static void reload() {
        try {
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to reload npc-dialogues.json", e);
        }
    }

    public static NpcDialogueJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            NpcDialogueJson data = GSON.fromJson(reader, NpcDialogueJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty npc-dialogues.json");
            }
            cached = data;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Invalid npc-dialogues.json. Falling back to defaults.", e);
            cached = new NpcDialogueJson();
            writeDefault();
        }
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new NpcDialogueJson(), writer);
        }
    }
}

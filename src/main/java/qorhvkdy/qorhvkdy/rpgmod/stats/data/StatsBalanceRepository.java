package qorhvkdy.qorhvkdy.rpgmod.stats.data;

/*
 * [RPGMOD 파일 설명]
 * 역할: 밸런스 JSON 로드/캐시/기본값 폴백을 담당하는 저장소입니다.
 * 수정 예시: 실시간 재로드를 붙이려면 캐시 무효화 메서드를 호출하도록 연결합니다.
 */


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

public final class StatsBalanceRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatsBalanceRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "rpgmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("stats-balance.json");

    private static StatsBalanceJson cached = new StatsBalanceJson();

    private StatsBalanceRepository() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefault();
            }
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize stats-balance.json", e);
        }
    }

    public static StatsBalanceJson get() {
        return cached;
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            StatsBalanceJson data = GSON.fromJson(reader, StatsBalanceJson.class);
            if (data == null) {
                throw new JsonSyntaxException("Empty stats-balance.json");
            }
            validate(data);
            cached = data;
        } catch (JsonSyntaxException e) {
            LOGGER.error("Invalid stats-balance.json syntax. Falling back to defaults.", e);
            cached = new StatsBalanceJson();
            writeDefault();
        }
    }

    private static void validate(StatsBalanceJson data) {
        if (data.dataVersion < 1) {
            throw new IllegalStateException("Invalid dataVersion in stats-balance.json");
        }
        data.primaryCaps.forEach((key, cap) -> {
            if (cap.softCap <= 0 || cap.hardCap <= 0 || cap.softCap >= cap.hardCap) {
                throw new IllegalStateException("Invalid cap entry for " + key);
            }
        });
    }

    private static void writeDefault() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(new StatsBalanceJson(), writer);
        }
    }
}

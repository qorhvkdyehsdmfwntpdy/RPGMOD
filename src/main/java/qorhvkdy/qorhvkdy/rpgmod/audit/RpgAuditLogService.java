package qorhvkdy.qorhvkdy.rpgmod.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 운영 감사 로그 분리 저장.
 * 권한/전직/드롭을 파일별로 기록한다.
 */
public final class RpgAuditLogService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpgAuditLogService.class);
    private static final Path LOG_DIR = Path.of("config", "rpgmod", "logs");
    private static final Path PERMISSION_LOG = LOG_DIR.resolve("permission-audit.log");
    private static final Path PROGRESSION_LOG = LOG_DIR.resolve("progression-audit.log");
    private static final Path DROP_LOG = LOG_DIR.resolve("drop-audit.log");
    private static final Path COMBAT_LOG = LOG_DIR.resolve("combat-audit.log");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private RpgAuditLogService() {
    }

    public static void bootstrap() {
        try {
            Files.createDirectories(LOG_DIR);
        } catch (IOException e) {
            LOGGER.error("Failed to create audit log directory", e);
        }
    }

    public static void permission(String line) {
        append(PERMISSION_LOG, line);
    }

    public static void progression(String line) {
        append(PROGRESSION_LOG, line);
    }

    public static void drop(String line) {
        append(DROP_LOG, line);
    }

    public static void combat(String line) {
        append(COMBAT_LOG, line);
    }

    private static synchronized void append(Path file, String line) {
        try {
            Files.createDirectories(LOG_DIR);
            String stamped = "[" + LocalDateTime.now().format(FORMATTER) + "] " + line + System.lineSeparator();
            Files.writeString(file, stamped, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.error("Failed to write audit log: {}", file.getFileName(), e);
        }
    }
}

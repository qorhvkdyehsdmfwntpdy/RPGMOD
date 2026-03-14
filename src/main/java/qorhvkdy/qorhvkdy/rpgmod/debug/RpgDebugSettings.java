package qorhvkdy.qorhvkdy.rpgmod.debug;

import qorhvkdy.qorhvkdy.rpgmod.debug.data.DebugSettingsJson;
import qorhvkdy.qorhvkdy.rpgmod.debug.data.DebugSettingsRepository;

/**
 * 디버그/운영 플래그 제공 서비스.
 */
public final class RpgDebugSettings {
    public enum LogLevel {
        OFF, INFO, DEBUG
    }

    private RpgDebugSettings() {
    }

    public static void bootstrap() {
        DebugSettingsRepository.bootstrap();
    }

    public static void reload() {
        DebugSettingsRepository.reload();
    }

    public static LogLevel progressionLogLevel() {
        String raw = DebugSettingsRepository.get().progressionLogLevel;
        if ("off".equalsIgnoreCase(raw)) {
            return LogLevel.OFF;
        }
        if ("debug".equalsIgnoreCase(raw)) {
            return LogLevel.DEBUG;
        }
        return LogLevel.INFO;
    }

    public static boolean dropVerboseLog() {
        return DebugSettingsRepository.get().dropVerboseLog;
    }

    public static boolean jsonValidationLog() {
        return DebugSettingsRepository.get().jsonValidationLog;
    }

    public static boolean combatSamplerEnabled() {
        return DebugSettingsRepository.get().combatSamplerEnabled;
    }

    public static int combatSamplerWindowSeconds() {
        return Math.max(5, DebugSettingsRepository.get().combatSamplerWindowSeconds);
    }

    public static int combatSamplerMinHits() {
        return Math.max(1, DebugSettingsRepository.get().combatSamplerMinHits);
    }

    public static void setProgressionLogLevel(LogLevel level) {
        DebugSettingsJson data = DebugSettingsRepository.get();
        data.progressionLogLevel = level.name().toLowerCase();
        DebugSettingsRepository.save();
    }
}

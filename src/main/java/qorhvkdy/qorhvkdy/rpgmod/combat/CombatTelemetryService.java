package qorhvkdy.qorhvkdy.rpgmod.combat;

import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.audit.RpgAuditLogService;
import qorhvkdy.qorhvkdy.rpgmod.debug.RpgDebugSettings;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 전투 로그 샘플러.
 * 짧은 구간(window) 단위로 유저별 평균 피해량/DPS를 계산해 감사 로그로 기록한다.
 */
public final class CombatTelemetryService {
    private static final Map<UUID, Window> WINDOWS = new ConcurrentHashMap<>();
    private static long lastFlushMillis = System.currentTimeMillis();

    private CombatTelemetryService() {
    }

    public static void recordHit(ServerPlayer attacker, float damage, boolean critical) {
        if (attacker == null || damage <= 0.0f || !RpgDebugSettings.combatSamplerEnabled()) {
            return;
        }
        Window window = WINDOWS.computeIfAbsent(attacker.getUUID(), ignored -> new Window(attacker.getScoreboardName()));
        window.hits++;
        window.totalDamage += damage;
        if (critical) {
            window.criticalHits++;
        }
    }

    /**
     * 외부 안정 이벤트(예: 플레이어 틱)에서 1초 주기로 호출하면 된다.
     */
    public static void tick() {
        if (!RpgDebugSettings.combatSamplerEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        int windowSeconds = RpgDebugSettings.combatSamplerWindowSeconds();
        if (now - lastFlushMillis < windowSeconds * 1000L) {
            return;
        }
        flush(now, windowSeconds);
    }

    private static void flush(long nowMillis, int windowSeconds) {
        int minHits = RpgDebugSettings.combatSamplerMinHits();
        for (Map.Entry<UUID, Window> entry : WINDOWS.entrySet()) {
            Window w = entry.getValue();
            if (w.hits < minHits) {
                w.reset();
                continue;
            }
            double avg = w.totalDamage / w.hits;
            double dps = w.totalDamage / Math.max(1.0, windowSeconds);
            double critRate = (w.criticalHits * 100.0) / Math.max(1, w.hits);
            RpgAuditLogService.combat(
                    "sample player=" + w.playerName
                            + ", hits=" + w.hits
                            + ", critHits=" + w.criticalHits
                            + ", critRate=" + round2(critRate) + "%"
                            + ", totalDamage=" + round2(w.totalDamage)
                            + ", avgDamage=" + round2(avg)
                            + ", dps=" + round2(dps)
                            + ", windowSec=" + windowSeconds
            );
            w.reset();
        }
        lastFlushMillis = nowMillis;
    }

    private static String round2(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static final class Window {
        private final String playerName;
        private int hits;
        private int criticalHits;
        private double totalDamage;

        private Window(String playerName) {
            this.playerName = playerName;
        }

        private void reset() {
            this.hits = 0;
            this.criticalHits = 0;
            this.totalDamage = 0.0;
        }
    }
}

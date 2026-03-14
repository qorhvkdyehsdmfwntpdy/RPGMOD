package qorhvkdy.qorhvkdy.rpgmod.permission;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.party.PartyService;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 플레이어 컨텍스트 해석기.
 * 경량 구조를 유지하면서도 대형 운영 확장을 대비해 context key를 고정 키로 분리한다.
 */
public final class PermissionContextResolver {
    private PermissionContextResolver() {
    }

    public static Map<String, String> resolve(ServerPlayer player) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("dimension", resolveDimension(player));
        values.put("world", values.get("dimension"));
        values.put("module", resolveModule(player));
        values.put("time", resolveTimeBucket(player));
        values.put("region", resolveRegion(player));
        return PermissionContextPriorityService.sorted(values);
    }

    private static String resolveDimension(ServerPlayer player) {
        return player.level().dimension().toString().toLowerCase(Locale.ROOT);
    }

    private static String resolveModule(ServerPlayer player) {
        String dimension = resolveDimension(player);
        if (dimension.contains("dungeon") || dimension.contains("instance")) {
            return "dungeon";
        }
        if (PartyService.getPartyId(player.getUUID()).isPresent()) {
            return "party";
        }
        return "default";
    }

    private static String resolveTimeBucket(ServerPlayer player) {
        long dayTime = player.level().getDayTime() % 24000L;
        return dayTime >= 13000L ? "night" : "day";
    }

    private static String resolveRegion(ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        int dx = Math.abs(pos.getX());
        int dz = Math.abs(pos.getZ());
        int distance = Math.max(dx, dz);
        String dimension = resolveDimension(player);
        String module = resolveModule(player);
        String time = resolveTimeBucket(player);
        return PermissionContextRuleService.resolveRegion(distance, dimension, module, time);
    }
}

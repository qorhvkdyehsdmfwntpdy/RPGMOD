package qorhvkdy.qorhvkdy.rpgmod.classes;

import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;

/**
 * Context used when validating a promotion requirement.
 */
public record PromotionCheckContext(
        ServerPlayer player,
        PlayerStats stats,
        ClassAdvancement current,
        ClassAdvancement target
) {
}


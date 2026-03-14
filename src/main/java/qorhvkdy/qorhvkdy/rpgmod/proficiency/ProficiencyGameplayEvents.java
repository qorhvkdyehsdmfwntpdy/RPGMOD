package qorhvkdy.qorhvkdy.rpgmod.proficiency;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;

import java.util.Objects;

/**
 * 숙련도 경험치 획득 이벤트.
 * 간단한 규칙 기반으로 시작하고, 이후 태그/컨텐츠 시스템으로 치환하기 쉽도록 구성한다.
 */
@Mod.EventBusSubscriber(modid = Rpgmod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ProficiencyGameplayEvents {
    private static final TagKey<Block> GATHERING_BLOCKS_TAG =
            TagKey.create(Registries.BLOCK, Objects.requireNonNull(Identifier.tryParse(Rpgmod.MODID + ":gathering_blocks")));
    private static final TagKey<Block> MINING_BLOCKS_TAG =
            TagKey.create(Registries.BLOCK, Objects.requireNonNull(Identifier.tryParse(Rpgmod.MODID + ":mining_blocks")));

    private ProficiencyGameplayEvents() {
    }

    @SubscribeEvent
    public static void onMobKilled(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.getKillCredit() instanceof ServerPlayer killer)) {
            return;
        }
        ProficiencySourceService.grantBySource(killer, "mob_kill.class", 1.0);
        ProficiencySourceService.grantBySource(killer, "mob_kill.weapon", 1.0);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        Block block = event.getState().getBlock();
        Identifier id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null) {
            return;
        }

        String key = id.toString().toLowerCase();

        // 한글 주석: 태그 우선, 없으면 이름 키워드로 최소한의 호환을 보장한다.
        if (event.getState().is(GATHERING_BLOCKS_TAG)
                || event.getState().is(BlockTags.LOGS)
                || key.contains("crop") || key.contains("leaf") || key.contains("plant")) {
            ProficiencySourceService.grantBySource(
                    player,
                    "block_break.gathering",
                    ProficiencyBlockRuleService.gatheringMultiplier(key)
            );
            return;
        }
        if (event.getState().is(MINING_BLOCKS_TAG)
                || key.contains("ore") || key.contains("stone") || key.contains("deepslate") || key.contains("netherrack")) {
            ProficiencySourceService.grantBySource(
                    player,
                    "block_break.mining",
                    ProficiencyBlockRuleService.miningMultiplier(key)
            );
        }
    }
}

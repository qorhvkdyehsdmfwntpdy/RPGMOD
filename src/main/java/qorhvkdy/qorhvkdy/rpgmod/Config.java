package qorhvkdy.qorhvkdy.rpgmod;

/*
 * [RPGMOD 파일 설명]
 * 역할: 모드 전역 설정값(기본 스탯, 캡, 밸런스 기본치)을 제공하는 설정 클래스입니다.
 * 수정 예시: STR 기본 시작값을 바꾸려면 Config의 기본 스탯 설정에서 STR 값을 수정합니다.
 */


import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatType;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = Rpgmod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER.comment("Whether to log the dirt block on common setup").define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER.comment("A magic number").defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue DEFAULT_STR = BUILDER.comment("Default STR for new player stats").defineInRange("defaultStr", 0, 0, 9999);
    private static final ForgeConfigSpec.IntValue DEFAULT_AGI = BUILDER.comment("Default AGI for new player stats").defineInRange("defaultAgi", 0, 0, 9999);
    private static final ForgeConfigSpec.IntValue DEFAULT_WIS = BUILDER.comment("Default WIS for new player stats").defineInRange("defaultWis", 0, 0, 9999);
    private static final ForgeConfigSpec.IntValue DEFAULT_LUK = BUILDER.comment("Default LUK for new player stats").defineInRange("defaultLuk", 0, 0, 9999);
    private static final ForgeConfigSpec.IntValue LEVEL_UP_STAT_POINTS = BUILDER.comment("Stat points granted per level up").defineInRange("levelUpStatPoints", 4, 0, 9999);
    private static final ForgeConfigSpec.DoubleValue DAMAGE_ATTACK_POWER_SCALE = BUILDER.comment("Attack power contribution to final damage").defineInRange("damageAttackPowerScale", 0.15, 0.0, 100.0);
    private static final ForgeConfigSpec.DoubleValue DAMAGE_DEFENSE_SCALE = BUILDER.comment("Defense effectiveness in damage mitigation").defineInRange("damageDefenseScale", 0.06, 0.0, 100.0);
    private static final ForgeConfigSpec.DoubleValue DAMAGE_MIN_MULTIPLIER = BUILDER.comment("Minimum post-mitigation multiplier").defineInRange("damageMinMultiplier", 0.10, 0.0, 1.0);

    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER.comment("What you want the introduction message to be for the magic number").define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER.comment("A list of items to log on common setup.").defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber = 42;
    public static int defaultStr = 0;
    public static int defaultAgi = 0;
    public static int defaultWis = 0;
    public static int defaultLuk = 0;
    public static int levelUpStatPoints = 4;
    public static double damageAttackPowerScale = 0.15;
    public static double damageDefenseScale = 0.06;
    public static double damageMinMultiplier = 0.10;
    public static String magicNumberIntroduction = "The magic number is... ";
    public static Set<Item> items = Set.of();

    public static int getDefaultStat(StatType type) {
        return switch (type) {
            case STR -> defaultStr;
            case AGI -> defaultAgi;
            case WIS -> defaultWis;
            case LUK -> defaultLuk;
        };
    }

    private static boolean validateItemName(final Object obj) {
        if (!(obj instanceof final String itemName)) {
            return false;
        }
        Identifier id = Identifier.tryParse(itemName);
        return id != null && ForgeRegistries.ITEMS.containsKey(id);
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        defaultStr = DEFAULT_STR.get();
        defaultAgi = DEFAULT_AGI.get();
        defaultWis = DEFAULT_WIS.get();
        defaultLuk = DEFAULT_LUK.get();
        levelUpStatPoints = LEVEL_UP_STAT_POINTS.get();
        damageAttackPowerScale = DAMAGE_ATTACK_POWER_SCALE.get();
        damageDefenseScale = DAMAGE_DEFENSE_SCALE.get();
        damageMinMultiplier = DAMAGE_MIN_MULTIPLIER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream()
                .map(Identifier::tryParse)
                .filter(Objects::nonNull)
                .map(ForgeRegistries.ITEMS::getValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}

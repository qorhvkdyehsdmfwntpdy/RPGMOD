package qorhvkdy.qorhvkdy.rpgmod;

/*
 * [RPGMOD 파일 설명]
 * 역할: 모드 엔트리 포인트로 블록/아이템/이벤트 등록 초기화를 시작합니다.
 * 수정 예시: 새 시스템 초기화가 필요하면 모드 생성자에 초기화 호출을 추가합니다.
 */


import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import qorhvkdy.qorhvkdy.rpgmod.audit.RpgAuditLogService;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassAdvancementRegistry;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassPassiveEffectService;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassLifecycleHooks;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassPassiveService;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassPassiveTemplateService;
import qorhvkdy.qorhvkdy.rpgmod.classes.resource.ClassResourceLifecycleHook;
import qorhvkdy.qorhvkdy.rpgmod.classes.resource.ClassResourceService;
import qorhvkdy.qorhvkdy.rpgmod.classes.seteffect.ClassSetEffectService;
import qorhvkdy.qorhvkdy.rpgmod.classes.skill.ClassSkillService;
import qorhvkdy.qorhvkdy.rpgmod.core.module.RpgModuleRegistry;
import qorhvkdy.qorhvkdy.rpgmod.debug.RpgDebugSettings;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;
import qorhvkdy.qorhvkdy.rpgmod.passive.StatPassiveSkillService;
import qorhvkdy.qorhvkdy.rpgmod.party.PartyService;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionContextRuleService;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencyBlockRuleService;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.data.ProficiencyBalanceRepository;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.data.ProficiencyRewardRepository;
import qorhvkdy.qorhvkdy.rpgmod.stats.data.StatsBalanceRepository;
import qorhvkdy.qorhvkdy.rpgmod.weapon.WeaponDataService;
import qorhvkdy.qorhvkdy.rpgmod.weapon.WeaponDropService;

@Mod(Rpgmod.MODID)
public class Rpgmod {
    public static final String MODID = "rpgmod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register(
            "example_block",
            () -> new Block(BlockBehaviour.Properties.of().setId(BLOCKS.key("example_block")).mapColor(MapColor.STONE))
    );
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register(
            "example_block",
            () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties().setId(ITEMS.key("example_block")))
    );
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register(
            "example_item",
            () -> new Item(
                    new Item.Properties()
                            .setId(ITEMS.key("example_item"))
                            .food(new FoodProperties.Builder().alwaysEdible().nutrition(1).saturationModifier(2f).build())
            )
    );
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register(
            "example_tab",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(EXAMPLE_ITEM.get()))
                    .build()
    );

    public Rpgmod(FMLJavaModLoadingContext context) {
        BusGroup modBusGroup = context.getModBusGroup();
        StatsBalanceRepository.bootstrap();
        StatPassiveSkillService.bootstrap();
        RpgModuleRegistry.bootstrap();
        RpgDebugSettings.bootstrap();
        RpgAuditLogService.bootstrap();
        PartyService.bootstrap();
        ClassLifecycleHooks.register(ClassPassiveService.INSTANCE);
        ClassLifecycleHooks.register(ClassResourceLifecycleHook.INSTANCE);
        ModNetwork.register();
        IModBusEvent.getBus(modBusGroup, FMLCommonSetupEvent.class).addListener(this::commonSetup);
        BLOCKS.register(modBusGroup);
        ITEMS.register(modBusGroup);
        CREATIVE_MODE_TABS.register(modBusGroup);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        if (Config.logDirtBlock) {
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        }
        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
        Config.items.forEach(item -> LOGGER.info("ITEM >> {}", item));
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}

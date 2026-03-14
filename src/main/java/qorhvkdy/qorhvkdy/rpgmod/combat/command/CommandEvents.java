package qorhvkdy.qorhvkdy.rpgmod.combat.command;

/*
 * [RPGMOD 파일 설명]
 * 역할: Forge 이벤트 버스에서 명령어 등록 이벤트를 수신합니다.
 * 수정 예시: 새 명령 클래스를 만들면 onRegisterCommands에서 register를 호출합니다.
 */


import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import qorhvkdy.qorhvkdy.rpgmod.Rpgmod;

@Mod.EventBusSubscriber(modid = Rpgmod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommandEvents {
    private CommandEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        StatsCommand.register(event.getDispatcher());
        ClassCommand.register(event.getDispatcher());
        ClassEnhancementCommand.register(event.getDispatcher());
        ProficiencyCommand.register(event.getDispatcher());
        PartyCommand.register(event.getDispatcher());
        PermissionCommand.register(event.getDispatcher());
        RpgDebugCommand.register(event.getDispatcher());
        RpgBalanceCommand.register(event.getDispatcher());
        RpgDevCommand.register(event.getDispatcher());
        QuestCommand.register(event.getDispatcher());
        NpcCommand.register(event.getDispatcher());
        SpellCommand.register(event.getDispatcher());
        RpgSkillCommand.register(event.getDispatcher());
        RpgMotionCommand.register(event.getDispatcher());
        RpgChainCommand.register(event.getDispatcher());
        RpgBossCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(Rpgmod.EXAMPLE_BLOCK_ITEM);
        }
    }
}

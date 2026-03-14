package qorhvkdy.qorhvkdy.rpgmod.core.module;

import qorhvkdy.qorhvkdy.rpgmod.classes.ClassAdvancementRegistry;
import qorhvkdy.qorhvkdy.rpgmod.bosschain.BossChainService;
import qorhvkdy.qorhvkdy.rpgmod.classes.balance.ClassBalanceCoefficientService;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassPassiveEffectService;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassPassiveTemplateService;
import qorhvkdy.qorhvkdy.rpgmod.classes.resource.ClassResourceService;
import qorhvkdy.qorhvkdy.rpgmod.classes.seteffect.ClassSetEffectService;
import qorhvkdy.qorhvkdy.rpgmod.classes.skill.ClassSkillService;
import qorhvkdy.qorhvkdy.rpgmod.classes.synergy.ClassPartySynergyService;
import qorhvkdy.qorhvkdy.rpgmod.combat.formula.CombatFormulaService;
import qorhvkdy.qorhvkdy.rpgmod.combat.profile.CombatProfileService;
import qorhvkdy.qorhvkdy.rpgmod.core.runtime.RuntimeFeatureService;
import qorhvkdy.qorhvkdy.rpgmod.npc.NpcDialogueService;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionContextRuleService;
import qorhvkdy.qorhvkdy.rpgmod.permission.PermissionContextPriorityService;
import qorhvkdy.qorhvkdy.rpgmod.permission.RpgPermissionService;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencyBlockRuleService;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.ProficiencySourceService;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.data.ProficiencyBalanceRepository;
import qorhvkdy.qorhvkdy.rpgmod.proficiency.data.ProficiencyRewardRepository;
import qorhvkdy.qorhvkdy.rpgmod.progression.ProgressionFormulaService;
import qorhvkdy.qorhvkdy.rpgmod.quest.QuestService;
import qorhvkdy.qorhvkdy.rpgmod.quest.content.QuestContentLinkService;
import qorhvkdy.qorhvkdy.rpgmod.skill.SkillMechanicService;
import qorhvkdy.qorhvkdy.rpgmod.spell.SpellAffinityService;
import qorhvkdy.qorhvkdy.rpgmod.weapon.WeaponDataService;
import qorhvkdy.qorhvkdy.rpgmod.weapon.WeaponDropService;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 모듈 단위 상태 조회/리로드를 위한 중앙 레지스트리.
 */
public final class RpgModuleRegistry {
    private static final Map<String, ModuleRuntimeState> MODULES = new LinkedHashMap<>();

    private RpgModuleRegistry() {
    }

    public static synchronized void bootstrap() {
        RuntimeFeatureService.bootstrap();
        MODULES.clear();
        register(new PermissionCoreModule());
        register(new ProgressionCoreModule());
        register(new ItemizationCoreModule());
        register(new ContentCoreModule());
        for (ModuleRuntimeState state : MODULES.values()) {
            bootstrapOne(state);
        }
    }

    public static synchronized boolean reload(String moduleId) {
        RuntimeFeatureService.reload();
        ModuleRuntimeState state = MODULES.get(normalize(moduleId));
        if (state == null) {
            return false;
        }
        reloadOne(state);
        return true;
    }

    public static synchronized void reloadAll() {
        RuntimeFeatureService.reload();
        for (ModuleRuntimeState state : MODULES.values()) {
            reloadOne(state);
        }
    }

    public static synchronized Collection<RpgRuntimeModule> all() {
        return MODULES.values().stream().map(state -> state.module).toList();
    }

    public static synchronized Collection<ModuleStatusView> statuses() {
        return MODULES.values().stream()
                .map(state -> new ModuleStatusView(
                        state.module.id(),
                        state.module.displayName(),
                        state.enabled,
                        state.bootstrapped,
                        state.lastBootstrapMs,
                        state.lastReloadMs,
                        state.errorMessage
                ))
                .toList();
    }

    public static synchronized Optional<RpgRuntimeModule> find(String moduleId) {
        ModuleRuntimeState state = MODULES.get(normalize(moduleId));
        return Optional.ofNullable(state == null ? null : state.module);
    }

    private static void register(RpgRuntimeModule module) {
        MODULES.put(normalize(module.id()), new ModuleRuntimeState(module));
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase();
    }

    private static void bootstrapOne(ModuleRuntimeState state) {
        state.enabled = RuntimeFeatureService.moduleEnabled(state.module.id());
        if (!state.enabled) {
            state.bootstrapped = false;
            state.errorMessage = "disabled_by_runtime_features";
            return;
        }
        long begin = System.nanoTime();
        try {
            state.module.bootstrap();
            state.bootstrapped = true;
            state.errorMessage = "";
        } catch (RuntimeException e) {
            state.bootstrapped = false;
            state.errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            throw e;
        } finally {
            state.lastBootstrapMs = (System.nanoTime() - begin) / 1_000_000L;
        }
    }

    private static void reloadOne(ModuleRuntimeState state) {
        state.enabled = RuntimeFeatureService.moduleEnabled(state.module.id());
        if (!state.enabled) {
            state.bootstrapped = false;
            state.errorMessage = "disabled_by_runtime_features";
            return;
        }
        long begin = System.nanoTime();
        try {
            if (!state.bootstrapped) {
                state.module.bootstrap();
                state.bootstrapped = true;
            } else {
                state.module.reload();
            }
            state.errorMessage = "";
        } catch (RuntimeException e) {
            state.errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            throw e;
        } finally {
            state.lastReloadMs = (System.nanoTime() - begin) / 1_000_000L;
        }
    }

    private static final class ModuleRuntimeState {
        private final RpgRuntimeModule module;
        private boolean enabled;
        private boolean bootstrapped;
        private long lastBootstrapMs;
        private long lastReloadMs;
        private String errorMessage = "";

        private ModuleRuntimeState(RpgRuntimeModule module) {
            this.module = module;
        }
    }

    public record ModuleStatusView(
            String id,
            String displayName,
            boolean enabled,
            boolean bootstrapped,
            long lastBootstrapMs,
            long lastReloadMs,
            String errorMessage
    ) {
    }

    private static final class PermissionCoreModule implements RpgRuntimeModule {
        @Override
        public String id() {
            return "permission-core";
        }

        @Override
        public String displayName() {
            return "Permission Core";
        }

        @Override
        public void bootstrap() {
            PermissionContextRuleService.bootstrap();
            PermissionContextPriorityService.bootstrap();
            RpgPermissionService.bootstrap();
        }

        @Override
        public void reload() {
            PermissionContextRuleService.reload();
            PermissionContextPriorityService.reload();
            RpgPermissionService.reload();
        }
    }

    private static final class ProgressionCoreModule implements RpgRuntimeModule {
        @Override
        public String id() {
            return "progression-core";
        }

        @Override
        public String displayName() {
            return "Progression Core";
        }

        @Override
        public void bootstrap() {
            BossChainService.bootstrap();
            ProficiencyBalanceRepository.bootstrap();
            ProficiencyRewardRepository.bootstrap();
            ProficiencySourceService.bootstrap();
            ProficiencyBlockRuleService.bootstrap();
            ClassAdvancementRegistry.bootstrap();
            ClassPassiveTemplateService.bootstrap();
            ClassPassiveEffectService.bootstrap();
            ClassBalanceCoefficientService.bootstrap();
            ClassPartySynergyService.bootstrap();
            ClassResourceService.bootstrap();
            ClassSkillService.bootstrap();
            ClassSetEffectService.bootstrap();
            ProgressionFormulaService.bootstrap();
        }

        @Override
        public void reload() {
            BossChainService.reload();
            ProficiencyBalanceRepository.reload();
            ProficiencyRewardRepository.reload();
            ProficiencySourceService.reload();
            ProficiencyBlockRuleService.reload();
            ClassAdvancementRegistry.reload();
            ClassPassiveTemplateService.reload();
            ClassPassiveEffectService.reload();
            ClassBalanceCoefficientService.reload();
            ClassPartySynergyService.reload();
            ClassResourceService.reload();
            ClassSkillService.reload();
            ClassSetEffectService.reload();
            ProgressionFormulaService.reload();
        }
    }

    private static final class ItemizationCoreModule implements RpgRuntimeModule {
        @Override
        public String id() {
            return "itemization-core";
        }

        @Override
        public String displayName() {
            return "Itemization Core";
        }

        @Override
        public void bootstrap() {
            WeaponDataService.bootstrap();
            WeaponDropService.bootstrap();
            CombatProfileService.bootstrap();
            CombatFormulaService.bootstrap();
        }

        @Override
        public void reload() {
            WeaponDataService.reload();
            WeaponDropService.reload();
            CombatProfileService.reload();
            CombatFormulaService.reload();
        }
    }

    private static final class ContentCoreModule implements RpgRuntimeModule {
        @Override
        public String id() {
            return "content-core";
        }

        @Override
        public String displayName() {
            return "Content Core";
        }

        @Override
        public void bootstrap() {
            QuestService.bootstrap();
            QuestContentLinkService.bootstrap();
            SkillMechanicService.bootstrap();
            NpcDialogueService.bootstrap();
            SpellAffinityService.bootstrap();
        }

        @Override
        public void reload() {
            QuestService.reload();
            QuestContentLinkService.reload();
            SkillMechanicService.reload();
            NpcDialogueService.reload();
            SpellAffinityService.reload();
        }
    }
}

package qorhvkdy.qorhvkdy.rpgmod.core.doctor;

import qorhvkdy.qorhvkdy.rpgmod.classes.ClassAdvancementRegistry;
import qorhvkdy.qorhvkdy.rpgmod.core.module.RpgModuleRegistry;
import qorhvkdy.qorhvkdy.rpgmod.permission.data.PermissionRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 운영 점검 서비스.
 * 빠른 1차 진단으로 설정 누락/모듈 비활성/핵심 데이터 이상 여부를 확인한다.
 */
public final class RpgDoctorService {
    private RpgDoctorService() {
    }

    public static DoctorReport runQuickChecks() {
        List<String> infos = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (RpgModuleRegistry.ModuleStatusView status : RpgModuleRegistry.statuses()) {
            if (!status.enabled()) {
                warnings.add("module disabled: " + status.id());
                continue;
            }
            if (!status.bootstrapped()) {
                errors.add("module not bootstrapped: " + status.id() + " (" + status.errorMessage() + ")");
                continue;
            }
            infos.add("module ok: " + status.id()
                    + " (boot=" + status.lastBootstrapMs() + "ms, reload=" + status.lastReloadMs() + "ms)");
        }

        checkConfig("permissions.json", errors);
        checkConfig("permission-context-priority.json", errors);
        checkConfig("class-advancements.json", errors);
        checkConfig("weapons.json", errors);
        checkConfig("proficiency-sources.json", errors);
        checkConfig("quests.json", errors);
        checkConfig("quest-links.json", errors);
        checkConfig("boss-chains.json", errors);
        checkConfig("boss-chain-progress.json", errors);
        checkConfig("quest-zones.json", errors);
        checkConfig("skill-runtime.json", errors);
        checkConfig("skill-motions.json", errors);
        checkConfig("passive-skills.json", errors);
        checkConfig("skill-tree.json", errors);
        checkConfig("skill-chains.json", errors);
        checkConfig("npc-dialogues.json", errors);
        checkConfig("spell-schools.json", errors);
        checkConfig("combat-profiles.json", errors);
        checkConfig("combat-formulas.json", errors);
        checkConfig("progression-formulas.json", errors);

        if (!PermissionRepository.get().groups.containsKey("player")) {
            errors.add("permissions.json missing group: player");
        }
        if (!PermissionRepository.get().groups.containsKey("admin")) {
            warnings.add("permissions.json missing group: admin");
        }

        List<String> advancementIds = ClassAdvancementRegistry.ids();
        if (advancementIds.isEmpty()) {
            errors.add("class advancements are empty");
        } else {
            infos.add("class advancements loaded: " + advancementIds.size());
        }

        return new DoctorReport(infos, warnings, errors);
    }

    private static void checkConfig(String fileName, List<String> errors) {
        Path path = Path.of("config", "rpgmod", fileName);
        if (!Files.exists(path)) {
            errors.add("missing config: " + path);
        }
    }

    public record DoctorReport(List<String> infos, List<String> warnings, List<String> errors) {
        public boolean healthy() {
            return errors.isEmpty();
        }
    }
}

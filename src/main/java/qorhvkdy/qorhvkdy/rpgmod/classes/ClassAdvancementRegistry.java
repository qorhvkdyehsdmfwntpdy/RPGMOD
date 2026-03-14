package qorhvkdy.qorhvkdy.rpgmod.classes;

import qorhvkdy.qorhvkdy.rpgmod.classes.data.ClassAdvancementJson;
import qorhvkdy.qorhvkdy.rpgmod.classes.data.ClassAdvancementRepository;
import qorhvkdy.qorhvkdy.rpgmod.stats.StatType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Runtime registry for class advancements loaded from config.
 */
public final class ClassAdvancementRegistry {
    private static volatile Map<String, ClassAdvancement> advancements = Map.of();

    private ClassAdvancementRegistry() {
    }

    public static void bootstrap() {
        ClassAdvancementRepository.bootstrap();
        reload();
    }

    public static synchronized void reload() {
        ClassAdvancementRepository.reload();
        ClassAdvancementJson source = ClassAdvancementRepository.get();
        Map<String, ClassAdvancement> next = new LinkedHashMap<>();
        for (ClassAdvancementJson.Entry entry : source.advancements) {
            ClassAdvancement node = toNode(entry);
            next.put(node.id(), node);
        }
        advancements = Map.copyOf(next);
    }

    public static Optional<ClassAdvancement> get(String id) {
        return Optional.ofNullable(advancements.get(ClassAdvancement.normalizeId(id)));
    }

    public static ClassAdvancement defaultBaseAdvancement(PlayerClassType baseClass) {
        Optional<ClassAdvancement> direct = advancements.values().stream()
                .filter(node -> node.baseClass() == baseClass && node.tier() == JobTier.BASE)
                .findFirst();
        if (direct.isPresent()) {
            return direct.get();
        }

        if (baseClass != PlayerClassType.NONE) {
            Optional<ClassAdvancement> novice = advancements.values().stream()
                    .filter(node -> node.baseClass() == PlayerClassType.NONE && node.tier() == JobTier.BASE)
                    .findFirst();
            if (novice.isPresent()) {
                return novice.get();
            }
        }
        return noneAdvancement();
    }

    public static List<ClassAdvancement> nextOptions(String currentAdvancementId, int level) {
        String normalized = ClassAdvancement.normalizeId(currentAdvancementId);
        return advancements.values().stream()
                .filter(node -> Objects.equals(node.parentId(), normalized))
                .filter(node -> level >= node.requiredLevel())
                .toList();
    }

    public static List<ClassAdvancement> lockedNextOptions(String currentAdvancementId, int level) {
        String normalized = ClassAdvancement.normalizeId(currentAdvancementId);
        return advancements.values().stream()
                .filter(node -> Objects.equals(node.parentId(), normalized))
                .filter(node -> level < node.requiredLevel())
                .toList();
    }

    public static boolean canPromote(String currentAdvancementId, String targetAdvancementId, int level) {
        Optional<ClassAdvancement> current = get(currentAdvancementId);
        Optional<ClassAdvancement> target = get(targetAdvancementId);
        if (current.isEmpty() || target.isEmpty()) {
            return false;
        }

        ClassAdvancement currentNode = current.get();
        ClassAdvancement targetNode = target.get();
        boolean firstPromotionFromNovice =
                currentNode.baseClass() == PlayerClassType.NONE
                        && targetNode.tier() == JobTier.FIRST;
        if (!firstPromotionFromNovice && currentNode.baseClass() != targetNode.baseClass()) {
            return false;
        }
        if (!Objects.equals(targetNode.parentId(), currentNode.id())) {
            return false;
        }
        return level >= targetNode.requiredLevel();
    }

    public static List<String> ids() {
        return new ArrayList<>(advancements.keySet());
    }

    private static ClassAdvancement toNode(ClassAdvancementJson.Entry entry) {
        PlayerClassType baseClass = PlayerClassType.fromId(entry.baseClass).orElse(PlayerClassType.NONE);
        JobTier tier = parseTier(entry.tier);

        EnumMap<StatType, Double> weights = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) {
            weights.put(type, Math.max(0.0, entry.statWeights.getOrDefault(type.key(), 0.0)));
        }

        List<RequirementSpec> requirements = new ArrayList<>();
        if (entry.requirements != null) {
            for (ClassAdvancementJson.RequirementEntry requirement : entry.requirements) {
                RequirementType type = RequirementType.fromId(requirement.type).orElse(RequirementType.LEVEL);
                requirements.add(new RequirementSpec(type, requirement.value, requirement.minValue));
            }
        }
        if (requirements.isEmpty()) {
            requirements.add(RequirementSpec.level(entry.requiredLevel));
        }

        return new ClassAdvancement(
                entry.id,
                entry.displayName,
                baseClass,
                tier,
                entry.parentId,
                entry.requiredLevel,
                entry.summary,
                weights,
                entry.weaponHints,
                requirements
        );
    }

    private static JobTier parseTier(String raw) {
        if (raw == null || raw.isBlank()) {
            return JobTier.BASE;
        }
        try {
            return JobTier.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return JobTier.BASE;
        }
    }

    private static ClassAdvancement noneAdvancement() {
        return new ClassAdvancement(
                "none",
                "None",
                PlayerClassType.NONE,
                JobTier.BASE,
                null,
                0,
                "No advancement selected.",
                Map.of(),
                List.of("Any"),
                List.of(RequirementSpec.level(0))
        );
    }
}

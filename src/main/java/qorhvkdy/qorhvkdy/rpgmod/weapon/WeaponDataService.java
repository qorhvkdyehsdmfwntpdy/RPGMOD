package qorhvkdy.qorhvkdy.rpgmod.weapon;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import qorhvkdy.qorhvkdy.rpgmod.weapon.data.WeaponDataJson;
import qorhvkdy.qorhvkdy.rpgmod.weapon.data.WeaponDataRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 무기 데이터 조회 서비스.
 * tooltip/UI/드랍테이블 검증이 동일 데이터를 보도록 중앙화한다.
 */
public final class WeaponDataService {
    private static volatile Map<String, WeaponDescriptor> descriptors = Map.of();
    private static volatile List<WeaponDescriptor> dropCandidates = List.of();

    private WeaponDataService() {
    }

    public static void bootstrap() {
        WeaponDataRepository.bootstrap();
        reload();
    }

    public static synchronized void reload() {
        WeaponDataRepository.reload();
        WeaponDataJson json = WeaponDataRepository.get();
        Map<String, WeaponDescriptor> next = new LinkedHashMap<>();
        List<WeaponDescriptor> drops = new java.util.ArrayList<>();
        for (WeaponDataJson.Entry entry : json.weapons) {
            String key = normalize(entry.itemId);
            if (key.isBlank()) {
                continue;
            }
            List<WeaponOptionDescriptor> options = entry.options.stream()
                    .map(option -> new WeaponOptionDescriptor(
                            normalize(option.mode),
                            normalize(option.stat),
                            option.value,
                            option.label == null ? "" : option.label
                    ))
                    .toList();
            List<WeaponAffixDescriptor> affixPool = entry.affixPool.stream()
                    .map(affix -> new WeaponAffixDescriptor(
                            normalize(affix.type),
                            normalize(affix.id),
                            affix.display == null ? "" : affix.display.trim(),
                            Math.max(0.1, affix.optionMultiplier)
                    ))
                    .filter(affix -> !affix.id().isBlank() || !affix.display().isBlank())
                    .toList();
            WeaponDataJson.SocketEntry socketEntry = entry.socket == null ? new WeaponDataJson.SocketEntry() : entry.socket;
            int minSockets = Math.max(0, socketEntry.min);
            int maxSockets = Math.max(minSockets, socketEntry.max);
            WeaponSocketDescriptor socket = new WeaponSocketDescriptor(
                    minSockets,
                    maxSockets,
                    List.copyOf(socketEntry.allowed == null ? List.of() : socketEntry.allowed.stream()
                            .map(String::trim)
                            .filter(value -> !value.isBlank())
                            .toList())
            );
            WeaponDescriptor descriptor = new WeaponDescriptor(
                    key,
                    entry.displayName,
                    entry.category,
                    entry.grade,
                    entry.rarity,
                    normalize(entry.requiredClass),
                    normalize(entry.requiredAdvancement),
                    options,
                    affixPool,
                    socket,
                    List.copyOf(entry.obtainMethods),
                    entry.dropEnabled,
                    Math.max(0.0, entry.baseDropChance),
                    Math.max(0.0, entry.dropWeight),
                    Math.max(0.0, entry.minMobMaxHealth)
            );
            next.put(key, descriptor);
            if (descriptor.dropEnabled()) {
                drops.add(descriptor);
            }
        }
        descriptors = Map.copyOf(next);
        dropCandidates = List.copyOf(drops);
    }

    public static Optional<WeaponDescriptor> find(ItemStack stack) {
        Identifier id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(descriptors.get(normalize(id.toString())));
    }

    public static List<WeaponDescriptor> dropCandidates() {
        return dropCandidates;
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase();
    }
}

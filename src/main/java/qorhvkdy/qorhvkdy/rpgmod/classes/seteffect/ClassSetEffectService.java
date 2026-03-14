package qorhvkdy.qorhvkdy.rpgmod.classes.seteffect;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import qorhvkdy.qorhvkdy.rpgmod.classes.seteffect.data.ClassSetEffectJson;
import qorhvkdy.qorhvkdy.rpgmod.classes.seteffect.data.ClassSetEffectRepository;
import qorhvkdy.qorhvkdy.rpgmod.passive.PassiveBonus;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;
import qorhvkdy.qorhvkdy.rpgmod.weapon.WeaponDataService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 클래스 장비 세트 효과 서비스.
 */
public final class ClassSetEffectService {
    private static volatile List<ClassSetEffectDescriptor> descriptors = List.of();

    private ClassSetEffectService() {
    }

    public static void bootstrap() {
        ClassSetEffectRepository.bootstrap();
        reload();
    }

    public static synchronized void reload() {
        ClassSetEffectRepository.reload();
        ClassSetEffectJson json = ClassSetEffectRepository.get();
        ArrayList<ClassSetEffectDescriptor> next = new ArrayList<>();
        for (ClassSetEffectJson.Entry entry : json.sets) {
            if (entry == null) {
                continue;
            }
            String id = normalize(entry.id);
            if (id.isBlank()) {
                continue;
            }
            next.add(new ClassSetEffectDescriptor(
                    id,
                    normalize(entry.category),
                    normalize(entry.requiredClass),
                    normalize(entry.requiredAdvancement),
                    Math.max(1, entry.requiredPieces),
                    normalize(entry.requiredPassiveId),
                    new PassiveBonus(
                            1.0,
                            Math.max(1.0, entry.attackMultiplier),
                            0.0,
                            Math.max(0.0, entry.armorBonus),
                            Math.max(0.0, entry.critChanceBonus),
                            Math.max(0.0, entry.critDamageBonus)
                    )
            ));
        }
        descriptors = List.copyOf(next);
    }

    public static PassiveBonus compute(Player player, PlayerStats stats) {
        List<ClassSetEffectDescriptor> active = activeDescriptors(player, stats);
        PassiveBonus total = PassiveBonus.none();
        for (ClassSetEffectDescriptor descriptor : active) {
            total = total.combine(descriptor.bonus());
        }
        return total;
    }

    /**
     * 현재 장비/직업 상태에서 실제로 활성화된 세트효과 목록을 반환한다.
     */
    public static List<ClassSetEffectDescriptor> activeDescriptors(Player player, PlayerStats stats) {
        Map<String, Integer> piecesByCategory = collectPieces(player);
        ArrayList<ClassSetEffectDescriptor> active = new ArrayList<>();
        for (ClassSetEffectDescriptor descriptor : descriptors) {
            if (!descriptor.requiredClass().isBlank() && !normalize(stats.getSelectedClass().id()).equals(descriptor.requiredClass())) {
                continue;
            }
            if (!descriptor.requiredAdvancement().isBlank() && !normalize(stats.getCurrentAdvancementId()).equals(descriptor.requiredAdvancement())) {
                continue;
            }
            if (!descriptor.requiredPassiveId().isBlank() && !stats.getPassiveSlots().containsValue(descriptor.requiredPassiveId())) {
                continue;
            }
            int pieces = piecesByCategory.getOrDefault(descriptor.category(), 0);
            if (pieces < descriptor.requiredPieces()) {
                continue;
            }
            active.add(descriptor);
        }
        return List.copyOf(active);
    }

    private static Map<String, Integer> collectPieces(Player player) {
        HashMap<String, Integer> counts = new HashMap<>();
        for (ItemStack stack : allEquipment(player)) {
            WeaponDataService.find(stack).ifPresent(descriptor -> {
                String category = normalize(descriptor.category());
                if (!category.isBlank()) {
                    counts.merge(category, 1, Integer::sum);
                }
            });
        }
        return counts;
    }

    private static List<ItemStack> allEquipment(Player player) {
        ArrayList<ItemStack> all = new ArrayList<>();
        all.add(player.getMainHandItem());
        all.add(player.getOffhandItem());
        all.add(player.getItemBySlot(EquipmentSlot.HEAD));
        all.add(player.getItemBySlot(EquipmentSlot.CHEST));
        all.add(player.getItemBySlot(EquipmentSlot.LEGS));
        all.add(player.getItemBySlot(EquipmentSlot.FEET));
        return all;
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}

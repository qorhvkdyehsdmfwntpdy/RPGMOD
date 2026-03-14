package qorhvkdy.qorhvkdy.rpgmod.classes;

import net.minecraft.world.item.ItemStack;
import qorhvkdy.qorhvkdy.rpgmod.weapon.WeaponDescriptor;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Weapon compatibility hook for class/advancement.
 */
public final class ClassWeaponPolicyService {
    private ClassWeaponPolicyService() {
    }

    public static Set<String> allowedWeaponFamilies(PlayerStats stats) {
        Set<String> families = new HashSet<>();
        families.addAll(stats.getClassProfile().recommendedWeaponFamilies().stream().map(v -> normalize(v)).toList());
        families.addAll(stats.getCurrentAdvancement().weaponHints().stream().map(ClassWeaponPolicyService::normalize).toList());
        return families;
    }

    public static boolean canUseFamily(PlayerStats stats, String family) {
        String normalized = normalize(family);
        if (normalized.isBlank() || "any".equals(normalized)) {
            return true;
        }
        Set<String> allowed = allowedWeaponFamilies(stats);
        if (allowed.contains("any")) {
            return true;
        }
        return allowed.contains(normalized);
    }

    public static boolean canUseItem(PlayerStats stats, ItemStack stack, WeaponFamilyResolver resolver) {
        List<String> families = resolver.resolveFamilies(stack).stream().toList();
        if (families.isEmpty()) {
            return true;
        }
        for (String family : families) {
            if (canUseFamily(stats, family)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canUseDescriptor(PlayerStats stats, WeaponDescriptor descriptor) {
        if (descriptor == null) {
            return true;
        }
        String requiredClass = normalize(descriptor.requiredClass());
        if (!requiredClass.isBlank() && !requiredClass.equals("none") && !requiredClass.equals(stats.getSelectedClass().id())) {
            return false;
        }
        String requiredAdv = normalize(descriptor.requiredAdvancement());
        if (!requiredAdv.isBlank() && !requiredAdv.equals("none") && !requiredAdv.equals(stats.getCurrentAdvancementId())) {
            return false;
        }
        return true;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
    }
}

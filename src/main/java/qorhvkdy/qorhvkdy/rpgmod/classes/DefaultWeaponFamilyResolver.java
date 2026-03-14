package qorhvkdy.qorhvkdy.rpgmod.classes;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

/**
 * Default name-based resolver.
 * Replace this with tag-based resolver when dedicated weapon tags are introduced.
 */
public final class DefaultWeaponFamilyResolver implements WeaponFamilyResolver {
    public static final DefaultWeaponFamilyResolver INSTANCE = new DefaultWeaponFamilyResolver();

    private DefaultWeaponFamilyResolver() {
    }

    @Override
    public Set<String> resolveFamilies(ItemStack stack) {
        Set<String> families = new HashSet<>();
        Identifier id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return families;
        }
        String path = id.getPath().toLowerCase();

        if (path.contains("sword")) families.add("sword");
        if (path.contains("axe")) families.add("axe");
        if (path.contains("bow")) families.add("bow");
        if (path.contains("crossbow")) families.add("crossbow");
        if (path.contains("staff")) families.add("staff");
        if (path.contains("wand")) families.add("wand");
        if (path.contains("dagger")) families.add("dagger");
        if (path.contains("spear")) families.add("spear");
        if (path.contains("throw")) families.add("throwing");

        return families;
    }
}


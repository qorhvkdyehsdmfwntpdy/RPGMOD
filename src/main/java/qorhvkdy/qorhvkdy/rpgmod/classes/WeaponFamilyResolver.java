package qorhvkdy.qorhvkdy.rpgmod.classes;

import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Resolves weapon family tags from an item.
 */
public interface WeaponFamilyResolver {
    Set<String> resolveFamilies(ItemStack stack);
}


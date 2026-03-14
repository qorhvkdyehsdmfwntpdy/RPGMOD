package qorhvkdy.qorhvkdy.rpgmod.classes;

import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lightweight lifecycle hook manager.
 */
public final class ClassLifecycleHooks {
    private static final List<ClassLifecycleHook> HOOKS = new CopyOnWriteArrayList<>();

    private ClassLifecycleHooks() {
    }

    public static void register(ClassLifecycleHook hook) {
        if (hook != null) {
            HOOKS.add(hook);
        }
    }

    public static void fireClassChanged(ServerPlayer player, PlayerStats stats, PlayerClassType before, PlayerClassType after, ClassAdvancement beforeAdv, ClassAdvancement afterAdv) {
        for (ClassLifecycleHook hook : HOOKS) {
            hook.onClassChanged(player, stats, before, after, beforeAdv, afterAdv);
        }
    }

    public static void firePromotion(ServerPlayer player, PlayerStats stats, ClassAdvancement before, ClassAdvancement after) {
        for (ClassLifecycleHook hook : HOOKS) {
            hook.onPromotion(player, stats, before, after);
        }
    }
}


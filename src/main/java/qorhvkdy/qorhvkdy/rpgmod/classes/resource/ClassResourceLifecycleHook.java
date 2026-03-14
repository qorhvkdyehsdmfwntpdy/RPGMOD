package qorhvkdy.qorhvkdy.rpgmod.classes.resource;

import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassAdvancement;
import qorhvkdy.qorhvkdy.rpgmod.classes.ClassLifecycleHook;
import qorhvkdy.qorhvkdy.rpgmod.classes.PlayerClassType;
import qorhvkdy.qorhvkdy.rpgmod.stats.PlayerStats;

/**
 * 클래스 변경/전직 시 리소스 프로필을 동기화한다.
 */
public final class ClassResourceLifecycleHook implements ClassLifecycleHook {
    public static final ClassResourceLifecycleHook INSTANCE = new ClassResourceLifecycleHook();

    private ClassResourceLifecycleHook() {
    }

    @Override
    public void onClassChanged(ServerPlayer player, PlayerStats stats, PlayerClassType before, PlayerClassType after, ClassAdvancement beforeAdv, ClassAdvancement afterAdv) {
        ClassResourceService.updateProfile(player, stats, true);
    }

    @Override
    public void onPromotion(ServerPlayer player, PlayerStats stats, ClassAdvancement before, ClassAdvancement after) {
        ClassResourceService.updateProfile(player, stats, false);
    }
}

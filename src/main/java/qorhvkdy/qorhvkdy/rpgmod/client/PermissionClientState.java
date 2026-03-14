package qorhvkdy.qorhvkdy.rpgmod.client;

/**
 * 클라이언트 권한 스냅샷 캐시.
 * GUI에서 버튼 노출 제어 시 사용한다.
 */
public final class PermissionClientState {
    private static volatile Snapshot snapshot = Snapshot.defaultState();

    private PermissionClientState() {
    }

    public static Snapshot get() {
        return snapshot;
    }

    public static void apply(Snapshot next) {
        snapshot = next == null ? Snapshot.defaultState() : next;
    }

    public record Snapshot(
            String groupId,
            String prefix,
            int weight,
            boolean permissionAdmin,
            boolean classAdmin,
            boolean statsAdmin,
            boolean proficiencyAdmin,
            boolean debugAdmin,
            boolean permUiOpen,
            boolean skillTreeUiOpen,
            boolean partyManage,
            boolean partyForceKick,
            boolean guildManage,
            boolean titleManage
    ) {
        public static Snapshot defaultState() {
            return new Snapshot(
                    "player",
                    "",
                    0,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false
            );
        }
    }
}

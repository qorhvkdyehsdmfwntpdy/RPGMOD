package qorhvkdy.qorhvkdy.rpgmod.permission.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 경량 권한 설정 DTO.
 * LuckPerms의 핵심 개념(그룹/상속/유저 오버라이드)만 단순화해서 제공한다.
 */
public class PermissionConfigJson {
    public int dataVersion = 1;
    public Map<String, GroupJson> groups = new LinkedHashMap<>();
    public Map<String, String> users = new LinkedHashMap<>();
    public Map<String, List<String>> userNodes = new LinkedHashMap<>();
    public Map<String, List<TempNodeJson>> userTempNodes = new LinkedHashMap<>();
    public Map<String, Map<String, List<String>>> userContextNodes = new LinkedHashMap<>();
    public Map<String, Map<String, String>> userMeta = new LinkedHashMap<>();

    public PermissionConfigJson() {
        GroupJson player = new GroupJson();
        player.parent = "";
        player.weight = 0;
        player.prefix = "";
        player.meta = new LinkedHashMap<>();
        player.nodes = new ArrayList<>();
        player.nodes.add("rpg.ui.perm.open");
        player.nodes.add("rpg.ui.skill_tree.open");
        player.nodes.add("rpg.party.manage");
        player.contextNodes = new LinkedHashMap<>();
        groups.put("player", player);

        GroupJson moderator = new GroupJson();
        moderator.parent = "player";
        moderator.weight = 50;
        moderator.prefix = "[MOD]";
        moderator.meta = new LinkedHashMap<>();
        moderator.nodes = new ArrayList<>();
        moderator.nodes.add("rpg.stats.admin");
        moderator.nodes.add("rpg.proficiency.admin");
        moderator.nodes.add("rpg.debug.drop.simulate");
        moderator.nodes.add("rpg.balance.admin");
        moderator.nodes.add("rpg.title.manage");
        moderator.contextNodes = new LinkedHashMap<>();
        groups.put("moderator", moderator);

        GroupJson admin = new GroupJson();
        admin.parent = "moderator";
        admin.weight = 100;
        admin.prefix = "[ADMIN]";
        admin.meta = new LinkedHashMap<>();
        admin.nodes = new ArrayList<>();
        admin.nodes.add("rpg.*");
        admin.contextNodes = new LinkedHashMap<>();
        groups.put("admin", admin);
    }

    public static final class GroupJson {
        public String parent = "";
        public int weight = 0;
        public String prefix = "";
        public Map<String, String> meta = new LinkedHashMap<>();
        public List<String> nodes = new ArrayList<>();
        public Map<String, List<String>> contextNodes = new LinkedHashMap<>();
    }

    /**
     * 만료 시간이 있는 임시 권한 노드.
     * expiresAtEpochSec 는 UTC epoch seconds 기준으로 저장한다.
     */
    public static final class TempNodeJson {
        public String node = "";
        public long expiresAtEpochSec = 0L;
    }
}

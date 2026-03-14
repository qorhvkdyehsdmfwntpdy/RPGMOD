package qorhvkdy.qorhvkdy.rpgmod.permission.data;

import java.util.ArrayList;
import java.util.List;

/**
 * LuckPerms 스타일 컨텍스트 우선순위 설정.
 */
public class PermissionContextPriorityJson {
    public int dataVersion = 1;
    public List<String> order = new ArrayList<>();

    public PermissionContextPriorityJson() {
        order.add("module");
        order.add("region");
        order.add("dimension");
        order.add("time");
        order.add("world");
    }
}

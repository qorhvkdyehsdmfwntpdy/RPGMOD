package qorhvkdy.qorhvkdy.rpgmod.classes.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 클래스 패시브 템플릿 JSON DTO.
 * advancementId 단위로 슬롯 구성을 정의한다.
 */
public class ClassPassiveJson {
    public int dataVersion = 1;
    public List<Entry> templates = new ArrayList<>();

    public ClassPassiveJson() {
        Entry novice = new Entry();
        novice.advancementId = "novice_base";
        novice.slots.put("core", "novice_core");
        novice.slots.put("offense", "novice_offense");
        novice.slots.put("utility", "novice_utility");
        templates.add(novice);
    }

    public static class Entry {
        public String advancementId = "";
        public Map<String, String> slots = new LinkedHashMap<>();
    }
}


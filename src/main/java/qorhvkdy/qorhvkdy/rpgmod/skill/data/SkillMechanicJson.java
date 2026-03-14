package qorhvkdy.qorhvkdy.rpgmod.skill.data;

import java.util.ArrayList;
import java.util.List;

/**
 * MythicMobs 스타일 스킬 체인 JSON.
 * Trigger/Condition/Action 분리로 스킬 확장을 코드 수정 없이 진행한다.
 */
public class SkillMechanicJson {
    public int dataVersion = 1;
    public List<SkillEntry> skills = new ArrayList<>();

    public SkillMechanicJson() {
        SkillEntry sample = new SkillEntry();
        sample.id = "warrior.training_roar";
        sample.trigger = "manual";
        sample.conditions.add(ConditionEntry.level(10));
        sample.conditions.add(ConditionEntry.baseClass("warrior"));
        sample.actions.add(ActionEntry.message("전사의 포효가 발동했습니다."));
        sample.actions.add(ActionEntry.grantProficiency("weapon", 8));
        skills.add(sample);
    }

    public static class SkillEntry {
        public String id = "";
        public String trigger = "manual";
        public int cooldownMs = 0;
        public List<ConditionEntry> conditions = new ArrayList<>();
        public List<ActionEntry> actions = new ArrayList<>();
    }

    public static class ConditionEntry {
        public String type = "LEVEL";
        public String key = "";
        public String value = "";
        public int min = 0;
        public double minDouble = 0.0;

        public static ConditionEntry level(int min) {
            ConditionEntry entry = new ConditionEntry();
            entry.type = "LEVEL";
            entry.min = min;
            return entry;
        }

        public static ConditionEntry baseClass(String classId) {
            ConditionEntry entry = new ConditionEntry();
            entry.type = "BASE_CLASS";
            entry.value = classId;
            return entry;
        }
    }

    public static class ActionEntry {
        public String type = "MESSAGE";
        public String key = "";
        public String value = "";
        public int amount = 0;
        public double amountDouble = 0.0;

        public static ActionEntry message(String text) {
            ActionEntry entry = new ActionEntry();
            entry.type = "MESSAGE";
            entry.value = text;
            return entry;
        }

        public static ActionEntry grantProficiency(String proficiencyType, int amount) {
            ActionEntry entry = new ActionEntry();
            entry.type = "GRANT_PROFICIENCY";
            entry.key = proficiencyType;
            entry.amount = amount;
            return entry;
        }
    }
}

package qorhvkdy.qorhvkdy.rpgmod.quest.data;

import java.util.ArrayList;
import java.util.List;

/**
 * BetonQuest/FTB Quests 스타일의 최소 DSL.
 * 조건/보상/상태를 데이터로 분리해 확장성을 확보한다.
 */
public class QuestJson {
    public int dataVersion = 1;
    public List<Entry> quests = new ArrayList<>();

    public QuestJson() {
        Entry sample = new Entry();
        sample.id = "starter_warrior_trial";
        sample.name = "quest.rpgmod.starter_warrior_trial";
        sample.repeatable = false;
        sample.repeatPolicy = "NONE";
        sample.repeatCooldownSec = 0;
        sample.prerequisites = List.of();
        sample.conditions.add(Condition.level(10));
        sample.conditions.add(Condition.baseClass("warrior"));
        sample.objectives.add(Objective.killMob("minecraft:zombie", 3));
        sample.objectives.add(Objective.talkNpc("trainer_warrior", 1));
        sample.rewards.commands.add("say %player% completed starter_warrior_trial");
        quests.add(sample);
    }

    public static class Entry {
        public String id = "";
        public String name = "";
        public boolean repeatable = false;
        public String repeatPolicy = "NONE";
        public int repeatCooldownSec = 0;
        public boolean shareWithParty = false;
        public boolean failOnDeath = false;
        public int retryCooldownSec = 0;
        public List<String> prerequisites = new ArrayList<>();
        public List<Condition> conditions = new ArrayList<>();
        public List<Objective> objectives = new ArrayList<>();
        public Reward rewards = new Reward();
    }

    public static class Condition {
        public String type = "LEVEL";
        public String key = "";
        public String value = "";
        public int min = 0;

        public static Condition level(int level) {
            Condition c = new Condition();
            c.type = "LEVEL";
            c.min = level;
            return c;
        }

        public static Condition baseClass(String classId) {
            Condition c = new Condition();
            c.type = "BASE_CLASS";
            c.value = classId;
            return c;
        }
    }

    public static class Reward {
        public int xpLevels = 0;
        public List<String> commands = new ArrayList<>();
        public List<ItemReward> guaranteedItems = new ArrayList<>();
        public List<RandomRewardGroup> randomGroups = new ArrayList<>();
        public List<ClassReward> classRewards = new ArrayList<>();
    }

    public static class ItemReward {
        public String itemId = "minecraft:air";
        public int count = 1;
    }

    public static class RandomRewardGroup {
        public String id = "";
        public boolean pickOne = true;
        public List<RandomRewardEntry> entries = new ArrayList<>();
    }

    public static class RandomRewardEntry {
        public String itemId = "minecraft:air";
        public int count = 1;
        public double chance = 0.0;
        public int weight = 1;
    }

    public static class ClassReward {
        public String classId = "";
        public int xpLevelsBonus = 0;
        public List<String> commands = new ArrayList<>();
        public List<ItemReward> guaranteedItems = new ArrayList<>();
    }

    public static class Objective {
        public String type = "KILL_MOB";
        public String key = "";
        public int target = 1;

        public static Objective killMob(String entityId, int target) {
            Objective o = new Objective();
            o.type = "KILL_MOB";
            o.key = entityId;
            o.target = target;
            return o;
        }

        public static Objective breakBlock(String blockId, int target) {
            Objective o = new Objective();
            o.type = "BREAK_BLOCK";
            o.key = blockId;
            o.target = target;
            return o;
        }

        public static Objective talkNpc(String npcId, int target) {
            Objective o = new Objective();
            o.type = "TALK_NPC";
            o.key = npcId;
            o.target = target;
            return o;
        }
    }
}

package qorhvkdy.qorhvkdy.rpgmod.npc.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Citizens/Denizen 스타일 대화 스크립트 JSON.
 */
public class NpcDialogueJson {
    public int dataVersion = 1;
    public List<Dialogue> dialogues = new ArrayList<>();

    public NpcDialogueJson() {
        Dialogue sample = new Dialogue();
        sample.npcId = "trainer_warrior";
        sample.startStep = "start";

        Step start = new Step();
        start.id = "start";
        start.text = "안녕, 전사 수련을 시작할 준비가 되었나?";
        start.options.add(Option.next("네, 시작할게요.", "offer_quest"));
        start.options.add(Option.end("아직 아니에요."));

        Step offer = new Step();
        offer.id = "offer_quest";
        offer.text = "좋아. 이 수련 퀘스트를 받겠나?";
        offer.options.add(Option.actionAndEnd("퀘스트 수락", "QUEST_ACCEPT", "starter_warrior_trial"));
        offer.options.add(Option.end("다음에 받을게요."));

        sample.steps.add(start);
        sample.steps.add(offer);
        dialogues.add(sample);
    }

    public static class Dialogue {
        public String npcId = "";
        public String startStep = "start";
        public List<Step> steps = new ArrayList<>();
    }

    public static class Step {
        public String id = "";
        public String text = "";
        public List<Option> options = new ArrayList<>();
    }

    public static class Option {
        public String text = "";
        public String next = "";
        public boolean end = false;
        public String actionType = "";
        public String actionValue = "";

        public static Option next(String text, String nextStep) {
            Option o = new Option();
            o.text = text;
            o.next = nextStep;
            o.end = false;
            return o;
        }

        public static Option end(String text) {
            Option o = new Option();
            o.text = text;
            o.end = true;
            return o;
        }

        public static Option actionAndEnd(String text, String actionType, String actionValue) {
            Option o = new Option();
            o.text = text;
            o.end = true;
            o.actionType = actionType;
            o.actionValue = actionValue;
            return o;
        }
    }
}

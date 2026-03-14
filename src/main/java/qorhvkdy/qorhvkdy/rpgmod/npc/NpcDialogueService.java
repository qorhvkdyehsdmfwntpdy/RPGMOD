package qorhvkdy.qorhvkdy.rpgmod.npc;

import net.minecraft.server.level.ServerPlayer;
import qorhvkdy.qorhvkdy.rpgmod.network.ModNetwork;
import qorhvkdy.qorhvkdy.rpgmod.npc.data.NpcDialogueJson;
import qorhvkdy.qorhvkdy.rpgmod.npc.data.NpcDialogueRepository;
import qorhvkdy.qorhvkdy.rpgmod.quest.QuestService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NPC 대화 상태머신 서비스.
 */
public final class NpcDialogueService {
    private static final Map<String, DialogueRuntime> DIALOGUES = new ConcurrentHashMap<>();
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private NpcDialogueService() {
    }

    public static synchronized void bootstrap() {
        NpcDialogueRepository.bootstrap();
        reload();
    }

    public static synchronized void reload() {
        NpcDialogueRepository.reload();
        DIALOGUES.clear();
        for (NpcDialogueJson.Dialogue d : NpcDialogueRepository.get().dialogues) {
            String npcId = normalize(d.npcId);
            if (npcId.isBlank()) {
                continue;
            }
            Map<String, NpcDialogueJson.Step> stepMap = new LinkedHashMap<>();
            for (NpcDialogueJson.Step step : d.steps) {
                String stepId = normalize(step.id);
                if (!stepId.isBlank()) {
                    stepMap.put(stepId, step);
                }
            }
            DIALOGUES.put(npcId, new DialogueRuntime(npcId, normalize(d.startStep), stepMap));
        }
        SESSIONS.clear();
    }

    public static Optional<DialogueView> talk(ServerPlayer player, String npcId) {
        DialogueRuntime runtime = DIALOGUES.get(normalize(npcId));
        if (runtime == null) {
            return Optional.empty();
        }
        String start = runtime.startStep.isBlank() ? "start" : runtime.startStep;
        if (!runtime.steps.containsKey(start)) {
            return Optional.empty();
        }
        Session session = new Session(runtime.npcId, start);
        SESSIONS.put(player.getUUID(), session);
        if (QuestService.onTalkNpc(player, runtime.npcId)) {
            QuestService.sync(player);
        }
        return toView(runtime, start);
    }

    public static Optional<DialogueView> choose(ServerPlayer player, int index) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return Optional.empty();
        }
        DialogueRuntime runtime = DIALOGUES.get(session.npcId);
        if (runtime == null) {
            return Optional.empty();
        }
        NpcDialogueJson.Step step = runtime.steps.get(session.stepId);
        if (step == null || index < 1 || index > step.options.size()) {
            return Optional.empty();
        }
        NpcDialogueJson.Option option = step.options.get(index - 1);
        applyOptionAction(player, option);
        if (option.end) {
            SESSIONS.remove(player.getUUID());
            return Optional.of(new DialogueView(runtime.npcId, "대화가 종료되었습니다.", List.of()));
        }
        String next = normalize(option.next);
        if (next.isBlank() || !runtime.steps.containsKey(next)) {
            SESSIONS.remove(player.getUUID());
            return Optional.of(new DialogueView(runtime.npcId, "다음 단계가 없어 대화를 종료합니다.", List.of()));
        }
        session.stepId = next;
        return toView(runtime, next);
    }

    public static void exit(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    private static void applyOptionAction(ServerPlayer player, NpcDialogueJson.Option option) {
        String actionType = normalize(option.actionType);
        String actionValue = option.actionValue == null ? "" : option.actionValue.trim();
        if (actionType.isBlank() || actionValue.isBlank()) {
            return;
        }
        boolean changed = false;
        switch (actionType) {
            case "quest_accept" -> changed = QuestService.accept(player, actionValue);
            case "quest_complete" -> changed = QuestService.complete(player, actionValue);
            default -> {
            }
        }
        if (changed) {
            QuestService.sync(player);
        }
    }

    private static Optional<DialogueView> toView(DialogueRuntime runtime, String stepId) {
        NpcDialogueJson.Step step = runtime.steps.get(stepId);
        if (step == null) {
            return Optional.empty();
        }
        List<String> options = new ArrayList<>();
        int i = 1;
        for (NpcDialogueJson.Option option : step.options) {
            options.add(i + ". " + option.text);
            i++;
        }
        return Optional.of(new DialogueView(runtime.npcId, step.text, options));
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private record DialogueRuntime(String npcId, String startStep, Map<String, NpcDialogueJson.Step> steps) {
    }

    private static final class Session {
        private final String npcId;
        private String stepId;

        private Session(String npcId, String stepId) {
            this.npcId = npcId;
            this.stepId = stepId;
        }
    }

    public record DialogueView(String npcId, String text, List<String> options) {
    }
}

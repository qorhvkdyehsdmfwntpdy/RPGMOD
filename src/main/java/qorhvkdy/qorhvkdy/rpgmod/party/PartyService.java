package qorhvkdy.qorhvkdy.rpgmod.party;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import qorhvkdy.qorhvkdy.rpgmod.audit.RpgAuditLogService;
import qorhvkdy.qorhvkdy.rpgmod.party.data.PartyLeaderRepository;
import qorhvkdy.qorhvkdy.rpgmod.party.data.PartyMemberRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 파티 서비스.
 * 생성/초대/수락/탈퇴/추방/해산을 단순 구조로 제공한다.
 */
public final class PartyService {
    private static final long INVITE_EXPIRE_MS = 60_000L;
    private static final ConcurrentHashMap<UUID, Invite> INVITES = new ConcurrentHashMap<>();

    private record Invite(String partyId, UUID inviterId, long expiresAt) {
    }

    private PartyService() {
    }

    public static void bootstrap() {
        PartyMemberRepository.bootstrap();
        PartyLeaderRepository.bootstrap();
    }

    public static void reload() {
        PartyMemberRepository.reload();
        PartyLeaderRepository.reload();
        INVITES.clear();
    }

    public static Optional<String> getPartyId(UUID playerId) {
        return Optional.ofNullable(PartyMemberRepository.get().get(normalize(playerId)));
    }

    public static List<UUID> membersOfParty(String partyId) {
        String normalizedParty = normalize(partyId);
        if (normalizedParty.isBlank()) {
            return List.of();
        }
        ArrayList<UUID> out = new ArrayList<>();
        for (var entry : PartyMemberRepository.get().entrySet()) {
            if (!normalizedParty.equals(entry.getValue())) {
                continue;
            }
            try {
                out.add(UUID.fromString(entry.getKey()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return out;
    }

    public static List<ServerPlayer> onlineMembers(MinecraftServer server, String partyId) {
        if (server == null) {
            return List.of();
        }
        ArrayList<ServerPlayer> out = new ArrayList<>();
        for (UUID memberId : membersOfParty(partyId)) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberId);
            if (player != null) {
                out.add(player);
            }
        }
        return out;
    }

    public static boolean isLeader(UUID playerId) {
        String player = normalize(playerId);
        return PartyLeaderRepository.get().values().stream().anyMatch(player::equals);
    }

    public static Optional<UUID> leaderOfParty(String partyId) {
        String raw = PartyLeaderRepository.get().get(normalize(partyId));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static String createParty(UUID ownerId) {
        if (getPartyId(ownerId).isPresent()) {
            return "Already in party.";
        }
        String partyId = newPartyId(ownerId);
        PartyMemberRepository.get().put(normalize(ownerId), partyId);
        PartyLeaderRepository.get().put(partyId, normalize(ownerId));
        PartyMemberRepository.save();
        PartyLeaderRepository.save();
        RpgAuditLogService.permission("party_create owner=" + ownerId + ", partyId=" + partyId);
        return "Party created: " + partyId;
    }

    public static String disband(UUID ownerId) {
        String owner = normalize(ownerId);
        Optional<String> partyOpt = getPartyId(ownerId);
        if (partyOpt.isEmpty()) {
            return "Not in party.";
        }
        String partyId = partyOpt.get();
        String leader = PartyLeaderRepository.get().get(partyId);
        if (!owner.equals(leader)) {
            return "Only leader can disband.";
        }
        List<String> toRemove = new ArrayList<>();
        for (var entry : PartyMemberRepository.get().entrySet()) {
            if (partyId.equals(entry.getValue())) {
                toRemove.add(entry.getKey());
            }
        }
        for (String uuid : toRemove) {
            PartyMemberRepository.get().remove(uuid);
        }
        PartyLeaderRepository.get().remove(partyId);
        PartyMemberRepository.save();
        PartyLeaderRepository.save();
        RpgAuditLogService.permission("party_disband owner=" + ownerId + ", partyId=" + partyId);
        return "Party disbanded: " + partyId;
    }

    public static String invite(UUID inviterId, UUID targetId) {
        Optional<String> partyOpt = getPartyId(inviterId);
        if (partyOpt.isEmpty()) {
            return "Create party first.";
        }
        String partyId = partyOpt.get();
        if (!leaderOfParty(partyId).map(id -> id.equals(inviterId)).orElse(false)) {
            return "Only leader can invite.";
        }
        if (getPartyId(targetId).isPresent()) {
            return "Target already in party.";
        }
        INVITES.put(targetId, new Invite(partyId, inviterId, System.currentTimeMillis() + INVITE_EXPIRE_MS));
        return "Invite sent to target.";
    }

    public static String acceptInvite(UUID targetId) {
        Invite invite = INVITES.get(targetId);
        if (invite == null) {
            return "No pending invite.";
        }
        if (System.currentTimeMillis() > invite.expiresAt) {
            INVITES.remove(targetId);
            return "Invite expired.";
        }
        if (getPartyId(targetId).isPresent()) {
            INVITES.remove(targetId);
            return "Already in party.";
        }
        PartyMemberRepository.get().put(normalize(targetId), invite.partyId);
        PartyMemberRepository.save();
        INVITES.remove(targetId);
        RpgAuditLogService.permission("party_accept target=" + targetId + ", partyId=" + invite.partyId);
        return "Joined party: " + invite.partyId;
    }

    public static String leave(UUID playerId) {
        Optional<String> partyOpt = getPartyId(playerId);
        if (partyOpt.isEmpty()) {
            return "Not in party.";
        }
        String partyId = partyOpt.get();
        if (leaderOfParty(partyId).map(id -> id.equals(playerId)).orElse(false)) {
            return "Leader must disband or kick before leaving.";
        }
        removeFromParty(playerId);
        RpgAuditLogService.permission("party_leave player=" + playerId + ", partyId=" + partyId);
        return "Left party.";
    }

    public static String kick(UUID leaderId, UUID targetId) {
        Optional<String> partyOpt = getPartyId(leaderId);
        if (partyOpt.isEmpty()) {
            return "Not in party.";
        }
        String partyId = partyOpt.get();
        if (!leaderOfParty(partyId).map(id -> id.equals(leaderId)).orElse(false)) {
            return "Only leader can kick.";
        }
        Optional<String> targetParty = getPartyId(targetId);
        if (targetParty.isEmpty() || !partyId.equals(targetParty.get())) {
            return "Target is not in your party.";
        }
        if (leaderId.equals(targetId)) {
            return "Leader cannot kick self.";
        }
        removeFromParty(targetId);
        RpgAuditLogService.permission("party_kick leader=" + leaderId + ", target=" + targetId + ", partyId=" + partyId);
        return "Kicked target from party.";
    }

    public static void assignParty(UUID playerId, String partyId) {
        String normalizedParty = normalize(partyId);
        PartyMemberRepository.get().put(normalize(playerId), normalizedParty);
        PartyLeaderRepository.get().putIfAbsent(normalizedParty, normalize(playerId));
        PartyMemberRepository.save();
        PartyLeaderRepository.save();
    }

    public static boolean removeFromParty(UUID playerId) {
        boolean removed = PartyMemberRepository.get().remove(normalize(playerId)) != null;
        if (removed) {
            PartyMemberRepository.save();
        }
        return removed;
    }

    public static String forceKick(ServerPlayer target) {
        Optional<String> beforeParty = getPartyId(target.getUUID());
        if (beforeParty.isEmpty()) {
            return target.getName().getString() + " is not in a party.";
        }
        removeFromParty(target.getUUID());
        return "Force-kicked " + target.getName().getString() + " from party " + beforeParty.get();
    }

    private static String normalize(UUID id) {
        return normalize(id.toString());
    }

    private static String newPartyId(UUID ownerId) {
        String seed = ownerId.toString().replace("-", "");
        int r = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "p_" + seed.substring(0, 6).toLowerCase(Locale.ROOT) + "_" + r;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

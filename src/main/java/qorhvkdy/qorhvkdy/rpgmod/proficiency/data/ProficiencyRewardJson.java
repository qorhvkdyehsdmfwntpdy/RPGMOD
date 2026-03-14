package qorhvkdy.qorhvkdy.rpgmod.proficiency.data;

/**
 * 숙련도 획득량 JSON DTO.
 * 카테고리별 주요 행동 보상치를 분리해 운영 중 밸런스 조정이 쉽다.
 */
public class ProficiencyRewardJson {
    public int dataVersion = 1;
    public int classKillExp = 10;
    public int weaponHitExp = 4;
    public int gatheringBreakExp = 6;
    public int miningBreakExp = 6;
}


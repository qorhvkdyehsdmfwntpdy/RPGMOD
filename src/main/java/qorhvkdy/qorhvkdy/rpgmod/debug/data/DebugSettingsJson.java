package qorhvkdy.qorhvkdy.rpgmod.debug.data;

/**
 * 운영/디버그 설정 DTO.
 */
public class DebugSettingsJson {
    public int dataVersion = 1;
    public String progressionLogLevel = "info"; // off | info | debug
    public boolean dropVerboseLog = false;
    public boolean jsonValidationLog = true;
    public boolean combatSamplerEnabled = true;
    public int combatSamplerWindowSeconds = 20;
    public int combatSamplerMinHits = 8;
}

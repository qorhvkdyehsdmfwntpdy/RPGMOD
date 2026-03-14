package qorhvkdy.qorhvkdy.rpgmod.classes.resource;

/**
 * 런타임 클래스 리소스 프로필.
 */
public record ClassResourceProfile(
        String resourceType,
        double maxBase,
        double maxPerLevel,
        double regenPerSecond,
        double gainOnHit,
        double gainOnCrit,
        double gainOnKill
) {
    public static ClassResourceProfile none() {
        return new ClassResourceProfile("none", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }
}

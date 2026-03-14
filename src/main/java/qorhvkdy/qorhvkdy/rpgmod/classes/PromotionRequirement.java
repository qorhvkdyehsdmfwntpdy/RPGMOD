package qorhvkdy.qorhvkdy.rpgmod.classes;

/**
 * Requirement strategy for promotion checks.
 */
public interface PromotionRequirement {
    RequirementCheckResult check(RequirementSpec spec, PromotionCheckContext context);

    record RequirementCheckResult(boolean passed, String message) {
        public static RequirementCheckResult pass() {
            return new RequirementCheckResult(true, "");
        }

        public static RequirementCheckResult fail(String message) {
            return new RequirementCheckResult(false, message);
        }
    }
}


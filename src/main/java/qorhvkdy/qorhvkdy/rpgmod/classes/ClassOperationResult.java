package qorhvkdy.qorhvkdy.rpgmod.classes;

import java.util.List;

/**
 * Unified operation result for class/promotion actions.
 */
public record ClassOperationResult(boolean success, String message, List<String> details) {
    public static ClassOperationResult ok(String message) {
        return new ClassOperationResult(true, message, List.of());
    }

    public static ClassOperationResult fail(String message) {
        return new ClassOperationResult(false, message, List.of());
    }

    public static ClassOperationResult fail(String message, List<String> details) {
        return new ClassOperationResult(false, message, List.copyOf(details));
    }
}


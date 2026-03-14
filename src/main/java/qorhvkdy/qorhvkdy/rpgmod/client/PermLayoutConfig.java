package qorhvkdy.qorhvkdy.rpgmod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Perm GUI 레이아웃 핫리로드 설정.
 */
public final class PermLayoutConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get().resolve("rpgmod").resolve("perm_ui.json");
    private static final long CHECK_INTERVAL_MS = 500L;

    private static LayoutData layout = LayoutData.defaults();
    private static long version = 1L;
    private static long lastCheckedAt = 0L;
    private static String lastRawJson = "";

    static {
        forceReload();
    }

    private PermLayoutConfig() {
    }

    public static synchronized Snapshot snapshot() {
        reloadIfChanged();
        return new Snapshot(layout, version);
    }

    public static synchronized void forceReload() {
        ensureFile();
        String raw = readRaw();
        LayoutData loaded = parse(raw);
        if (loaded == null) {
            loaded = LayoutData.defaults();
        }
        loaded.validate();
        layout = loaded;
        version++;
        lastRawJson = raw;
    }

    private static void reloadIfChanged() {
        long now = System.currentTimeMillis();
        if (now - lastCheckedAt < CHECK_INTERVAL_MS) {
            return;
        }
        lastCheckedAt = now;
        String raw = readRaw();
        if (raw.equals(lastRawJson)) {
            return;
        }
        LayoutData loaded = parse(raw);
        if (loaded == null) {
            return;
        }
        loaded.validate();
        layout = loaded;
        version++;
        lastRawJson = raw;
    }

    private static LayoutData parse(String raw) {
        try {
            LayoutData loaded = GSON.fromJson(raw, LayoutData.class);
            return loaded == null ? LayoutData.defaults() : loaded;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String readRaw() {
        try {
            return Files.readString(FILE_PATH, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static void ensureFile() {
        try {
            if (Files.notExists(FILE_PATH.getParent())) {
                Files.createDirectories(FILE_PATH.getParent());
            }
            if (Files.notExists(FILE_PATH)) {
                try (Writer writer = Files.newBufferedWriter(FILE_PATH, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                    GSON.toJson(LayoutData.defaults(), writer);
                }
            }
        } catch (IOException ignored) {
        }
    }

    public record Snapshot(LayoutData layout, long version) {
    }

    public static final class LayoutData {
        public Panel panel = Panel.defaults();
        public Buttons buttons = Buttons.defaults();
        public Admin admin = Admin.defaults();

        public static LayoutData defaults() {
            return new LayoutData();
        }

        public void validate() {
            if (panel == null) panel = Panel.defaults();
            if (buttons == null) buttons = Buttons.defaults();
            if (admin == null) admin = Admin.defaults();
            panel.validate();
            buttons.validate();
            admin.validate();
        }
    }

    public static final class Panel {
        public int width = 380;
        public int height = 260;
        public int titleLeft = 12;
        public int titleTop = 10;
        public int innerPadding = 8;

        public static Panel defaults() {
            return new Panel();
        }

        public void validate() {
            width = clamp(width, 300, 760);
            height = clamp(height, 220, 560);
            titleLeft = clamp(titleLeft, 6, 60);
            titleTop = clamp(titleTop, 4, 30);
            innerPadding = clamp(innerPadding, 4, 24);
        }
    }

    public static final class Buttons {
        public int left = 18;
        public int top = 60;
        public int width = 160;
        public int height = 20;
        public int colGap = 12;
        public int rowGap = 8;
        public int closeWidth = 64;
        public int closeHeight = 16;
        public int closeRightPadding = 16;
        public int closeBottomPadding = 10;

        public static Buttons defaults() {
            return new Buttons();
        }

        public void validate() {
            left = clamp(left, 4, 180);
            top = clamp(top, 40, 260);
            width = clamp(width, 80, 260);
            height = clamp(height, 14, 30);
            colGap = clamp(colGap, 2, 40);
            rowGap = clamp(rowGap, 2, 20);
            closeWidth = clamp(closeWidth, 40, 140);
            closeHeight = clamp(closeHeight, 14, 30);
            closeRightPadding = clamp(closeRightPadding, 6, 40);
            closeBottomPadding = clamp(closeBottomPadding, 4, 24);
        }
    }

    public static final class Admin {
        public int targetInputLeft = 18;
        public int targetInputTop = 174;
        public int targetInputWidth = 200;
        public int targetInputHeight = 18;
        public int uuidInputLeft = 18;
        public int uuidInputTop = 86;
        public int uuidInputWidth = 220;
        public int groupInputLeft = 244;
        public int groupInputTop = 86;
        public int groupInputWidth = 116;
        public int nodeInputLeft = 18;
        public int nodeInputTop = 110;
        public int nodeInputWidth = 342;
        public int selectorInputLeft = 18;
        public int selectorInputTop = 158;
        public int selectorInputWidth = 180;
        public int tempSecondsInputLeft = 18;
        public int tempSecondsInputTop = 134;
        public int tempSecondsInputWidth = 120;

        public static Admin defaults() {
            return new Admin();
        }

        public void validate() {
            targetInputLeft = clamp(targetInputLeft, 4, 300);
            targetInputTop = clamp(targetInputTop, 80, 500);
            targetInputWidth = clamp(targetInputWidth, 120, 280);
            targetInputHeight = clamp(targetInputHeight, 14, 24);
            uuidInputLeft = clamp(uuidInputLeft, 4, 360);
            uuidInputTop = clamp(uuidInputTop, 40, 500);
            uuidInputWidth = clamp(uuidInputWidth, 120, 320);
            groupInputLeft = clamp(groupInputLeft, 4, 420);
            groupInputTop = clamp(groupInputTop, 40, 500);
            groupInputWidth = clamp(groupInputWidth, 80, 180);
            nodeInputLeft = clamp(nodeInputLeft, 4, 360);
            nodeInputTop = clamp(nodeInputTop, 40, 520);
            nodeInputWidth = clamp(nodeInputWidth, 120, 420);
            selectorInputLeft = clamp(selectorInputLeft, 4, 360);
            selectorInputTop = clamp(selectorInputTop, 40, 520);
            selectorInputWidth = clamp(selectorInputWidth, 80, 220);
            tempSecondsInputLeft = clamp(tempSecondsInputLeft, 4, 360);
            tempSecondsInputTop = clamp(tempSecondsInputTop, 40, 520);
            tempSecondsInputWidth = clamp(tempSecondsInputWidth, 60, 180);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

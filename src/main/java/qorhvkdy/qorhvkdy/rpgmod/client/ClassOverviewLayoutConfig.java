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
 * Hot-reload layout for class overview screen.
 */
public final class ClassOverviewLayoutConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get().resolve("rpgmod").resolve("class_ui.json");
    private static final long CHECK_INTERVAL_MS = 500L;

    private static LayoutData layout = LayoutData.defaults();
    private static long version = 1L;
    private static long lastCheckedAt = 0L;
    private static String lastRawJson = "";

    static {
        forceReload();
    }

    private ClassOverviewLayoutConfig() {
    }

    public static synchronized Snapshot snapshot() {
        reloadIfChanged();
        return new Snapshot(layout, version);
    }

    public static synchronized void forceReload() {
        ensureFileExists();
        String raw = readRawFromDisk();
        applyIfValid(raw);
        lastCheckedAt = System.currentTimeMillis();
    }

    private static void reloadIfChanged() {
        long now = System.currentTimeMillis();
        if (now - lastCheckedAt < CHECK_INTERVAL_MS) {
            return;
        }
        lastCheckedAt = now;
        String raw = readRawFromDisk();
        if (raw.equals(lastRawJson)) {
            return;
        }
        applyIfValid(raw);
    }

    private static boolean applyIfValid(String raw) {
        LayoutData loaded = parse(raw);
        if (loaded == null) {
            return false;
        }
        loaded.validate();
        layout = loaded;
        version++;
        lastRawJson = raw;
        return true;
    }

    private static LayoutData parse(String raw) {
        try {
            LayoutData loaded = GSON.fromJson(raw, LayoutData.class);
            return loaded == null ? LayoutData.defaults() : loaded;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String readRawFromDisk() {
        try {
            return Files.readString(FILE_PATH, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    private static void ensureFileExists() {
        try {
            if (Files.notExists(FILE_PATH.getParent())) {
                Files.createDirectories(FILE_PATH.getParent());
            }
            if (Files.notExists(FILE_PATH)) {
                writeDefaults();
            }
        } catch (IOException ignored) {
        }
    }

    private static void writeDefaults() {
        LayoutData defaults = LayoutData.defaults();
        try (Writer writer = Files.newBufferedWriter(
                FILE_PATH,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            GSON.toJson(defaults, writer);
        } catch (IOException ignored) {
        }
    }

    public record Snapshot(LayoutData layout, long version) {
    }

    public static final class LayoutData {
        public Panel panel = Panel.defaults();
        public Info info = Info.defaults();
        public Buttons buttons = Buttons.defaults();

        public static LayoutData defaults() {
            LayoutData data = new LayoutData();
            data.panel = Panel.defaults();
            data.info = Info.defaults();
            data.buttons = Buttons.defaults();
            return data;
        }

        public void validate() {
            if (panel == null) panel = Panel.defaults();
            if (info == null) info = Info.defaults();
            if (buttons == null) buttons = Buttons.defaults();
            panel.validate();
            info.validate();
            buttons.validate();
        }
    }

    public static final class Panel {
        public int width = 560;
        public int height = 360;
        public int titleLeft = 12;
        public int titleTop = 10;
        public int innerPadding = 8;

        public static Panel defaults() {
            return new Panel();
        }

        public void validate() {
            width = clamp(width, 360, 900);
            height = clamp(height, 240, 700);
            titleLeft = clamp(titleLeft, 4, 40);
            titleTop = clamp(titleTop, 4, 30);
            innerPadding = clamp(innerPadding, 4, 24);
        }
    }

    public static final class Info {
        public int left = 12;
        public int top = 34;
        public int lineStep = 14;
        public int headerGap = 16;

        public static Info defaults() {
            return new Info();
        }

        public void validate() {
            left = clamp(left, 4, 140);
            top = clamp(top, 20, 120);
            lineStep = clamp(lineStep, 10, 24);
            headerGap = clamp(headerGap, 8, 32);
        }
    }

    public static final class Buttons {
        public int backWidth = 64;
        public int backHeight = 16;
        public int backRightPadding = 16;
        public int backBottomPadding = 10;
        public int baseStartLeft = 14;
        public int baseY = 128;
        public int baseButtonWidth = 110;
        public int baseButtonHeight = 18;
        public int baseGap = 8;
        public int promoteStartLeft = 14;
        public int promoteStartY = 214;
        public int promoteWidth = 530;
        public int promoteHeight = 20;
        public int promoteGap = 6;

        public static Buttons defaults() {
            return new Buttons();
        }

        public void validate() {
            backWidth = clamp(backWidth, 40, 140);
            backHeight = clamp(backHeight, 14, 28);
            backRightPadding = clamp(backRightPadding, 6, 40);
            backBottomPadding = clamp(backBottomPadding, 4, 24);
            baseStartLeft = clamp(baseStartLeft, 4, 180);
            baseY = clamp(baseY, 70, 320);
            baseButtonWidth = clamp(baseButtonWidth, 64, 220);
            baseButtonHeight = clamp(baseButtonHeight, 14, 28);
            baseGap = clamp(baseGap, 2, 40);
            promoteStartLeft = clamp(promoteStartLeft, 4, 180);
            promoteStartY = clamp(promoteStartY, 140, 520);
            promoteWidth = clamp(promoteWidth, 120, 760);
            promoteHeight = clamp(promoteHeight, 14, 28);
            promoteGap = clamp(promoteGap, 2, 20);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

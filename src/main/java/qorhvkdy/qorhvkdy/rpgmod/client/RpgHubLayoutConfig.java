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
 * Hot-reload layout for RPG hub screen.
 */
public final class RpgHubLayoutConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get().resolve("rpgmod").resolve("hub_ui.json");
    private static final long CHECK_INTERVAL_MS = 500L;

    private static LayoutData layout = LayoutData.defaults();
    private static long version = 1L;
    private static long lastCheckedAt = 0L;
    private static String lastRawJson = "";

    static {
        forceReload();
    }

    private RpgHubLayoutConfig() {
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
        public Summary summary = Summary.defaults();
        public Buttons buttons = Buttons.defaults();

        public static LayoutData defaults() {
            LayoutData data = new LayoutData();
            data.panel = Panel.defaults();
            data.summary = Summary.defaults();
            data.buttons = Buttons.defaults();
            return data;
        }

        public void validate() {
            if (panel == null) panel = Panel.defaults();
            if (summary == null) summary = Summary.defaults();
            if (buttons == null) buttons = Buttons.defaults();
            panel.validate();
            summary.validate();
            buttons.validate();
        }
    }

    public static final class Panel {
        public int width = 360;
        public int height = 260;
        public int titleLeft = 12;
        public int titleTop = 10;
        public int innerPadding = 8;
        public int sectionTop = 58;
        public int sectionBottomPadding = 36;

        public static Panel defaults() {
            return new Panel();
        }

        public void validate() {
            width = clamp(width, 300, 700);
            height = clamp(height, 220, 500);
            titleLeft = clamp(titleLeft, 4, 40);
            titleTop = clamp(titleTop, 4, 30);
            innerPadding = clamp(innerPadding, 4, 24);
            sectionTop = clamp(sectionTop, 40, 120);
            sectionBottomPadding = clamp(sectionBottomPadding, 16, 80);
        }
    }

    public static final class Summary {
        public int left = 12;
        public int top = 32;
        public int lineStep = 12;
        public int colGap = 170;

        public static Summary defaults() {
            return new Summary();
        }

        public void validate() {
            left = clamp(left, 4, 120);
            top = clamp(top, 20, 100);
            lineStep = clamp(lineStep, 10, 24);
            colGap = clamp(colGap, 80, 340);
        }
    }

    public static final class Buttons {
        public int startLeft = 18;
        public int startTop = 82;
        public int width = 140;
        public int height = 20;
        public int rowGap = 6;
        public int colGap = 12;
        public int closeWidth = 64;
        public int closeHeight = 16;
        public int closeRightPadding = 18;
        public int closeBottomPadding = 10;

        public static Buttons defaults() {
            return new Buttons();
        }

        public void validate() {
            startLeft = clamp(startLeft, 6, 160);
            startTop = clamp(startTop, 50, 220);
            width = clamp(width, 80, 260);
            height = clamp(height, 14, 30);
            rowGap = clamp(rowGap, 2, 20);
            colGap = clamp(colGap, 4, 40);
            closeWidth = clamp(closeWidth, 40, 120);
            closeHeight = clamp(closeHeight, 14, 28);
            closeRightPadding = clamp(closeRightPadding, 6, 40);
            closeBottomPadding = clamp(closeBottomPadding, 4, 24);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

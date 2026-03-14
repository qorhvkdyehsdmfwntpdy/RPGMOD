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
 * 숙련도 화면 레이아웃 설정.
 */
public final class ProficiencyLayoutConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get().resolve("rpgmod").resolve("proficiency_ui.json");
    private static final long CHECK_INTERVAL_MS = 500L;

    private static LayoutData layout = LayoutData.defaults();
    private static long version = 1L;
    private static long lastCheckedAt = 0L;
    private static String lastRawJson = "";

    static {
        forceReload();
    }

    private ProficiencyLayoutConfig() {
    }

    public static synchronized Snapshot snapshot() {
        reloadIfChanged();
        return new Snapshot(layout, version);
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
        public Table table = Table.defaults();
        public Buttons buttons = Buttons.defaults();

        public static LayoutData defaults() {
            return new LayoutData();
        }

        public void validate() {
            if (panel == null) panel = Panel.defaults();
            if (table == null) table = Table.defaults();
            if (buttons == null) buttons = Buttons.defaults();
            panel.validate();
            table.validate();
            buttons.validate();
        }
    }

    public static final class Panel {
        public int width = 380;
        public int height = 250;
        public int titleLeft = 12;
        public int titleTop = 10;

        public static Panel defaults() {
            return new Panel();
        }

        public void validate() {
            width = clamp(width, 260, 720);
            height = clamp(height, 180, 480);
            titleLeft = clamp(titleLeft, 4, 60);
            titleTop = clamp(titleTop, 4, 30);
        }
    }

    public static final class Table {
        public int left = 14;
        public int top = 36;
        public int rowStep = 24;
        public int levelCol = 170;
        public int expCol = 230;

        public static Table defaults() {
            return new Table();
        }

        public void validate() {
            left = clamp(left, 6, 200);
            top = clamp(top, 24, 160);
            rowStep = clamp(rowStep, 16, 40);
            levelCol = clamp(levelCol, 90, 300);
            expCol = clamp(expCol, 140, 340);
        }
    }

    public static final class Buttons {
        public int backWidth = 64;
        public int backHeight = 16;
        public int backRightPadding = 14;
        public int backBottomPadding = 10;

        public static Buttons defaults() {
            return new Buttons();
        }

        public void validate() {
            backWidth = clamp(backWidth, 40, 120);
            backHeight = clamp(backHeight, 14, 30);
            backRightPadding = clamp(backRightPadding, 4, 40);
            backBottomPadding = clamp(backBottomPadding, 4, 26);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}


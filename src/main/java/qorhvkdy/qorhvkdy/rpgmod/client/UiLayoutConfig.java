package qorhvkdy.qorhvkdy.rpgmod.client;

/*
 * [RPGMOD 파일 설명]
 * 역할: run/config/rpgmod_ui.json을 읽고 변경 감지/검증하는 레이아웃 로더입니다.
 * 수정 예시: 패널 폭을 바꾸려면 json의 hud.panelWidth 또는 gui.panelWidth를 수정합니다.
 */


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class UiLayoutConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get().resolve("rpgmod_ui.json");
    private static final long CHECK_INTERVAL_MS = 500L;

    private static LayoutData layout = LayoutData.defaults();
    private static long layoutVersion = 1L;
    private static long lastCheckedAt = 0L;
    private static String lastRawJson = "";

    static {
        forceReload();
    }

    private UiLayoutConfig() {
    }

    public static synchronized LayoutData get() {
        reloadIfChanged();
        return layout;
    }

    public static synchronized long version() {
        reloadIfChanged();
        return layoutVersion;
    }

    public static synchronized Snapshot snapshot() {
        reloadIfChanged();
        return new Snapshot(layout, layoutVersion);
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
        layoutVersion++;
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

    public static final class LayoutData {
        public Hud hud = new Hud();
        public Gui gui = new Gui();

        public static LayoutData defaults() {
            LayoutData data = new LayoutData();
            data.hud = Hud.defaults();
            data.gui = Gui.defaults();
            return data;
        }

        public void validate() {
            if (hud == null) {
                hud = Hud.defaults();
            }
            if (gui == null) {
                gui = Gui.defaults();
            }
            hud.validate();
            gui.validate();
        }
    }

    public record Snapshot(LayoutData layout, long version) {
    }

    public static final class Hud {
        public int panelWidth = 240;
        public int panelHeight = 32;
        public boolean alignLeft = true;
        public int leftMargin = 10;
        public boolean alignToHotbar = false;
        public int hotbarGap = 0;
        public int bottomMargin = -1;
        // 하위 호환용(기존 키). bottomMargin이 있으면 우선 적용됩니다.
        public int bottomOffset = 10;
        public int textLeftPadding = 8;
        public double fontScale = 1.25;
        public int expColor = 0xFF55FF55;

        public static Hud defaults() {
            return new Hud();
        }

        public void validate() {
            panelWidth = clamp(panelWidth, 120, 360);
            panelHeight = clamp(panelHeight, 18, 48);
            leftMargin = clamp(leftMargin, 0, 240);
            hotbarGap = clamp(hotbarGap, 0, 20);
            bottomOffset = clamp(bottomOffset, 0, 120);
            bottomMargin = bottomMargin < 0 ? bottomOffset : clamp(bottomMargin, 0, 120);
            textLeftPadding = clamp(textLeftPadding, 2, 24);
            fontScale = clampDouble(fontScale, 0.70, 3.00);
        }
    }

    public static final class Gui {
        public int panelWidth = 360;
        public int panelHeight = 252;
        public int primaryTop = 44;
        public int primaryRowStep = 21;
        public int buttonRightOffset = 64;
        public int derivedTop = 146;
        public int derivedRowStep = 16;
        public int derivedColumnGap = 28;

        public static Gui defaults() {
            return new Gui();
        }

        public void validate() {
            panelWidth = clamp(panelWidth, 280, 520);
            panelHeight = clamp(panelHeight, 200, 360);
            primaryTop = clamp(primaryTop, 34, 90);
            primaryRowStep = clamp(primaryRowStep, 16, 26);
            buttonRightOffset = clamp(buttonRightOffset, 48, 88);
            derivedTop = clamp(derivedTop, 120, 260);
            derivedRowStep = clamp(derivedRowStep, 12, 24);
            derivedColumnGap = clamp(derivedColumnGap, 8, 64);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

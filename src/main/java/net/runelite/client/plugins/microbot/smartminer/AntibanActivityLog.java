package net.runelite.client.plugins.microbot.smartminer;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AntibanActivityLog {
    private static final int MAX_ENTRIES = 10;
    private static final LinkedList<LogEntry> activityLog = new LinkedList<>();
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static class LogEntry {
        public final String timestamp;
        public final String message;
        public final LogType type;

        public LogEntry(String message, LogType type) {
            this.timestamp = LocalTime.now().format(timeFormatter);
            this.message = message;
            this.type = type;
        }
    }

    public enum LogType {
        MOUSE_MOVEMENT,
        ACTION_DELAY,
        BREAK,
        FATIGUE,
        ATTENTION,
        BEHAVIOR,
        MISTAKE,
        GENERAL
    }

    public static void log(String message, LogType type) {
        synchronized (activityLog) {
            activityLog.addFirst(new LogEntry(message, type));
            if (activityLog.size() > MAX_ENTRIES) {
                activityLog.removeLast();
            }
        }
    }

    public static List<LogEntry> getRecentActivity() {
        synchronized (activityLog) {
            return new ArrayList<>(activityLog);
        }
    }

    public static void clear() {
        synchronized (activityLog) {
            activityLog.clear();
        }
    }

    // Convenience methods for logging specific antiban activities
    public static void logMouseOffScreen() {
        log("🖱️ Mouse moved off screen", LogType.MOUSE_MOVEMENT);
    }

    public static void logRandomMouseMovement(String location) {
        log("🖱️ Random mouse to " + location, LogType.MOUSE_MOVEMENT);
    }

    public static void logNaturalMouseHover() {
        log("🖱️ Natural mouse hover before click", LogType.MOUSE_MOVEMENT);
    }

    public static void logActionCooldown(int delayMs) {
        log(String.format("⏸️ Action cooldown: %dms", delayMs), LogType.ACTION_DELAY);
    }

    public static void logMicroBreak(int durationMs) {
        log(String.format("☕ Micro break: %.1fs", durationMs / 1000.0), LogType.BREAK);
    }

    public static void logFatigueSlowdown() {
        log("😴 Fatigue: Slowing down actions", LogType.FATIGUE);
    }

    public static void logAttentionLapse() {
        log("💭 Attention span: Brief distraction", LogType.ATTENTION);
    }

    public static void logBehaviorVariation(String action) {
        log("🎭 Behavior variation: " + action, LogType.BEHAVIOR);
    }

    public static void logProfileSwitch(String profile) {
        log("🔄 Profile switched to: " + profile, LogType.BEHAVIOR);
    }

    public static void logMistake(String mistake) {
        log("❌ Human mistake: " + mistake, LogType.MISTAKE);
    }

    public static void logMining(String rockType) {
        log("⛏️ Mining " + rockType, LogType.GENERAL);
    }

    public static void logBanking() {
        log("🏦 Banking ores", LogType.GENERAL);
    }

    public static void logLocationChange(String location) {
        log("🗺️ Moved to " + location, LogType.GENERAL);
    }
}

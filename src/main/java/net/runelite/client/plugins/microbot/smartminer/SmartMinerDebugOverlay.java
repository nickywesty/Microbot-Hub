package net.runelite.client.plugins.microbot.smartminer;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.text.DecimalFormat;

public class SmartMinerDebugOverlay extends OverlayPanel {
    private final SmartMinerPlugin plugin;
    private final SmartMinerConfig config;
    private final DecimalFormat decimalFormat = new DecimalFormat("#,###");
    private final DecimalFormat percentFormat = new DecimalFormat("##0.0");

    // RuneScape color scheme
    private static final Color RS_ORANGE = new Color(255, 152, 31);
    private static final Color RS_YELLOW = new Color(255, 255, 0);
    private static final Color RS_GREEN = new Color(0, 255, 0);
    private static final Color RS_RED = new Color(255, 0, 0);
    private static final Color RS_CYAN = new Color(0, 255, 255);
    private static final Color RS_WHITE = new Color(255, 255, 255);
    private static final Color RS_GRAY = new Color(127, 127, 127);
    private static final Color RS_DARK_ORANGE = new Color(255, 140, 0);

    @Inject
    SmartMinerDebugOverlay(SmartMinerPlugin plugin, SmartMinerConfig config) {
        super(plugin);
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_RIGHT);
        setNaughty();
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.debugMode()) {
            return null;
        }

        try {
            panelComponent.setPreferredSize(new Dimension(350, 500));

            // Header with RS theme
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("⛏ Debug Console ⛏")
                    .color(RS_ORANGE)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().build());

            // === LIVE ACTIVITY LOG SECTION ===
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("📋 Live Activity Log")
                    .color(RS_YELLOW)
                    .build());

            var recentActivity = AntibanActivityLog.getRecentActivity();
            if (recentActivity.isEmpty()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  No activity yet...")
                        .leftColor(RS_GRAY)
                        .build());
            } else {
                for (AntibanActivityLog.LogEntry entry : recentActivity) {
                    Color logColor = getLogColor(entry.type);
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left(entry.timestamp)
                            .right(entry.message)
                            .leftColor(RS_GRAY)
                            .rightColor(logColor)
                            .build());
                }
            }

            panelComponent.getChildren().add(LineComponent.builder().build());

            // === ANTIBAN STATUS SECTION ===
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Antiban Status")
                    .color(RS_CYAN)
                    .build());

            // Natural Mouse
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  Natural Mouse:")
                    .right(config.naturalMouse() ? "✓ ENABLED" : "✗ DISABLED")
                    .rightColor(config.naturalMouse() ? RS_GREEN : RS_GRAY)
                    .build());

            // Move Mouse Off Screen
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  Mouse Off-Screen:")
                    .right(config.moveMouseOffScreen() ? "✓ ENABLED" : "✗ DISABLED")
                    .rightColor(config.moveMouseOffScreen() ? RS_GREEN : RS_GRAY)
                    .build());

            // Move Mouse Randomly
            if (config.moveMouseRandomly()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Random Mouse:")
                        .right(String.format("✓ 10%% chance", Rs2AntibanSettings.moveMouseRandomlyChance * 100))
                        .rightColor(RS_YELLOW)
                        .build());
            } else {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Random Mouse:")
                        .right("✗ DISABLED")
                        .rightColor(RS_GRAY)
                        .build());
            }

            // Action Cooldowns
            if (config.actionCooldowns()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Action Cooldown:")
                        .right(Rs2AntibanSettings.actionCooldownActive ? "⏸ WAITING" : "✓ READY")
                        .rightColor(Rs2AntibanSettings.actionCooldownActive ? RS_ORANGE : RS_GREEN)
                        .build());
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("    Chance:")
                        .right("20%")
                        .rightColor(RS_GRAY)
                        .build());
            } else {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Action Cooldown:")
                        .right("✗ DISABLED")
                        .rightColor(RS_GRAY)
                        .build());
            }

            // Micro Breaks
            if (config.microBreaks()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Micro Breaks:")
                        .right("✓ 5% chance")
                        .rightColor(RS_YELLOW)
                        .build());
            } else {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Micro Breaks:")
                        .right("✗ DISABLED")
                        .rightColor(RS_GRAY)
                        .build());
            }

            // Simulate Fatigue
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  Fatigue:")
                    .right(config.simulateFatigue() ? "✓ ENABLED" : "✗ DISABLED")
                    .rightColor(config.simulateFatigue() ? RS_ORANGE : RS_GRAY)
                    .build());

            // Attention Span
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  Attention Span:")
                    .right(config.simulateAttentionSpan() ? "✓ ENABLED" : "✗ DISABLED")
                    .rightColor(config.simulateAttentionSpan() ? RS_GREEN : RS_GRAY)
                    .build());

            // Behavioral Variability
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  Behavior Variation:")
                    .right(config.behavioralVariability() ? "✓ ENABLED" : "✗ DISABLED")
                    .rightColor(config.behavioralVariability() ? RS_GREEN : RS_GRAY)
                    .build());

            // Non-Linear Intervals
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  Non-Linear Timing:")
                    .right(config.nonLinearIntervals() ? "✓ ENABLED" : "✗ DISABLED")
                    .rightColor(config.nonLinearIntervals() ? RS_GREEN : RS_GRAY)
                    .build());

            // Profile Switching
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  Profile Switching:")
                    .right(config.profileSwitching() ? "✓ ENABLED" : "✗ DISABLED")
                    .rightColor(config.profileSwitching() ? RS_GREEN : RS_GRAY)
                    .build());

            // Simulate Mistakes
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  Mistake Simulation:")
                    .right(config.simulateMistakes() ? "✓ ENABLED" : "✗ DISABLED")
                    .rightColor(config.simulateMistakes() ? RS_ORANGE : RS_GRAY)
                    .build());

            // Use Play Style
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  Play Style:")
                    .right(config.usePlayStyle() ? "✓ ENABLED" : "✗ DISABLED")
                    .rightColor(config.usePlayStyle() ? RS_GREEN : RS_GRAY)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().build());

            // === SESSION STATS SECTION ===
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Session Statistics")
                    .color(RS_DARK_ORANGE)
                    .build());

            // Runtime
            long runtime = SmartMinerScript.getRuntime();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  Runtime:")
                    .right(formatTime(runtime))
                    .rightColor(RS_WHITE)
                    .build());

            // Ores mined
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  Ores Mined:")
                    .right(decimalFormat.format(SmartMinerScript.oresMined))
                    .rightColor(RS_YELLOW)
                    .build());

            // Trips (banks)
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  Bank Trips:")
                    .right(String.valueOf(SmartMinerScript.tripCount))
                    .rightColor(RS_CYAN)
                    .build());

            // Ores per hour
            if (runtime > 0) {
                long oresPerHour = (SmartMinerScript.oresMined * 3600000) / runtime;
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Ores/Hour:")
                        .right(decimalFormat.format(oresPerHour))
                        .rightColor(RS_GREEN)
                        .build());
            }

        } catch (Exception ex) {
            Microbot.log("Error rendering debug overlay: " + ex.getMessage());
        }

        return super.render(graphics);
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds = seconds % 60;
        minutes = minutes % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    private Color getLogColor(AntibanActivityLog.LogType type) {
        switch (type) {
            case MOUSE_MOVEMENT:
                return RS_CYAN;
            case ACTION_DELAY:
                return RS_ORANGE;
            case BREAK:
                return RS_YELLOW;
            case FATIGUE:
                return new Color(200, 150, 255); // Purple
            case ATTENTION:
                return new Color(255, 182, 193); // Light pink
            case BEHAVIOR:
                return RS_GREEN;
            case MISTAKE:
                return RS_RED;
            case GENERAL:
            default:
                return RS_WHITE;
        }
    }
}

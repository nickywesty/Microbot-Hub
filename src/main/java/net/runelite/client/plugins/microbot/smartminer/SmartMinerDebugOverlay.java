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
            panelComponent.setPreferredSize(new Dimension(320, 400));

            // Header with RS theme
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("⛏ Debug Console ⛏")
                    .color(RS_ORANGE)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().build());

            // === ANTIBAN ACTIVITY SECTION ===
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Antiban Activity")
                    .color(RS_CYAN)
                    .build());

            // Natural Mouse
            if (config.naturalMouse()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Natural Mouse:")
                        .right(Rs2AntibanSettings.naturalMouse ? "✓ ACTIVE" : "✗ OFF")
                        .rightColor(Rs2AntibanSettings.naturalMouse ? RS_GREEN : RS_GRAY)
                        .build());
            }

            // Move Mouse Off Screen
            if (config.moveMouseOffScreen()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Mouse Off-Screen:")
                        .right(Rs2AntibanSettings.moveMouseOffScreen ? "✓ ACTIVE" : "✗ OFF")
                        .rightColor(Rs2AntibanSettings.moveMouseOffScreen ? RS_GREEN : RS_GRAY)
                        .build());
            }

            // Move Mouse Randomly
            if (config.moveMouseRandomly()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Random Mouse:")
                        .right(String.format("%.0f%% chance", Rs2AntibanSettings.moveMouseRandomlyChance * 100))
                        .rightColor(RS_YELLOW)
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
                        .right(String.format("%.0f%%", Rs2AntibanSettings.actionCooldownChance * 100))
                        .rightColor(RS_GRAY)
                        .build());
            }

            // Micro Breaks
            if (config.microBreaks()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Micro Breaks:")
                        .right(String.format("%.1f%% chance", Rs2AntibanSettings.microBreakChance * 100))
                        .rightColor(RS_YELLOW)
                        .build());
            }

            // Simulate Fatigue
            if (config.simulateFatigue()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Fatigue:")
                        .right(Rs2AntibanSettings.simulateFatigue ? "✓ ACTIVE" : "✗ OFF")
                        .rightColor(Rs2AntibanSettings.simulateFatigue ? RS_ORANGE : RS_GRAY)
                        .build());
            }

            // Attention Span
            if (config.simulateAttentionSpan()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Attention Span:")
                        .right(Rs2AntibanSettings.simulateAttentionSpan ? "✓ ACTIVE" : "✗ OFF")
                        .rightColor(Rs2AntibanSettings.simulateAttentionSpan ? RS_GREEN : RS_GRAY)
                        .build());
            }

            // Behavioral Variability
            if (config.behavioralVariability()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Behavior Variation:")
                        .right(Rs2AntibanSettings.behavioralVariability ? "✓ ACTIVE" : "✗ OFF")
                        .rightColor(Rs2AntibanSettings.behavioralVariability ? RS_GREEN : RS_GRAY)
                        .build());
            }

            // Non-Linear Intervals
            if (config.nonLinearIntervals()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Non-Linear Timing:")
                        .right(Rs2AntibanSettings.nonLinearIntervals ? "✓ ACTIVE" : "✗ OFF")
                        .rightColor(Rs2AntibanSettings.nonLinearIntervals ? RS_GREEN : RS_GRAY)
                        .build());
            }

            // Profile Switching
            if (config.profileSwitching()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Profile Switching:")
                        .right(Rs2AntibanSettings.profileSwitching ? "✓ ACTIVE" : "✗ OFF")
                        .rightColor(Rs2AntibanSettings.profileSwitching ? RS_GREEN : RS_GRAY)
                        .build());
            }

            // Simulate Mistakes
            if (config.simulateMistakes()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Mistake Simulation:")
                        .right(Rs2AntibanSettings.simulateMistakes ? "✓ ACTIVE" : "✗ OFF")
                        .rightColor(Rs2AntibanSettings.simulateMistakes ? RS_ORANGE : RS_GRAY)
                        .build());
            }

            // Use Play Style
            if (config.usePlayStyle()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  Play Style:")
                        .right(Rs2AntibanSettings.usePlayStyle ? "✓ ACTIVE" : "✗ OFF")
                        .rightColor(Rs2AntibanSettings.usePlayStyle ? RS_GREEN : RS_GRAY)
                        .build());
            }

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
}

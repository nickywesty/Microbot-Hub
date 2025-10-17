package net.runelite.client.plugins.microbot.smartminer;

import net.runelite.api.Experience;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Skill;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.text.DecimalFormat;

public class SmartMinerOverlay extends OverlayPanel {
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
    private static final Color RS_DARK_ORANGE = new Color(255, 140, 0);

    // XP tracking
    private int startXp = -1;
    private long startTime = System.currentTimeMillis();

    @Inject
    SmartMinerOverlay(SmartMinerPlugin plugin, SmartMinerConfig config) {
        super(plugin);
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
        setPriority(OverlayPriority.HIGH);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            // Initialize XP tracking
            if (startXp == -1 && Microbot.getClient().getSkillExperience(Skill.MINING) > 0) {
                startXp = Microbot.getClient().getSkillExperience(Skill.MINING);
                startTime = System.currentTimeMillis();
            }

            // Info Panel
            panelComponent.setPreferredSize(new Dimension(280, 350));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("⛏ Smart Miner v" + SmartMinerPlugin.version + " ⛏")
                    .color(RS_ORANGE)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().build());

            // State with color coding
            Color stateColor = getStateColor(SmartMinerScript.currentState);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("State:")
                    .right(SmartMinerScript.currentState != null ? SmartMinerScript.currentState.toString() : "IDLE")
                    .rightColor(stateColor)
                    .build());

            // Mining Location
            if (config.usePresetLocation()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Location:")
                        .right(config.miningLocation().getName())
                        .rightColor(Color.WHITE)
                        .build());
            }

            // Radius
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Radius:")
                    .right(config.miningRadius() + " tiles")
                    .rightColor(RS_WHITE)
                    .build());

            // Banking
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Banking:")
                    .right(config.useBank() ? "Enabled" : "Disabled")
                    .rightColor(config.useBank() ? RS_GREEN : RS_RED)
                    .build());

            // Active ores count
            int activeOresCount = SmartMinerScript.getSelectedOreTypes(config).size();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Active Ores:")
                    .right(String.valueOf(activeOresCount))
                    .rightColor(RS_YELLOW)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().build());

            // === XP TRACKER SECTION ===
            if (config.showXpTracker()) {
                renderXpTracker();
            }

            // === SESSION STATS SECTION ===
            if (config.showSessionStats()) {
                renderSessionStats();
            }

            // Antiban Status
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Antiban:")
                    .right(getActiveAntibanCount(config) + "/12 Active")
                    .rightColor(RS_CYAN)
                    .build());

            // Render rock highlights
            renderRockHighlights(graphics);

        } catch (Exception ex) {
            Microbot.log("Error rendering Smart Miner overlay: " + ex.getMessage());
        }
        return super.render(graphics);
    }

    private int getActiveAntibanCount(SmartMinerConfig config) {
        int count = 0;
        if (config.naturalMouse()) count++;
        if (config.moveMouseOffScreen()) count++;
        if (config.moveMouseRandomly()) count++;
        if (config.actionCooldowns()) count++;
        if (config.microBreaks()) count++;
        if (config.simulateFatigue()) count++;
        if (config.simulateAttentionSpan()) count++;
        if (config.behavioralVariability()) count++;
        if (config.nonLinearIntervals()) count++;
        if (config.profileSwitching()) count++;
        if (config.simulateMistakes()) count++;
        if (config.usePlayStyle()) count++;
        return count;
    }

    private int getTotalAntibanCount() {
        return 12; // Total number of antiban options
    }

    private Color getStateColor(net.runelite.client.plugins.microbot.smartminer.enums.MiningState state) {
        if (state == null) return RS_WHITE;
        switch (state) {
            case MINING:
                return RS_GREEN;
            case BANKING:
            case WALKING_TO_BANK:
                return RS_CYAN;
            case DROPPING:
                return RS_YELLOW;
            case WAITING_FOR_RESPAWN:
                return RS_ORANGE;
            case STARTING:
                return RS_WHITE;
            default:
                return RS_GRAY;
        }
    }

    private void renderXpTracker() {
        if (startXp == -1) return;

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Mining XP")
                .color(RS_DARK_ORANGE)
                .build());

        int currentXp = Microbot.getClient().getSkillExperience(Skill.MINING);
        int currentLevel = Microbot.getClient().getRealSkillLevel(Skill.MINING);
        int xpGained = currentXp - startXp;

        // XP Gained
        panelComponent.getChildren().add(LineComponent.builder()
                .left("  XP Gained:")
                .right(decimalFormat.format(xpGained))
                .rightColor(RS_GREEN)
                .build());

        // XP per hour
        long runtime = System.currentTimeMillis() - startTime;
        if (runtime > 0) {
            long xpPerHour = (xpGained * 3600000L) / runtime;
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  XP/Hour:")
                    .right(decimalFormat.format(xpPerHour))
                    .rightColor(RS_YELLOW)
                    .build());
        }

        // Current level
        panelComponent.getChildren().add(LineComponent.builder()
                .left("  Level:")
                .right(String.valueOf(currentLevel))
                .rightColor(RS_CYAN)
                .build());

        // Progress to next level
        if (currentLevel < 99) {
            int xpForCurrentLevel = Experience.getXpForLevel(currentLevel);
            int xpForNextLevel = Experience.getXpForLevel(currentLevel + 1);
            int xpNeeded = xpForNextLevel - currentXp;
            double progress = ((double) (currentXp - xpForCurrentLevel) / (xpForNextLevel - xpForCurrentLevel)) * 100;

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  To Level " + (currentLevel + 1) + ":")
                    .right(decimalFormat.format(xpNeeded) + " XP")
                    .rightColor(RS_WHITE)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  Progress:")
                    .right(percentFormat.format(progress) + "%")
                    .rightColor(RS_GREEN)
                    .build());

            // Time to next level
            if (runtime > 0 && xpGained > 0) {
                long xpPerHour = (xpGained * 3600000L) / runtime;
                if (xpPerHour > 0) {
                    double hoursToLevel = (double) xpNeeded / xpPerHour;
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("  Time to Level:")
                            .right(formatTimeEstimate(hoursToLevel))
                            .rightColor(RS_ORANGE)
                            .build());
                }
            }
        }

        panelComponent.getChildren().add(LineComponent.builder().build());
    }

    private void renderSessionStats() {
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Session Stats")
                .color(RS_DARK_ORANGE)
                .build());

        // Runtime
        long runtime = System.currentTimeMillis() - startTime;
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

        // Bank trips
        panelComponent.getChildren().add(LineComponent.builder()
                .left("  Bank Trips:")
                .right(String.valueOf(SmartMinerScript.tripCount))
                .rightColor(RS_CYAN)
                .build());

        // Ores per hour
        if (runtime > 0) {
            long oresPerHour = (SmartMinerScript.oresMined * 3600000L) / runtime;
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  Ores/Hour:")
                    .right(decimalFormat.format(oresPerHour))
                    .rightColor(RS_GREEN)
                    .build());
        }

        panelComponent.getChildren().add(LineComponent.builder().build());
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

    private String formatTimeEstimate(double hours) {
        if (hours < 0.017) { // Less than 1 minute
            return "< 1m";
        } else if (hours < 1) {
            int minutes = (int) (hours * 60);
            return minutes + "m";
        } else if (hours < 24) {
            int h = (int) hours;
            int m = (int) ((hours - h) * 60);
            return h + "h " + m + "m";
        } else {
            int days = (int) (hours / 24);
            return days + "d";
        }
    }

    private static final Color RS_GRAY = new Color(127, 127, 127);

    private void renderRockHighlights(Graphics2D graphics) {
        if (Microbot.getClient().getLocalPlayer() == null) return;

        // Get rocks within radius - need script instance for non-static method
        SmartMinerScript script = plugin.getScript();
        if (script == null) return;

        var rocks = script.getMinableRocksInRadius(config);

        for (GameObject rock : rocks) {
            if (rock == null) continue;

            LocalPoint lp = rock.getLocalLocation();
            if (lp == null) continue;

            Polygon poly = Perspective.getCanvasTilePoly(Microbot.getClient(), lp);
            if (poly == null) continue;

            // Highlight the rock
            graphics.setColor(new Color(0, 255, 0, 50));
            graphics.fillPolygon(poly);
            graphics.setColor(Color.GREEN);
            graphics.setStroke(new BasicStroke(2));
            graphics.drawPolygon(poly);

            // Draw rock name
            String name = SmartMinerScript.getRockName(rock);
            int height = rock.getRenderable() != null ? rock.getRenderable().getModelHeight() / 2 : 0;
            net.runelite.api.Point textPoint = Perspective.getCanvasTextLocation(
                    Microbot.getClient(),
                    graphics,
                    lp,
                    name,
                    height);

            if (textPoint != null) {
                graphics.setColor(Color.WHITE);
                graphics.drawString(name, textPoint.getX(), textPoint.getY());
            }
        }
    }
}

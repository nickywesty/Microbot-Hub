package net.runelite.client.plugins.microbot.wildernessnicky;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.plugins.microbot.wildernessnicky.enums.WildernessProjectileType;
import javax.inject.Inject;
import java.awt.*;

public class WildernessNickyOverlay extends OverlayPanel {
    private boolean active = false;
    private WildernessNickyScript script;

    @Inject
    public WildernessNickyOverlay(WildernessNickyPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setScript(WildernessNickyScript script) {
        this.script = script;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!active || script == null) return null;

        // Dynamic height based on banked loot entries, projectile detection, and escape reason
        int baseHeight = 200;
        int bankedLootEntries = Math.min(5, script.getBankedLoot().size()); // Max 5 items shown
        int bankedLootHeight = bankedLootEntries > 0 ? (bankedLootEntries + 2) * 20 : 0; // +2 for section header and separator

        // Add height for projectile detection section (if active)
        int projectileHeight = 0;
        if (script.projectilesDetectedCount > 0) {
            projectileHeight = 80; // Base height for projectile section
            if (script.lastProjectileId > 0 && (System.currentTimeMillis() - script.lastProjectileDetectionTime < 30000)) {
                projectileHeight += 20; // Add line for "time ago"
            }
            if (script.incomingProjectiles.size() > 0) {
                projectileHeight += 20; // Add line for incoming projectiles
            }
        }

        // Add height for escape reason section (if active)
        int escapeReasonHeight = 0;
        if (script.getLastEscapeTime() > 0) {
            escapeReasonHeight = 60; // Base: header + time + separator
            // Add height based on reason length (up to 2 lines)
            String reason = script.getLastEscapeReason();
            if (reason.length() > 30) {
                escapeReasonHeight += 20; // Second line
            }
        }

        // Add height for looting bag contents (if present)
        int lootingBagHeight = 0;
        if (!script.getLootingBagContents().isEmpty()) {
            int itemsShown = Math.min(3, script.getLootingBagContents().size());
            lootingBagHeight = (itemsShown + 1) * 20; // +1 for "Bag Contents:" header
        }

        panelComponent.setPreferredSize(new Dimension(250, baseHeight + bankedLootHeight + projectileHeight + escapeReasonHeight + lootingBagHeight));

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("\uD83D\uDC2C Wilderness Agility v" + (script != null ? script.VERSION : "1.6.0") + " \uD83D\uDC2C")
                .color(new Color(0x00B4D8))
                .build());
        panelComponent.getChildren().add(LineComponent.builder().build());

        // ===== RUNTIME STATS =====
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Dispensers Looted")
                .right(Integer.toString(script.getDispenserLoots()))
                .leftColor(Color.YELLOW)
                .rightColor(Color.YELLOW)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Time Running")
                .right(script.getRunningTime())
                .leftColor(new Color(0xFFA726)) // light orange
                .rightColor(new Color(0xFFA726))
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Looting Bag Value")
                .right(String.format("%,d gp", script.getLootingBagValue()))
                .leftColor(new Color(0x2ECC40)) // money green
                .rightColor(new Color(0x2ECC40))
                .build());

        // Show looting bag contents if not empty
        if (!script.getLootingBagContents().isEmpty()) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  Bag Contents:")
                    .leftColor(new Color(0x90EE90)) // light green
                    .build());

            // Show top 3 items in looting bag
            script.getLootingBagContents().entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())) // Sort by quantity descending
                    .limit(3)
                    .forEach(entry -> {
                        panelComponent.getChildren().add(LineComponent.builder()
                                .left("    " + entry.getKey())
                                .right(String.format("%,d", entry.getValue()))
                                .leftColor(Color.LIGHT_GRAY)
                                .rightColor(Color.WHITE)
                                .build());
                    });
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Previous Lap Time")
                .right(script.getPreviousLapTime())
                .leftColor(Color.WHITE)
                .rightColor(Color.WHITE)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Fastest Lap Time")
                .right(script.getFastestLapTime())
                .leftColor(new Color(0xFFD600)) // race car yellow
                .rightColor(new Color(0xFFD600))
                .build());

        // ===== ESCAPE REASON SECTION =====
        // Show if an escape has been triggered
        if (script.getLastEscapeTime() > 0) {
            panelComponent.getChildren().add(LineComponent.builder().build()); // Separator

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("⚠️ Last Escape Reason")
                    .leftColor(new Color(0xFF6B6B)) // red
                    .build());

            // Split long escape reasons into multiple lines (max 30 chars per line)
            String reason = script.getLastEscapeReason();
            if (reason.length() <= 30) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left(reason)
                        .leftColor(Color.ORANGE)
                        .build());
            } else {
                // Split into two lines
                String line1 = reason.substring(0, Math.min(30, reason.length()));
                String line2 = reason.length() > 30 ? reason.substring(30) : "";

                panelComponent.getChildren().add(LineComponent.builder()
                        .left(line1)
                        .leftColor(Color.ORANGE)
                        .build());
                if (!line2.isEmpty()) {
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left(line2)
                            .leftColor(Color.ORANGE)
                            .build());
                }
            }

            // Show time since escape
            long timeSinceEscape = System.currentTimeMillis() - script.getLastEscapeTime();
            String timeAgo = String.format("%ds ago", timeSinceEscape / 1000);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Triggered")
                    .right(timeAgo)
                    .leftColor(Color.LIGHT_GRAY)
                    .rightColor(Color.YELLOW)
                    .build());
        }

        // ===== PROJECTILE DETECTION SECTION =====
        // Only show if projectile-based switching is enabled and projectiles have been detected
        if (script.projectilesDetectedCount > 0) {
            panelComponent.getChildren().add(LineComponent.builder().build()); // Separator

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("🎯 Projectile Defense")
                    .leftColor(new Color(0xFF6B6B)) // red-ish
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Projectiles Detected")
                    .right(Integer.toString(script.projectilesDetectedCount))
                    .leftColor(Color.ORANGE)
                    .rightColor(Color.ORANGE)
                    .build());

            // Show last detected projectile info
            if (script.lastProjectileId > 0) {
                String attackStyle = WildernessProjectileType.getAttackStyleName(script.lastProjectileId);
                long timeSinceLastProjectile = System.currentTimeMillis() - script.lastProjectileDetectionTime;

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Last Attack Style")
                        .right(attackStyle)
                        .leftColor(Color.CYAN)
                        .rightColor(getColorForAttackStyle(attackStyle))
                        .build());

                // Show "X seconds ago" if recent (within last 30 seconds)
                if (timeSinceLastProjectile < 30000) {
                    String timeAgo = String.format("%ds ago", timeSinceLastProjectile / 1000);
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Last Detected")
                            .right(timeAgo)
                            .leftColor(Color.LIGHT_GRAY)
                            .rightColor(Color.WHITE)
                            .build());
                }
            }

            // Show active incoming projectiles count
            int activeProjectiles = script.incomingProjectiles.size();
            if (activeProjectiles > 0) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("⚠ Incoming Attacks")
                        .right(Integer.toString(activeProjectiles))
                        .leftColor(new Color(0xFF0000)) // bright red
                        .rightColor(new Color(0xFF0000))
                        .build());
            }
        }

        // ===== BANKED LOOT SECTION =====
        if (script.getTotalBankingTrips() > 0) {
            panelComponent.getChildren().add(LineComponent.builder().build()); // Separator

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Total Banked")
                    .right(String.format("%,d gp", script.getTotalBankedValue()))
                    .leftColor(new Color(0x00FF00)) // bright green
                    .rightColor(new Color(0x00FF00))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Banking Trips")
                    .right(Integer.toString(script.getTotalBankingTrips()))
                    .leftColor(new Color(0xADD8E6)) // light blue
                    .rightColor(new Color(0xADD8E6))
                    .build());

            // Show top 5 banked items
            java.util.Map<String, Integer> topLoot = script.getTopBankedLoot(5);
            if (!topLoot.isEmpty()) {
                panelComponent.getChildren().add(LineComponent.builder().build()); // Separator
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("--- Top Banked Items ---")
                        .leftColor(Color.CYAN)
                        .build());

                for (java.util.Map.Entry<String, Integer> entry : topLoot.entrySet()) {
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left(entry.getKey())
                            .right(String.format("%,d", entry.getValue()))
                            .leftColor(new Color(0xC0C0C0)) // silver
                            .rightColor(Color.WHITE)
                            .build());
                }
            }
        }

        return super.render(graphics);
    }

    /**
     * Helper method to get color for attack style
     */
    private Color getColorForAttackStyle(String attackStyle) {
        switch (attackStyle) {
            case "Magic":
                return new Color(0x00BFFF); // cyan/blue for magic
            case "Ranged":
                return new Color(0x00FF00); // green for ranged
            case "Melee":
                return new Color(0xFF4500); // orange-red for melee
            default:
                return Color.WHITE;
        }
    }
} 
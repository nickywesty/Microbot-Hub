package net.runelite.client.plugins.microbot.smartminer;

import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class SmartMinerOverlay extends OverlayPanel {
    private final SmartMinerPlugin plugin;
    private final SmartMinerConfig config;

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
            // Info Panel
            panelComponent.setPreferredSize(new Dimension(250, 300));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Smart Miner v" + SmartMinerPlugin.version)
                    .color(Color.CYAN)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().build());

            // State
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("State:")
                    .right(SmartMinerScript.currentState != null ? SmartMinerScript.currentState.toString() : "IDLE")
                    .rightColor(Color.GREEN)
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
                    .rightColor(Color.WHITE)
                    .build());

            // Banking
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Banking:")
                    .right(config.useBank() ? "Enabled" : "Disabled")
                    .rightColor(config.useBank() ? Color.GREEN : Color.RED)
                    .build());

            // Active ores count
            int activeOresCount = SmartMinerScript.getSelectedOreTypes(config).size();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Active Ores:")
                    .right(String.valueOf(activeOresCount))
                    .rightColor(Color.YELLOW)
                    .build());

            // Render rock highlights
            renderRockHighlights(graphics);

        } catch (Exception ex) {
            Microbot.log("Error rendering Smart Miner overlay: " + ex.getMessage());
        }
        return super.render(graphics);
    }

    private void renderRockHighlights(Graphics2D graphics) {
        if (Microbot.getClient().getLocalPlayer() == null) return;

        // Get rocks within radius
        var rocks = SmartMinerScript.getMinableRocksInRadius(config);

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
            Point textPoint = Perspective.getCanvasTextLocation(
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

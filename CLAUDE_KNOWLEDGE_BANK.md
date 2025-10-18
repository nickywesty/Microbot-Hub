# Claude Knowledge Bank - Microbot Project

**Last Updated:** 2025-10-17
**Purpose:** Reference document for Claude AI sessions working on this Microbot project

---

## 📁 Project Structure

### Base Directory:
```
C:\Users\Elite\Desktop\Microbot Practice\Microbot-Hub\
```

### Working Directory:
```
/mnt/c/Users/Elite/Desktop/Microbot Practice/Microbot-Hub/
```

### Plugin Source Location:
```
src/main/java/net/runelite/client/plugins/microbot/
```

---

## 🔧 Build System

### Gradle Build Command:
```bash
cd "/mnt/c/Users/Elite/Desktop/Microbot Practice/Microbot-Hub"
bash gradlew build
```

**Note:** Use `bash gradlew` not `./gradlew` due to WSL line ending issues

---

## 📚 Microbot API Reference

### Player APIs

#### Health & Stats:
```java
// Health
Rs2Player.getBoostedSkillLevel(Skill.HITPOINTS)  // Current HP
Rs2Player.getRealSkillLevel(Skill.HITPOINTS)     // Max HP
Rs2Player.getHealthPercentage()                   // HP as percentage (0-100)

// Prayer
Rs2Player.getBoostedSkillLevel(Skill.PRAYER)     // Current prayer
Rs2Player.getRealSkillLevel(Skill.PRAYER)        // Max prayer
Rs2Player.hasPrayerPoints()                       // Boolean check

// Other Stats
Rs2Player.getBoostedSkillLevel(Skill.X)          // Any skill (boosted)
Rs2Player.getRealSkillLevel(Skill.X)             // Any skill (base)
```

#### Player Actions:
```java
Rs2Player.logout()                                // Logout
Rs2Player.isMoving()                             // Check if moving
Rs2Player.isAnimating()                          // Check if animating
Rs2Player.isInteracting()                        // Check if interacting
Rs2Player.getWorldLocation()                     // WorldPoint location
Rs2Player.distanceTo(WorldPoint point)           // Distance to point
Rs2Player.isMember()                             // Check membership
Rs2Player.getWorld()                             // Current world number

// Food & Potions
Rs2Player.eatAt(int hp)                          // Eat at HP threshold
Rs2Player.drinkPrayerPotion()                    // Drink prayer pot
Rs2Player.drinkCombatPotionAt(int boost)         // Drink combat pot
```

### Combat APIs

```java
Rs2Combat.inCombat()                             // Boolean - in combat
Rs2Combat.getSpecEnergy()                        // Special attack % (0-100)
Rs2Combat.setSpecState(boolean enabled, int clicks) // Enable spec (clicks for multi-spec)
```

### Prayer APIs

```java
Rs2Prayer.toggle(Rs2PrayerEnum prayer, boolean enable)  // Toggle specific prayer
Rs2Prayer.isPrayerActive(Rs2PrayerEnum prayer)          // Check if active
Rs2Prayer.toggleQuickPrayer(boolean enable)             // Toggle quick prayers
Rs2Prayer.isQuickPrayerEnabled()                        // Check quick prayer

// Prayer Enums
Rs2PrayerEnum.PROTECT_MELEE
Rs2PrayerEnum.PROTECT_RANGE
Rs2PrayerEnum.PROTECT_MAGIC
Rs2PrayerEnum.PIETY
Rs2PrayerEnum.RIGOUR
Rs2PrayerEnum.EAGLE_EYE
Rs2PrayerEnum.HAWK_EYE
```

### Inventory APIs

```java
Rs2Inventory.contains(String itemName)           // Check has item
Rs2Inventory.contains(int itemId)                // Check has item by ID
Rs2Inventory.count(String itemName)              // Count items
Rs2Inventory.count()                             // Count all items (used slots)
Rs2Inventory.isFull()                            // Check if full
Rs2Inventory.wield(String itemName)              // Equip item
Rs2Inventory.wield(int itemId)                   // Equip item by ID
Rs2Inventory.interact(String itemName, String action)  // Use item
Rs2Inventory.interact(int itemId, String action)       // Use item by ID
Rs2Inventory.drop(String itemName)               // Drop item
Rs2Inventory.dropAll(String itemName)            // Drop all matching
```

### Equipment APIs

```java
Rs2Equipment.isWearing(String itemName)          // Check wearing (String)
Rs2Equipment.isWearing(int itemId)               // Check wearing (int ID)
```

### Banking APIs

```java
Rs2Bank.openBank()                               // Open nearest bank
Rs2Bank.isOpen()                                 // Check if bank open
Rs2Bank.closeBank()                              // Close bank
Rs2Bank.depositAll(String itemName)              // Deposit all of item
Rs2Bank.withdrawX(String itemName, int amount)   // Withdraw X amount
Rs2Bank.withdrawX(int itemId, int amount)        // Withdraw X by ID
Rs2Bank.withdrawAll(String itemName)             // Withdraw all
Rs2Bank.withdrawItem(boolean exact, String itemName) // Withdraw 1
Rs2Bank.walkToBank(BankLocation location)        // Walk to specific bank
```

### GameObject APIs

```java
Rs2GameObject.findObject(String name)                    // Find object
Rs2GameObject.findObjectById(int id)                     // Find by ID
Rs2GameObject.interact(String objectName, String action) // Interact
Rs2GameObject.interact(int objectId, String action)      // Interact by ID
Rs2GameObject.interact(TileObject object)                // Interact with object
Rs2GameObject.interact(TileObject object, String action) // Interact with action
Rs2GameObject.getGameObject(int id)                      // Get game object
```

### Walker APIs

```java
Rs2Walker.walkTo(WorldPoint point)               // Walk to point
Rs2Walker.walkTo(WorldPoint point, int radius)   // Walk to point with radius
Rs2Walker.setTarget(WorldPoint point)            // Set walk target (non-blocking)
```

### Camera APIs

```java
Rs2Camera.setYaw(int yaw)                        // Set camera yaw (0-2047)
Rs2Camera.resetPitch()                           // Reset camera pitch
Rs2Camera.resetZoom()                            // Reset camera zoom
Rs2Camera.turnTo(LocalPoint point)               // Turn to point
Rs2Camera.turnTo(LocalPoint point, int deviation) // Turn to point with deviation
```

### Mouse APIs

```java
// Click Methods
Microbot.getMouse().click(Point point)           // Click at Point ✅
Microbot.getMouse().click(Rectangle rect)        // Click in Rectangle ✅
Microbot.getMouse().click()                      // Click at current position

// Player Clicking (convert Polygon to Point)
java.awt.Rectangle bounds = player.getCanvasTilePoly().getBounds();
java.awt.Point center = new java.awt.Point(bounds.x + bounds.width/2, bounds.y + bounds.height/2);
Microbot.getMouse().click(center);
```

### Widget APIs

```java
Rs2Widget.hasWidget(String text)                 // Check for widget with text
Rs2Widget.sleepUntilHasWidget(String text)       // Wait for widget
Microbot.getClient().getWidget(int groupId, int childId) // Get widget
```

### Antiban APIs

```java
Rs2Antiban.actionCooldown()                      // Cooldown action
Rs2Antiban.takeMicroBreakByChance()              // Random micro break
Rs2Antiban.resetAntibanSettings()                // Reset settings

// Antiban Settings
Rs2AntibanSettings.naturalMouse = true           // Enable natural mouse
Rs2AntibanSettings.usePlayStyle = true           // Use play style
Rs2AntibanSettings.actionCooldownActive          // Check if cooldown active
```

### World Hopping APIs

```java
Login.getRandomWorld(boolean members)            // Get random world
Microbot.hopToWorld(int worldNumber)             // Hop to world
Microbot.getClient().openWorldHopper()           // Open world hopper
```

---

## 🎯 Current Active Projects

### 1. Auto Bounty Hunter PvP Plugin ✅ COMPLETE
**Location:** `src/main/java/net/runelite/client/plugins/microbot/autobountyhunter/`

**Status:** All compilation errors fixed, ready to test

**Key Files:**
- AutoBountyHunterPlugin.java
- AutoBountyHunterScript.java
- AutoBountyHunterConfig.java
- AutoBountyHunterOverlay.java
- BountyHunterState.java

**Combat Strategy:**
- Range (Magic Shortbow) → detect high hits → granite maul double spec
- Automatic prayer switching
- Health/prayer management
- Safe zone retreat when low HP

**Known Issues Fixed:**
- ✅ StatChanged import
- ✅ Rs2Player.getHealth() → getBoostedSkillLevel(Skill.HITPOINTS)
- ✅ Player attack using polygon center Point
- ✅ Banking using Rs2Bank.withdrawX()

### 2. Smart Miner Plugin ✅ COMPLETE
**Location:** `src/main/java/net/runelite/client/plugins/microbot/smartminer/`

**Features:**
- WebWalker integration
- Preset mining locations
- Cluster detection (3+ ores preferred, then 2, then 1)
- Camera scanning for best ore spots
- Player detection with world hopping
- Displacement detection (returns if teleported)
- Antiban integration

**Key Files:**
- SmartMinerPlugin.java
- SmartMinerScript.java
- SmartMinerConfig.java
- SmartMinerOverlay.java
- MiningState.java
- OreType.java (enum)

### 3. Wilderness Nicky (Wilderness Agility Enhanced) 🔄 IN PROGRESS
**Location:** `src/main/java/net/runelite/client/plugins/microbot/wildernessnicky/`

**Original Plugin:** WildernessAgility (copied from wildernessagility/)

**Key Files:**
- WildernessNickyPlugin.java
- WildernessNickyScript.java
- WildernessNickyConfig.java
- WildernessNickyOverlay.java
- WildernessNickyItems.java
- WildernessNickyObstacleModel.java
- ANTI_PK_ANALYSIS.md (comprehensive analysis)

**Current Anti-PK System:**
- Phoenix necklace detection (reactive - after death)
- Health percentage monitoring (proactive)
- Player Monitor plugin integration
- 4-step emergency escape sequence
- Walk to Mage Bank → logout

**Identified Issues:**
- ❌ Death walking incomplete (no respawn detection)
- ❌ No proactive player scanning
- ❌ Navigation can fail silently
- ❌ Too aggressive (logs out on accidents)

**Planned Improvements:**
1. Proactive player detection (scan before attack)
2. Proper death walking (respawn → gear → return)
3. Smarter escape logic (differentiate PKer vs accident)
4. Better navigation (waypoints, retries)
5. Configurable aggression levels

---

## 🐛 Common Compilation Errors & Fixes

### Error 1: cannot find symbol - method getHealth()
**Fix:**
```java
// WRONG:
int hp = Rs2Player.getHealth();

// CORRECT:
int hp = Rs2Player.getBoostedSkillLevel(Skill.HITPOINTS);
```

### Error 2: Player cannot be converted to Rs2PlayerModel
**Fix:**
```java
// WRONG:
Rs2Player.attack(player);

// CORRECT:
java.awt.Rectangle bounds = player.getCanvasTilePoly().getBounds();
java.awt.Point center = new java.awt.Point(bounds.x + bounds.width/2, bounds.y + bounds.height/2);
Microbot.getMouse().click(center);
```

### Error 3: no suitable method found for click(Polygon)
**Fix:**
```java
// WRONG:
Microbot.getMouse().click(player.getCanvasTilePoly());

// CORRECT (convert to Point):
java.awt.Rectangle bounds = polygon.getBounds();
java.awt.Point center = new java.awt.Point(bounds.x + bounds.width/2, bounds.y + bounds.height/2);
Microbot.getMouse().click(center);
```

### Error 4: cannot find symbol - method withdraw()
**Fix:**
```java
// WRONG:
Rs2Inventory.withdraw(itemName, amount);

// CORRECT:
Rs2Bank.withdrawX(itemName, amount);
```

### Error 5: Missing imports
**Common Missing Imports:**
```java
import net.runelite.api.events.StatChanged;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.api.Skill;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
```

---

## 📝 Code Patterns & Best Practices

### Plugin Structure:
```
pluginname/
├── PluginNamePlugin.java      // Main plugin class
├── PluginNameScript.java      // Main script with game loop
├── PluginNameConfig.java      // Configuration interface
├── PluginNameOverlay.java     // Visual overlay
├── PluginNameState.java       // State enum (if using state machine)
└── README.md                   // Documentation
```

### State Machine Pattern:
```java
public enum BotState {
    IDLE,
    BANKING,
    WALKING,
    MINING,
    // etc.
}

private BotState currentState = BotState.IDLE;

// In main loop:
switch (currentState) {
    case IDLE:
        handleIdle();
        break;
    case BANKING:
        handleBanking();
        break;
    // etc.
}
```

### Main Loop Structure:
```java
@Override
public boolean run(ConfigClass config) {
    this.config = config;

    mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
        try {
            if (!Microbot.isLoggedIn()) return;

            // Your logic here

        } catch (Exception e) {
            log.error("Error: ", e);
        }
    }, 0, 600, TimeUnit.MILLISECONDS); // 600ms = 1 game tick

    return true;
}
```

### Overlay Pattern:
```java
@Override
public Dimension render(Graphics2D graphics) {
    try {
        panelComponent.getChildren().clear();

        // Title
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("Plugin Name")
            .color(Color.CYAN)
            .build());

        // Status lines
        panelComponent.getChildren().add(LineComponent.builder()
            .left("Status:")
            .right(statusMessage)
            .build());

    } catch (Exception e) {
        System.out.println("Error in overlay: " + e.getMessage());
    }

    return super.render(graphics);
}
```

---

## 🎮 User Preferences

### Combat Plugins:
- User wants **rule-based approach** not ML/AI
- Prefers configurable settings over hardcoded
- Values transparency (can see what bot is doing)
- Focus on: PvP, wilderness survival, anti-PK

### Plugin Features User Likes:
- ✅ Antiban integration
- ✅ State overlays (visual feedback)
- ✅ World hopping
- ✅ Smart detection (clusters, players, etc.)
- ✅ Automatic recovery (displacement, death, etc.)
- ✅ Comprehensive configuration options

### Development Style:
- Start with analysis/planning
- Document thoroughly
- Iterative improvements
- Test compilation frequently
- Use TodoWrite to track progress

---

## 🗂️ Important File Locations

### Microbot API Documentation:
**User mentioned:** "Microbot has the documentation in the other folder with all the api info"
**Likely location:** Check project root or docs/ folder

### Gradle Files:
- `build.gradle` - Main build config
- `gradle/wrapper/gradle-wrapper.properties` - Gradle version
- `gradlew` / `gradlew.bat` - Gradle wrapper scripts

---

## 💡 Tips for New Claude Sessions

1. **Always read this file first** when starting a new session
2. **Check current working directory** - should be in Microbot-Hub
3. **Review active projects** section to see what's in progress
4. **Use Microbot API Reference** - don't guess method names
5. **Follow code patterns** - use established structures
6. **Test compilation** after changes
7. **Update this knowledge bank** when adding new info

---

## 📊 Session History Summary

### Session 1 (2025-10-17):
- Created Auto Bounty Hunter PvP plugin
- Fixed all compilation errors
- Created Smart Miner enhancements
- Analyzed Wilderness Agility plugin
- Created Wilderness Nicky copy
- Documented anti-PK system
- Created this knowledge bank

---

**End of Knowledge Bank**
**Always update this file with new learnings!**

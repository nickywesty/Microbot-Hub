# Refined Wilderness Agility Plugin - Complete Refactoring Plan

## Analysis of Current Code (4,233 lines)

### Current Issues Identified:
1. **Overly complex**: 144+ methods, many redundant
2. **Poor organization**: Related functionality scattered
3. **Duplicate logic**: Multiple detection methods doing similar things
4. **Unnecessary states**: Debug states, unused recovery states
5. **Complex banking value tracking**: Calculating from multiple sources instead of simple addition
6. **Lack of documentation**: Minimal JavaDoc
7. **Poor separation of concerns**: Everything in one massive script file

## Refined Plugin Structure (Target: ~2,000 lines total)

### **File 1: RefinedWildernessAgilityPlugin.java** (~100 lines)
- Main plugin entry point
- Event subscriptions (ChatMessage, ItemContainerChanged, ProjectileMoved)
- Overlay management
- Clean startup/shutdown

### **File 2: RefinedWildernessAgilityConfig.java** (~250 lines)
- Play Mode enum (Solo/Mass/Proactive) - KEEP
- Clean config sections with emojis - KEEP
- Remove redundant options
- Comprehensive descriptions

### **File 3: RefinedWildernessAgilityScript.java** (~1,200 lines)
Core script with clean organization:

**A. Constants & Setup** (~100 lines)
- Item IDs, object IDs, coordinates
- Regex patterns
- Configuration

**B. State Management** (~100 lines)
- Simplified states: INIT, OBSTACLES (5x), DISPENSER, BANKING, ESCAPE
- Remove: DEBUG, PIT_RECOVERY, WORLD_HOP states

**C. Core Agility Loop** (~300 lines)
- Clean obstacle traversal
- Dispenser interaction
- Entrance fee management (KEEP - working well)
- Incomplete lap detection (KEEP)

**D. Anti-PK System** (~400 lines)
- Play mode detection (Solo/Mass/Proactive)
- Projectile prayer switching (KEEP - works great)
- Emergency escape with logout priority (KEEP)
- World hopping for solo mode (KEEP)
- Safe zone detection (KEEP)

**E. Banking System** (~200 lines)
- Mage Bank chest detection (KEEP)
- Simple withdrawal logic
- **SIMPLIFIED**: totalBankedValue += lootingBagValue (each trip)
- Phoenix necklace withdrawal (KEEP)

**F. Helper Methods** (~100 lines)
- isAt(), getWildernessLevel()
- eatFood(), hasPhoenixNecklace()
- Cleanup redundant methods

### **File 4: RefinedWildernessAgilityOverlay.java** (~200 lines)
- Clean GUI with looting bag contents (KEEP - "5x Item" format)
- Escape reason display (KEEP)
- Projectile detection stats (KEEP)
- Banking trip stats (SIMPLIFIED)

### **File 5: enums/ProjectileType.java** (~240 lines)
- KEEP AS-IS - comprehensive and well-documented

### **File 6: models/ObstacleModel.java** (~15 lines)
- KEEP AS-IS - simple and clean

### **File 7: models/WildernessItems.java** (~50 lines)
- KEEP AS-IS - handles item name mapping

## Key Improvements

### 1. **Simplified Banking Value Tracking**
```java
// OLD: Complex tracking from inventory + bag + calculations
// NEW: Simple addition
private long totalBankedValue = 0;

private void handleBanking() {
    // ... deposit logic ...
    totalBankedValue += lootingBagValue; // SIMPLE!
    totalBankingTrips++;
    lootingBagValue = 0;
}
```

### 2. **Consolidated Escape Logic**
```java
// ONE method instead of 5 scattered methods
private void handleEmergencyEscape() {
    // 1. SPAM logout attempts
    // 2. Activate prayers immediately
    // 3. Eat food
    // 4. World hop if logout succeeds (solo mode)
    // 5. Run to Mage Bank if timeout
}
```

### 3. **Clean State Machine**
```java
enum State {
    INIT,           // Startup checks
    PIPE,           // Obstacle 1
    ROPE,           // Obstacle 2
    STONES,         // Obstacle 3
    LOG,            // Obstacle 4
    ROCKS,          // Obstacle 5
    DISPENSER,      // Tag dispenser
    BANKING,        // Bank & regear
    ESCAPE          // Emergency escape
}
```

### 4. **Better Code Organization**
```java
// Group related methods
//===================
// AGILITY METHODS
//===================
private void handleObstacle() { }
private void handleDispenser() { }

//===================
// ANTI-PK METHODS
//===================
private void detectThreats() { }
private void handleEscape() { }

//===================
// BANKING METHODS
//===================
private void handleBanking() { }
private void withdrawItems() { }
```

### 5. **Comprehensive JavaDoc**
```java
/**
 * Handles emergency escape from PKers with aggressive logout attempts.
 *
 * Flow:
 * 1. Spam Rs2Player.logout() every 100ms
 * 2. If logout fails → Activate protection prayers
 * 3. Eat food if HP < 60%
 * 4. After 3s timeout → Physical escape to Mage Bank
 * 5. If solo mode + logout succeeds → World hop (3 attempts)
 *
 * @see #handleProjectilePrayerSwitching() for prayer logic
 * @see #isInSafeZone() for safe zone detection
 */
private void handleEmergencyEscape() { }
```

## What Gets REMOVED

### Redundant Methods:
- `detectSkulledThreat()` - replaced by `detectAttackablePlayer()`
- `checkLootingBagOnStartup()` - causes corruption
- `waitForInventoryChanges()` - unused
- `generateRandomDropLocation()` - not needed
- Multiple retry counters - simplified to essential only

### Unused States:
- `DEBUG` states
- `PIT_RECOVERY` - rarely needed
- `WORLD_HOP_1/2` - consolidated into escape
- `INTERACT_LEVER` - web walking handles this

### Duplicate Tracking:
- `bankedLoot` HashMap - not needed, just track total value
- Multiple XP tracking variables - keep only essential
- Redundant failsafe counters

## Testing Strategy

1. **Compile test** - Ensure no errors
2. **Basic agility** - Run 5 laps, verify obstacles work
3. **Banking** - Force bank, verify Mage Bank chest opens
4. **Solo mode** - Verify logout on player detection
5. **Mass mode** - Verify hit detection thresholds
6. **Projectile switching** - Verify prayers activate
7. **Emergency escape** - Verify logout spam + world hop
8. **Entrance fee** - Verify zero-value detection

## Estimated Line Counts
- Plugin: 100 lines
- Config: 250 lines
- Script: 1,200 lines
- Overlay: 200 lines
- Enums: 240 lines
- Models: 65 lines
**Total: ~2,055 lines** (51% reduction from 4,233)

## Files to Create
1. `/refinedwildernessagility/RefinedWildernessAgilityPlugin.java`
2. `/refinedwildernessagility/RefinedWildernessAgilityConfig.java`
3. `/refinedwildernessagility/RefinedWildernessAgilityScript.java`
4. `/refinedwildernessagility/RefinedWildernessAgilityOverlay.java`
5. `/refinedwildernessagility/enums/ProjectileType.java` (copy)
6. `/refinedwildernessagility/models/ObstacleModel.java` (copy)
7. `/refinedwildernessagility/models/WildernessItems.java` (copy)

## Implementation Notes

### Core Features to Preserve:
✅ Play mode dropdown (Solo/Mass/Proactive)
✅ Projectile-based prayer switching (1-tick accuracy)
✅ Emergency escape with logout priority
✅ World hopping in solo mode (3 attempts)
✅ Mage Bank chest detection
✅ Entrance fee validation (zero-value detection)
✅ Looting bag sync ("Check" option every 3-7 laps)
✅ Safe zone detection (Mage Bank, non-wilderness)
✅ Clan vs non-clan hit detection
✅ Incomplete lap detection

### Simplifications:
- Banking value = simple addition per trip
- One escape method (not scattered)
- Fewer states (9 instead of 20+)
- Consolidated threat detection
- Minimal retry counters (only essential)

### Code Quality:
- JavaDoc on all public/key methods
- Grouped method organization
- Clear variable naming
- Constants properly defined
- No magic numbers

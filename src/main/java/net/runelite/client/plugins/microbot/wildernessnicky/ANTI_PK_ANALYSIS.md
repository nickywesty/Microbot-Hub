# Wilderness Nicky - Anti-PK & Death Walking Analysis

## Overview

This document analyzes the anti-PK (Player Killer) detection and emergency escape mechanics in the original WildernessAgility plugin, now copied as WildernessNicky for enhancements.

---

## Anti-PK System Analysis

### 1. **Player Detection Methods**

The plugin uses **INDIRECT** player detection through these triggers:

#### A. Phoenix Necklace Detection
**Location:** `triggerPhoenixEscape()` (Line ~1397)

**How it works:**
```java
if (config.phoenixEscape() && !emergencyEscapeTriggered) {
    if (!hasPhoenixNecklace()) {
        Microbot.log("Phoenix necklace missing - triggering escape!");
        triggerPhoenixEscape();
    }
}
```

**Logic:**
- Phoenix necklace teleports you when HP reaches 0 in wilderness
- If necklace disappears from equipment = you died = PKer killed you
- **Smart**: Doesn't detect PKers directly, detects consequence of death
- **Weakness**: Only triggers AFTER you die once

#### B. Health Percentage Monitoring
**Location:** Health check (Line ~301)

**How it works:**
```java
if (config.leaveAtHealthPercent() > 0 && !emergencyEscapeTriggered) {
    if (Rs2Player.getHealthPercentage() <= config.leaveAtHealthPercent()) {
        Microbot.log("Health dropped to " + (int)Rs2Player.getHealthPercentage() + "% - triggering emergency escape!");
        triggerPhoenixEscape();
    }
}
```

**Logic:**
- Monitors HP percentage every game tick
- If HP < threshold (configurable, e.g., 40%) → escape
- **Smart**: Preemptive - escapes before death
- **Strength**: Works even if PKer hasn't attacked yet (could be low from agility fails)

#### C. Player Monitor Plugin Integration
**Location:** `triggerPhoenixEscape()` (Line ~1414)

**How it works:**
```java
if (config.enablePlayerMonitor()) {
    Plugin playerMonitor = Microbot.getPluginManager().getPlugins().stream()
        .filter(x -> x.getClass().getName().equals("net.runelite.client.plugins.microbot.playermonitor.PlayerMonitorPlugin"))
        .findFirst()
        .orElse(null);
    if (playerMonitor != null) {
        Microbot.startPlugin(playerMonitor);
        Microbot.log("Player Monitor enabled for Emergency Escape");
    }
}
```

**Logic:**
- Activates separate PlayerMonitor plugin during escape
- PlayerMonitor likely detects nearby players and can trigger logout
- **Benefit**: Adds layer of protection during escape route
- **Note**: PlayerMonitor plugin not included in this codebase (external dependency)

---

### 2. **Emergency Escape Sequence**

When triggered, the escape follows this **4-step process**:

#### Step 1: Equip Phoenix Necklace (if available)
```java
if (!hasEquippedPhoenixNecklace && Rs2Inventory.hasItem("Phoenix necklace")) {
    while (!Rs2Equipment.isWearing("Phoenix necklace")) {
        Rs2Inventory.wield("Phoenix necklace");
        sleepUntil(() -> Rs2Equipment.isWearing("Phoenix necklace"), 600);
    }
    hasEquippedPhoenixNecklace = true;
    return;
}
```

**Purpose:**
- If you died and respawned, phoenix necklace might be in inventory
- Re-equip it for protection during escape
- **Smart**: Uses while loop to ensure it equips

#### Step 2: Navigate to Gate Area or Climb Rocks
```java
if (!hasClimbedRocks) {
    WorldArea rockArea = new WorldArea(SOUTH_WEST_CORNER, ROCK_AREA_WIDTH, ROCK_AREA_HEIGHT);
    boolean isInArea = rockArea.contains(Rs2Player.getWorldLocation());

    if (isInArea) {
        Microbot.log("Climbing rocks");
        Rs2GameObject.interact(ROCKS_OBJECT_ID, "Climb");
        sleep(1200);
        hasClimbedRocks = true;
        return;
    } else {
        Microbot.log("Walking to gate area");
        Rs2Walker.walkTo(GATE_AREA, 4);
        return;
    }
}
```

**Purpose:**
- If you're in the agility course area (rocks), climb out
- Otherwise, walk to the gate area
- **Smart**: Uses WorldArea check to determine location
- **Timeout Protection**: 10-second timeout per step

#### Step 3: Open Gate
```java
if (!hasOpenedGate) {
    Microbot.log("Opening gate");
    sleepUntilOnClientThread(() -> Rs2GameObject.getGameObject(GATE_OBJECT_ID) != null);
    Rs2GameObject.interact(GATE_OBJECT_ID, "Open");
    hasOpenedGate = true;
    return;
}
```

**Purpose:**
- Open wilderness gate to escape
- **Smart**: Waits for gate object to be visible on client thread

#### Step 4: Walk to Mage Bank & Logout
```java
Microbot.log("Walking to Mage Bank");
Rs2Walker.walkTo(new WorldPoint(2534, 4712, 0), 20); // Mage Bank

WorldPoint currentLoc = Rs2Player.getWorldLocation();
if (currentLoc != null && currentLoc.distanceTo(new WorldPoint(2534, 4712, 0)) <= 10) {
    Microbot.log("Successfully reached Mage Bank - logging out");
    while (Microbot.isLoggedIn()) {
        Rs2Player.logout();
        sleepUntil(() -> !Microbot.isLoggedIn(), 300);
    }
    // Reset escape state
    emergencyEscapeTriggered = false;
    ...
}
```

**Purpose:**
- Walk to Mage Bank (safe area inside wilderness)
- Force logout when reached
- **Smart**: While loop ensures logout succeeds
- **Benefit**: Mage Bank is a safe teleport destination

---

### 3. **Timeout & Safety Mechanisms**

#### Global Escape Timeout
```java
private static final long EMERGENCY_ESCAPE_TIMEOUT = 180000; // 3 minutes

if (timeSinceStart > EMERGENCY_ESCAPE_TIMEOUT) {
    Microbot.log("Emergency Escape timed out - logging out");
    Rs2Player.logout();
    return;
}
```

**Purpose:**
- If escape takes > 3 minutes, force logout
- **Protection**: Prevents infinite escape loops

#### Step-Level Timeout
```java
private static final long ESCAPE_STEP_TIMEOUT = 10000; // 10 seconds per step

if (stepElapsedTime > ESCAPE_STEP_TIMEOUT) {
    Microbot.log("Emergency Escape Step 2: Timeout - forcing to next step");
    hasClimbedRocks = true;
    return;
}
```

**Purpose:**
- If stuck on a step for > 10 seconds, skip to next step
- **Protection**: Prevents getting stuck on one step

---

## Death Walking Analysis

### 1. **Death Detection**

```java
// Death tracking
private boolean deathDetected = false;

// In main loop:
if (Rs2Player.getHealthPercentage() <= 0 || deathDetected) {
    // Death handling logic
    ...
}
```

**How it works:**
- Monitors HP percentage
- If HP <= 0, death is detected
- **Issue**: `deathDetected` flag is declared but never set to true anywhere in code!
- **Potential Bug**: Death detection may not work properly

### 2. **Respawn Handling**

**Current Status:** NOT IMPLEMENTED

**Problems:**
- No explicit respawn location detection
- No "walk back to course" logic after death
- Plugin relies on emergency escape, which logs out

### 3. **Banking After Death**

**Location:** BANKING state (not shown in escape logic)

The plugin has a BANKING state but it's not triggered after death respawn. Banking is only used for:
- Restocking food
- Depositing loot

**Issue**: No automatic return to course after banking

---

## Identified Issues & Improvements Needed

### Issues:

1. **Death Detection Incomplete**
   - `deathDetected` flag never set
   - No respawn location detection
   - No automatic return to course

2. **Emergency Escape Too Aggressive**
   - Triggers on low HP even from agility fails
   - Logs out immediately (no attempt to continue)
   - Could be smarter about differentiating PKer vs. accident

3. **Navigation Problems**
   - No stuck detection during escape
   - Web walking timeout may not be sufficient
   - Gate/rocks navigation can fail silently

4. **Player Detection Reactive, Not Proactive**
   - Only detects after phoenix necklace used (after death)
   - Health check is better but still reactive
   - No proactive player scanning in area

### Proposed Improvements:

1. ✅ **Add Proactive Player Detection**
   - Scan for players near course every few seconds
   - Check combat levels within attack range
   - Trigger escape BEFORE being attacked

2. ✅ **Improve Death Walking**
   - Detect respawn location
   - Auto-walk back to wilderness course
   - Re-equip gear from bank
   - Resume agility training

3. ✅ **Smarter Escape Logic**
   - Differentiate between PKer and agility fail
   - Add option to continue if HP recoverable
   - Only escape if real threat detected

4. ✅ **Better Navigation**
   - Add waypoint system for escape route
   - Improve stuck detection
   - Retry failed interactions

5. ✅ **Enhanced Logging & Debugging**
   - More detailed state logging
   - Track escape success rate
   - Log PKer encounters

---

## Current Anti-PK System Summary

**Strengths:**
- ✅ Robust 4-step escape sequence
- ✅ Multiple timeout protections
- ✅ PlayerMonitor integration
- ✅ Persistent logout attempts

**Weaknesses:**
- ❌ Reactive, not proactive
- ❌ Death walking incomplete
- ❌ Navigation can fail
- ❌ No player scanning
- ❌ Too aggressive (logs out on accidents)

---

## Next Steps for WildernessNicky

1. Document current implementation (this file)
2. Add proactive player scanning
3. Implement proper death walking
4. Improve navigation reliability
5. Add configurable aggression levels
6. Test and refine

---

**Status:** Analysis Complete ✅
**Next:** Implement improvements in WildernessNicky

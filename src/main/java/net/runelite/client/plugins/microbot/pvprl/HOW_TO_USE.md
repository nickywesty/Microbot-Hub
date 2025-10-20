# 🤖 PVP RL Bot - Quick Start Guide

**AI-Powered PvP Bot for Old School RuneScape**

This plugin uses trained reinforcement learning models to automatically play PvP in OSRS. The AI was trained through 1000s of simulated fights and can handle NH-style combat including prayer switching, gear switches, eating, and special attacks.

---

## ⚡ Quick Start (3 Steps)

### Step 1: Start the API Server

**Double-click this file:**
```
C:\Users\Elite\Desktop\Microbot Practice\START_PVP_RL.bat
```

A black window will open showing:
```
========================================
  API Server is now starting...
========================================

The API will listen on: http://127.0.0.1:8888
```

**Keep this window open!** (Don't close it or the bot won't work)

---

### Step 2: Configure the Plugin

1. **Launch Microbot-Hub**
2. **Find the PVP RL plugin** in the plugins list
3. **Enable it** (click the checkbox)
4. **Click the gear icon** to configure:

   **Required Settings:**
   - ✅ API Host: `127.0.0.1`
   - ✅ API Port: `8888`
   - ✅ Model Name: `FineTunedNh`

   **Recommended Settings:**
   - ✅ Enable Prayer Switching: ON
   - ✅ Enable Eating: ON
   - ✅ Enable Attacking: ON
   - ✅ Emergency Logout HP: `20`
   - ✅ Show Debug Overlay: ON

5. **Click "Apply"** to save

---

### Step 3: Start Fighting!

1. **Enter a PvP area:**
   - LMS (recommended for testing)
   - PvP worlds
   - Wilderness (at your own risk!)

2. **Get your gear:**
   - NH setup with food, potions, prayers unlocked
   - Or use LMS free gear

3. **Find an opponent** (target them)

4. **Look for the PVP RL panel** in Microbot sidebar

5. **Click "Start Bot"**

6. **Watch the magic happen!** 🎯
   - AI will start switching prayers
   - Eating when low HP
   - Attacking your target
   - Making combat decisions

---

## 📊 Understanding the Overlay

When the bot is running, you'll see an overlay in the top-left corner:

```
┌─────────────────────┐
│ PVP RL Bot          │ ← Green = Running
├─────────────────────┤
│ API: CONNECTED      │ ← Green = Good
│ Model: FineTunedNh  │
│ Latency: 45ms       │ ← Lower is better
├─────────────────────┤
│ HP: 85/99 (86%)     │ ← Your stats
│ Prayer: 70/99 (71%) │
│ Spec: 100%          │
│ Target: OpponentName│
├─────────────────────┤
│ Queue: 3 actions    │ ← AI decisions
│ This tick: 2 exec   │
│ Pending:            │
│   1. Switch Prayer  │ ← Red = Critical
│   2. Eat Food       │ ← Orange = High
└─────────────────────┘
```

**Color Meanings:**
- 🔴 **Red**: Critical actions (prayers, emergency eating)
- 🟠 **Orange**: High priority (offensive, special attacks)
- 🟡 **Yellow**: Medium priority (gear, attacks)
- ⚪ **Gray**: Low priority (movement)

---

## ⚙️ Configuration Explained

### API Connection
- **API Host**: Should always be `127.0.0.1` (localhost)
- **API Port**: Must match the port in START_PVP_RL.bat (default: 8888)
- **Model Name**: The AI model to use (FineTunedNh is recommended)

### Feature Toggles
Turn features on/off to control what the AI can do:

- ✅ **Enable Prayer Switching**: AI switches overhead prayers
- ✅ **Enable Gear Switching**: AI changes equipment (limited support)
- ❌ **Enable Movement**: AI moves your character (DISABLED by default - can be risky!)
- ✅ **Enable Attacking**: AI attacks targets
- ✅ **Enable Eating**: AI eats food and drinks potions
- ✅ **Enable Special Attack**: AI uses special attacks

### Safety Settings
- **Emergency Logout HP**: Auto-logout if HP drops below this (default: 20)
- **Only in PvP Areas**: Only runs in PvP zones (recommended: ON)
- **Require Target**: Only runs when you have a target (recommended: ON)
- **Max Actions Per Tick**: How many actions to execute per game tick (default: 12)

### Debug Options
- **Show Debug Overlay**: Display the stats overlay (recommended: ON)
- **Log Actions**: Print all actions to console (good for debugging)
- **Log Observations**: Print game state to console (very verbose!)
- **Tick Delay Multiplier**: Slow down the bot (1.0 = normal, 2.0 = half speed)

---

## 🎯 Tips for Best Results

### 1. Start in LMS
- Safest place to test
- Free gear provided
- No risk of losing items
- Good opponents to train against

### 2. Use NH Gear
The AI was trained on NH (No Honor) style combat:
- **Melee**: Godsword, Dragon claws, Dragon dagger
- **Ranged**: Crossbow, Ballista, Dark bow
- **Mage**: Ancient spellbook (Ice Barrage, Blood Barrage)
- **Food**: Manta rays, Anglerfish, Karambwans
- **Potions**: Saradomin brews, Super restores

### 3. Watch the First Fight
- Don't walk away immediately
- Watch how the AI behaves
- Check for any errors in overlay
- Make sure prayers are switching

### 4. Adjust Settings
If the AI dies too often:
- Increase "Emergency Logout HP" to 40-50
- Disable risky features (movement, special attacks)
- Use tankier gear

If the AI is too passive:
- Make sure "Enable Attacking" is ON
- Check you have a target selected
- Enable "Log Actions" to see what it's doing

---

## ❌ Troubleshooting

### Problem: "API: DISCONNECTED" (Red)

**Cause**: Python API server isn't running

**Fix**:
1. Check if START_PVP_RL.bat is still open
2. If closed, double-click it again
3. Look for errors in the black window
4. Make sure port 8888 isn't used by another program

---

### Problem: Bot connects but doesn't do anything

**Cause**: Missing target or disabled features

**Fix**:
1. Make sure you're targeting an opponent (right-click → Attack)
2. Check "Require Target" setting (if ON, you need a target)
3. Enable features: Attacking, Prayer Switching, Eating should all be ON
4. Check overlay - it should show "Target: [name]"

---

### Problem: Bot dies instantly

**Cause**: AI not defending properly

**Fix**:
1. Lower "Emergency Logout HP" to 40-50 (more cautious)
2. Make sure you have food in inventory
3. Check prayer points aren't 0
4. The AI was trained in simulation - real game is harder!
5. Try easier opponents first

---

### Problem: High latency (>500ms)

**Cause**: Slow API server or computer lag

**Fix**:
1. Close other programs
2. Restart START_PVP_RL.bat
3. Check your computer isn't under heavy load
4. The AI should respond in <100ms normally

---

### Problem: Actions not executing

**Cause**: Invalid actions or inventory issues

**Fix**:
1. Make sure you have food/potions in inventory
2. Check prayer points > 0
3. Enable "Log Actions" in config
4. Look for errors in Microbot console

---

## 🚨 Safety Warnings

### ⚠️ This is a BOT
- Use at your own risk
- Jagex bans bots
- Recommended: Use on alt accounts only
- Safest: Test in LMS (no ban risk for learning)

### ⚠️ The AI Isn't Perfect
- Trained in simulation, not real game
- May make mistakes
- Can die unexpectedly
- Don't risk expensive gear at first

### ⚠️ Emergency Stop
If something goes wrong:
1. **Click "Stop Bot"** in Microbot panel
2. **Press ESC** in game
3. **Log out manually**
4. **Close Microbot**

---

## 📁 File Locations

### Plugin Files
```
C:\Users\Elite\Desktop\Microbot Practice\Microbot-Hub\
  └── src\main\java\net\runelite\client\plugins\microbot\pvprl\
```

### Python API
```
C:\Users\Elite\Desktop\Microbot Practice\osrs-pvp-reinforcement-learning\pvp-ml\
```

### AI Models
```
C:\Users\Elite\Desktop\Microbot Practice\osrs-pvp-reinforcement-learning\pvp-ml\models\
  └── FineTunedNh.zip  ← Main model
```

### Startup Script
```
C:\Users\Elite\Desktop\Microbot Practice\START_PVP_RL.bat
```

---

## 🆘 Getting Help

### Check the Logs
1. **Microbot Console**: View → Show Console
2. **Python API Window**: The black window from START_PVP_RL.bat
3. **Enable Debug Logging**: Turn on "Log Actions" and "Log Observations"

### Common Error Messages

**"Failed to connect to API"**
→ START_PVP_RL.bat isn't running

**"No target"**
→ You need to attack someone first

**"No food found in inventory"**
→ Add food to your inventory

**"Failed to execute action"**
→ Check console for specific error

---

## 🎮 Advanced Usage

### Using Different Models
1. Check available models in `pvp-ml/models/` folder
2. Change "Model Name" in config to match filename (without .zip)
3. Examples: `FineTunedNh`, `GeneralizedNh`

### Customizing Gear Switches
Edit `ActionExecutor.java` to define your own gear sets

### Fine-tuning Performance
- Adjust "Max Actions Per Tick" (8-15 range)
- Modify "Tick Delay Multiplier" for speed
- Enable/disable specific features

---

## 📖 Additional Documentation

For detailed technical information, see:
- `README.md` - Full technical documentation
- `SETUP_GUIDE.md` - Detailed setup and testing guide

---

## ✨ Credits

- **Original PVP RL Project**: Training system and AI models
- **Microbot Framework**: RuneLite bot framework
- **Integration**: Claude AI + Elite

---

**Version**: 1.0.0
**Last Updated**: 2025-10-20

**Good luck in the wilderness! 🗡️**

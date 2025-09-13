# Nate Humidifier Plugin

The **Nate Humidifier Plugin** automates the process of casting the Humidify spell in Old School RuneScape, making it easy to fill empty containers (such as vials, buckets, jugs, and bowls) with water for skilling or profit.

---

## Features

- Automatically casts the Humidify spell on empty containers in your inventory
- Supports a variety of container types (see `enums/Items.java`)
- Overlay displays current status and progress
- Integrates with the Microbot RuneLite client

---

## Requirements

- Microbot RuneLite client
- Sufficient Magic level and runes to cast Humidify
- Plugin enabled in the Microbot plugin list

---

## How It Works

1. Configure the plugin with the type of container you wish to fill.
2. Start the plugin from the Microbot sidebar.
3. The script withdraws empty containers from the bank, casts Humidify, and repeats the process.
4. An overlay displays the current status and progress.

---

## Configuration

- Select which container to fill (see `HumidifierConfig.java` and `enums/Items.java`)
- Enable/disable overlay
- Adjust intervals or behavior if supported

---

## Limitations

- Only supports containers defined in the `enums/Items.java` file
- Does not handle buying containers or selling filled items
- Only automates the Humidify spell casting process

---

## Disclaimer & Waiver of Liability

**The Plugin is provided for educational purposes only.** By using this Plugin, you acknowledge and agree to the following:

- The Plugin is intended solely for research, learning, and understanding botting mechanics.
- It is **not** meant for active use in *Old School RuneScape (OSRS)* or any live game environment.
- The Plugin is provided **"as-is"** without warranties of any kind.
- You assume **all risks** (e.g., account bans, progress loss) from using this Plugin.
- You **waive all rights** to hold developers/contributors liable for any damages (direct, indirect, or consequential).
- Automation tools violate [Jagex’s Terms of Service](https://www.jagex.com/en-GB/terms). Use may result in **permanent account bans**. This Plugin does **not** endorse rule-breaking.
- This Plugin is **not affiliated with** Jagex Ltd. or *Old School RuneScape*.

---

## Feedback

Open an issue or contribute improvements!
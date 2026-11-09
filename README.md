# Slot Potions (Fabric, Minecraft 26.3)

Every inventory slot gives its own potion effect. The number of items in the slot is the effect level
(1 item = level I, 2 items = level II, ...). The effect lasts until you take the items out.
Hotbar, main inventory, armor slots and the offhand all work.

## Get the .jar

Open the **Releases** page of this repository and download `slot-potions-1.0.0.jar` from the newest build.
(Each push builds a new jar automatically with GitHub Actions. Check the **Actions** tab if a build fails.)

To build it yourself you need JDK 25 and Gradle 9.6+, then run `gradle build`. The jar is `build/libs/slot-potions-1.0.0.jar`.

## Install

Put the jar in your `mods` folder together with Fabric API 0.166.0+26.3.
Needs Minecraft 26.3, Fabric Loader 0.19.5+ and Java 25.
Works in singleplayer and on servers. On a server, only the server needs the mod.

## Default slots

| Slot | Effect |
|------|--------|
| Hotbar 1 to 9 | Strength, Speed, Haste, Resistance, Regeneration, Jump Boost, Fire Resistance, Night Vision, Water Breathing |
| Inventory top-left onward | Health Boost, Absorption, Saturation, Luck, Slow Falling, Conduit Power, Dolphin's Grace, Invisibility, then the list repeats |
| Boots / Leggings / Chestplate / Helmet | Haste / Resistance / Regeneration / Jump Boost |
| Offhand | Fire Resistance |

There are 17 effects and 41 slots, so some slots share an effect. Slots that share an effect add their item counts together.

## Config

`config/slotpotions.json` is created on first launch.

- `maxLevel`: highest level an item count can give (default 10, max 255).
- `slots`: slot number to effect ID. Use any effect ID, including ones from other mods. Set `""` to turn a slot off.

Slot numbers: 0-8 hotbar, 9-35 main inventory, 36 boots, 37 leggings, 38 chestplate, 39 helmet, 40 offhand.
Restart the game after editing.

# Antioxidant

[![License: LGPL-3.0-or-later](https://img.shields.io/badge/license-LGPL--3.0--or--later-blue.svg)](LICENSE)
[![Platform: Fabric](https://img.shields.io/badge/platform-Fabric-brightgreen.svg)](https://fabricmc.net/)
[![Minecraft: 26.2](https://img.shields.io/badge/minecraft-26.2-orange.svg)](https://minecraft.net/)

> **Antioxidant** is a community-developed fork of [GeyserMC/Rainbow](https://github.com/GeyserMC/Rainbow), maintained by **Jubair_Prem**, focused on improving Bedrock Edition compatibility through custom resource pack generation and Geyser mappings.

---

## Table of Contents

- [Overview](#overview)
- [Relationship with Upstream GeyserMC/Rainbow](#relationship-with-upstream-geysermcrainbow)
- [Features](#features)
- [Requirements & Dependencies](#requirements--dependencies)
- [Installation](#installation)
- [Complete Command Reference](#complete-command-reference)
- [Workflow Guide](#workflow-guide)
  - [1. Creating a Pack](#1-creating-a-pack)
  - [2. Mapping Custom Content](#2-mapping-custom-content)
  - [3. Exporting the Pack](#3-exporting-the-pack)
  - [4. Installing into Geyser](#4-installing-into-geyser)
- [Architecture & Distinction](#architecture--distinction)
- [Configuration and Storage Paths](#configuration-and-storage-paths)
- [Building from Source](#building-from-source)
- [Troubleshooting](#troubleshooting)
- [Attribution & Licensing](#attribution--licensing)

---

## Overview

Antioxidant is a client-side Minecraft Fabric mod designed to bridge Java Edition custom content with Bedrock Edition players via Geyser. By analyzing loaded resource packs, item components, block state overrides, models, textures, animations, and sound definitions directly inside the running client, Antioxidant automates the creation of:
1. **Bedrock Resource Packs** (`pack.zip`) containing converted Bedrock geometries, attachables, textures, sound definitions, and flipbook animations.
2. **Geyser Custom Mappings** (`custom_mappings/`) ready for Geyser's custom item and block API (v2).
3. **Locale Overrides** (`lang/`) merging translation keys so Bedrock clients display accurate item names and tooltips.

---

## Relationship with Upstream GeyserMC/Rainbow

Antioxidant originated as a specialized fork of **Rainbow**, originally developed by the GeyserMC team and contributors. 

- **Maintainer:** Jubair_Prem
- **Upstream Repository:** [https://github.com/GeyserMC/Rainbow](https://github.com/GeyserMC/Rainbow)
- **Mod ID:** `antioxidant` (core library: `antioxidant-core`)
- **Root Command:** `/antioxidant`

Antioxidant preserves all mapping capabilities and algorithms established by upstream Rainbow while providing enhanced branding, verified chat messaging and localized feedback, backward-compatible workspace fallback, streamlined packaging, and active maintenance for Minecraft 26.2.

---

## Features

- **Custom Block Mappings:** Automatically or explicitly maps custom blocks created with block state overrides.
- **Custom Item Mappings:** Scans items for custom `minecraft:item_model` and `minecraft:custom_model_data` components. Supports:
  - Plain item models.
  - Conditional item model predicates (`broken`, `damaged`, `custom_model_data`, `has_component`, `fishing_rod/cast`).
  - Range dispatch item models (`bundle/fullness`, `count`, `custom_model_data`, `damage`).
  - Select item models (`charge_type`, `trim_material`, `context_dimension`, `custom_model_data`, `display_context`).
  - Legacy `custom_model_data` range-dispatch mapping to Geyser legacy mappings.
- **Player Head / Skull Mappings:** Automatically generates Geyser custom skull definitions from player head profiles.
- **Waypoint Style Mappings:** Maps locator bar waypoint styles and icons to Bedrock format.
- **Sound Mapping:** Extracts and converts sound events and definitions from loaded Java resource packs.
- **3D & 2D Item Attachables:** Generates Bedrock attachable JSONs, animations, and 3D geometry models from Java unbaked models, rendering custom GUI icons where appropriate.
- **Animated Textures:** Fully handles animated (flipbook) textures for 2D and 3D items.
- **Language / Localization Export:** Gathers and merges language files from loaded resource packs for direct insertion into Geyser locale overrides.

---

## Requirements & Dependencies

- **Minecraft:** `26.2`
- **Mod Loader:** [Fabric Loader](https://fabricmc.net/) (v0.19.3+)
- **Required Mod:** [Fabric API](https://modrinth.com/mod/fabric-api) (`0.153.0+26.2` or compatible)
- **Java Runtime:** Java 25 or higher

---

## Installation

1. Install **Fabric Loader** for Minecraft 26.2.
2. Place the [Fabric API](https://modrinth.com/mod/fabric-api) JAR into your client's `.minecraft/mods/` directory.
3. Download or build `Antioxidant-26.2.jar` and place it into your `.minecraft/mods/` directory.
4. Launch Minecraft using your Fabric profile.

---

## Complete Command Reference

All commands in Antioxidant are registered under the primary root `/antioxidant`.

| Command | Description |
| :--- | :--- |
| `/antioxidant create <name>` | Starts a new resource pack mapping session named `<name>`. |
| `/antioxidant map block <target>` | Maps the custom block at the specified block coordinates (`<target>`). |
| `/antioxidant map item` | Maps the custom item or skull currently held in the player's main hand. |
| `/antioxidant map sounds <namespace>` | Maps all custom sounds belonging to the specified resource namespace. |
| `/antioxidant map waypoint <style>` | Maps the specified custom waypoint style. |
| `/antioxidant mapinventory` | Scans the player's entire inventory and hotbar for custom items and skulls. |
| `/antioxidant auto blocks` | Scans loaded resource packs for custom blocks created via block state overrides. |
| `/antioxidant auto inventory` | Begins continuous background scanning of any container or menu opened by the player. |
| `/antioxidant auto recipes` | Scans all known recipes for results producing custom items or skulls. |
| `/antioxidant auto sounds` | Scans all loaded resource packs for custom sounds. |
| `/antioxidant auto waypoints` | Scans all loaded resource packs for custom waypoint styles. |
| `/antioxidant auto stop` | Stops active automatic container/inventory background scanning. |
| `/antioxidant finish` | Completes and writes the Bedrock pack, Geyser mappings, language overrides, and report to disk. |

---

## Workflow Guide

### 1. Creating a Pack
Begin by starting a new pack session with a unique name:
```mcfunction
/antioxidant create mypack
```
This initializes mapping caches in `.minecraft/antioxidant/mypack/`.

### 2. Mapping Custom Content
Use any combination of manual and automated mapping commands:
- Hold a custom item in your main hand and run:
  ```mcfunction
  /antioxidant map item
  ```
- Or open menus and containers while continuous scanning is active:
  ```mcfunction
  /antioxidant auto inventory
  ```
  *(Run `/antioxidant auto stop` once you are done).*
- Scan all custom blocks defined across your resource packs:
  ```mcfunction
  /antioxidant auto blocks
  ```

### 3. Exporting the Pack
When you have finished mapping all items and blocks, execute:
```mcfunction
/antioxidant finish
```
Antioxidant will compile all geometries, attachables, textures, and mapping descriptors, then display clickable chat confirmation:
```
[Antioxidant] Your Bedrock resource pack and Geyser mappings have been generated successfully! Click to open folder.
```

### 4. Installing into Geyser
Navigate to `.minecraft/antioxidant/<name>/`. You will find:
- **`pack.zip`**: Copy this file to your Geyser server's `packs/` folder.
- **`custom_mappings/`**: Copy the JSON mapping files inside this folder to Geyser's `custom_mappings/` directory.
- **`lang/`**: Copy language files from this folder to Geyser's `locales/overrides/` directory.
- **`report.txt`**: A generation report containing conversion details, counts, and non-fatal warnings.

Restart your Geyser server or proxy. When Bedrock players connect, they will automatically download the resource pack and see all custom items, blocks, and sounds seamlessly.

---

## Architecture & Distinction

Antioxidant operates strictly as a **client-side mod**. It utilizes the client's internal resource manager, model loaders, and texture stitcher to read and interpret complex Java JSON models and textures.

- **Antioxidant (Fabric Client Mod):** Does not run on the server. Used only during development or configuration to inspect models and export pack assets.
- **Generated Output (`pack.zip`, `custom_mappings/`, `lang/`):** Deployed onto the server or proxy running **Geyser**. The server itself does not need the Fabric mod installed.

---

## Configuration and Storage Paths

Exported packs and reports are written to:
```
.minecraft/antioxidant/<pack_name>/
```
**Backward Compatibility:** If `.minecraft/antioxidant/` is not present but a legacy `.minecraft/rainbow/` folder exists, Antioxidant safely falls back to `.minecraft/rainbow/` to prevent loss of existing workspaces.

---

## Building from Source

Antioxidant uses the Gradle wrapper and modern Loom build toolchain.

### Prerequisites
- JDK 25 installed and available on `PATH`.
- Git.

### Build Steps
```bash
# Clone the repository:
git clone https://github.com/Jubair-Prem/Antioxidant.git
cd Antioxidant

# Build the mod and generate distributable JARs:
./gradlew build
```

The compiled, distributable client JAR is generated at:
```
client/build/libs/Antioxidant-26.2.jar
```

---

## Troubleshooting

- **Raw translation keys in chat:** All messages use formatted text components and `assets/antioxidant/lang/en_us.json`. Ensure the Antioxidant resource assets are intact.
- **Missing custom items in Bedrock:** Verify that `custom_mappings` JSON files were placed in Geyser's `custom_mappings/` folder and that `pack.zip` was added to `packs/`.
- **Client freeze during `/antioxidant auto blocks`:** Scanning thousands of block states across large resource packs is CPU-intensive. Allow the client several seconds to finish baking unbaked model trees.

---

## Attribution & Licensing

- **Project Name:** Antioxidant
- **Maintainer:** Jubair_Prem
- **Original Project:** [GeyserMC/Rainbow](https://github.com/GeyserMC/Rainbow) by the GeyserMC organization and contributors.

Antioxidant is distributed under the terms of the **GNU Lesser General Public License v3.0 or later (LGPL-3.0-or-later)**. See [LICENSE](LICENSE) and [LICENSE.LESSER](LICENSE.LESSER) for full license terms.

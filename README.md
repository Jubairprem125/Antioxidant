# Antioxidant

Java 25 Bukkit plugin for Paper and Spigot. It scans Java resource-pack assets and the optional ItemsAdder/Nexo registries, then creates a Bedrock resource-pack draft, a Geyser-oriented mapping manifest, a conversion report, and a `.mcpack` archive.

## Build

Use a Gradle installation running on JDK 25:

```sh
gradle clean test jar
```

The artifact is `build/libs/Antioxidant.jar`. The Spigot API is compile-only; no server API or optional plugin is bundled.

## Install and use

1. Install the JAR into `plugins/` on Paper or Spigot. Geyser is optional for generating files; install it to use the Bedrock resource-pack workflow.
2. Start the server. Java resource packs are scanned from `resourcepacks/`, `plugins/Antioxidant/source-packs/`, and resource assets under ItemsAdder/Nexo plugin folders.
3. Generated artifacts are written under `plugins/Antioxidant/generated/`; cache and JSON report are under `cache/` and `logs/`.
4. Use `/antioxidant status`, `/antioxidant mappings`, or `/antioxidant reload`.

## What it converts

The scanner recognizes Java item-model overrides containing `custom_model_data`, item definitions that explicitly declare a `base_item`, and custom item metadata exposed by the installed ItemsAdder/Nexo APIs. It copies resolvable PNG textures without recompression, emits Bedrock item/icon resources and mapping records, and keeps identifiers and pack UUIDs stable. Basic 2D/generated models are represented by their texture; complex Java geometry, predicates, component semantics, animation, and arbitrary parent/model behavior are not losslessly translated.

The generated mapping manifest is version-neutral output for review/integration, not a promise that every Geyser release will load it as a runtime mapping file. Geyser does not expose a stable general-purpose Bukkit API for registering arbitrary custom item mappings on all platforms. If its detected pack directory is available, the plugin stages the `.mcpack` there; whether a running Geyser instance loads a newly staged pack depends on that installation's pack configuration and may require a Geyser reload/restart. The plugin does not silently alter original resource packs or Geyser configuration.

Generic Java resource packs cannot reveal which vanilla item a model is assigned to unless the pack has an override or explicit metadata. ItemsAdder/Nexo content is therefore best-effort and requires those plugins' item registries to be ready during scanning.

## Commands

All commands require `antioxidant.admin`:

* `/antioxidant` or `status` — status summary
* `reload` — reload config and rescan/rebuild
* `scan` — rescan resource sources and regenerate output
* `generate` — rebuild all generated output
* `debug` — detailed environment and conversion summary
* `mappings` — show the generated mapping file location
* ## 🚧 Currently in active development.
* 

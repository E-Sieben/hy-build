# hy-build

A Gradle plugin that handles the boilerplate of Hytale plugin development — downloading the server,
setting up dependencies, and managing your plugin manifest.

## Installation

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
  id("net.esieben.hybuild") version "<version>"
}
```

The plugin automatically applies [Lombok](https://projectlombok.org/) and adds `HytaleServer.jar` as
a `compileOnly` dependency whenever the `java` plugin is present.

## Configuration

Use the `hytale` block to describe your plugin:

```kotlin
hytale {
  authors = listOf("Your Name")
  description = "What your plugin does"   // optional
  website = "https://github.com/you/repo" // optional
}
```

The `Group`, `Name`, `Version`, `Main`, and `ServerVersion` fields in the manifest are derived
automatically from your Gradle project — you only need to supply what Gradle doesn't already know.

| Manifest field  | Derived from                                                      |
|-----------------|-------------------------------------------------------------------|
| `Group`         | `project.group`                                                   |
| `Name`          | `project.name`                                                    |
| `Version`       | `project.version`                                                 |
| `Main`          | `${project.group}.${PascalCase(project.name)}`                    |
| `ServerVersion` | Filename of the version zip inside `.hytale/` (without extension) |

---

## Tasks

All tasks appear under the **hytale server** or **hytale project** groups in your Gradle task list.

### Server tasks

These tasks manage the Hytale server used for local testing. They run in order and each one depends
on the previous.

#### `downloadServerDownloader`

Downloads the official Hytale Server Downloader executable into `.hytale/` if it isn't there
already.

#### `launchServerDownloader`

Runs the downloader to fetch the server version package. Skips automatically if the version zip is
already present.

*Depends on: `downloadServerDownloader`*

#### `extractServerZip`

Extracts `HytaleServer.jar`, `HytaleServer.aot`, and `Assets.zip` from the downloaded version
package into `.hytale/`

*Depends on: `launchServerDownloader`*

#### `runServer`

Starts the Hytale Server. Opens a dedicated terminal window on Linux, macOS, and Windows so the
server stays interactive. Falls back to inline output in headless environments (CI, SSH).

If the `java` plugin is present, your plugin's JAR is automatically built and deployed to
`.hytale/server/mods/` before launch.

*Depends on: `extractServerZip`, `jar` (when java plugin is applied)*

---

### Project tasks

#### `overwriteManifest`

Updates `src/main/resources/manifest.json` in place — only the fields that have changed are touched.
Fields you've added manually are left exactly as they are.

Logs each changed field with its old and new value:

```
  Version: 1.0.0 → 1.1.0
  ServerVersion: 2024.01.01-abc → 2024.06.15-def
Manifest updated (2 field(s) changed)
```

Runs automatically as part of `processResources` and `check`, so your manifest stays current on
every build without losing anything you've added by hand.

#### `validateManifest`

Validates `src/main/resources/manifest.json` against the Hytale plugin manifest schema:

- All required fields are present (`Group`, `Name`, `Version`, `Authors`, `Main`, `ServerVersion`)
- `Version` follows [Semantic Versioning](https://semver.org/)
- `Main` matches the fully-qualified class name pattern (`com.example.MyPlugin`)
- All errors are reported together in a single failure

Runs automatically after `overwriteManifest`.

#### `createManifest`

Generates a completely fresh `src/main/resources/manifest.json`, **replacing all existing content**
including any fields you've added manually. Use this for initial setup or to reset the manifest to
the plugin-managed defaults.

For day-to-day builds, `overwriteManifest` runs automatically and is the right task to use.

---

### Shipping assets with your plugin

If your plugin includes an asset pack, add `IncludesAssetPack` to your manifest manually and
`overwriteManifest` will preserve it across builds:

```json
{
  "IncludesAssetPack": true
}
```

#### `addHytaleFolderToGitignore`

Appends a `### Hytale ###` section with `.hytale` to your `.gitignore`. Creates the file if it
doesn't exist and is idempotent — safe to run multiple times.

---

## Getting started

A typical first-time setup looks like this:

```
./gradlew initializeProject
```

`runServer` downloads everything it needs automatically. Once the server has been downloaded once,
subsequent runs skip straight to launching.

To include your plugin in the server during development:

```
./gradlew runServer
```

Your JAR is built and copied into `.hytale/server/mods/` before the server starts.

---

## Project structure

After set up your project will look something like this:

```
your-plugin/
├── .hytale/                     # managed by hy-build, add to .gitignore
│   ├── hytale-downloader        # the official downloader binary
│   ├── 2024.06.15-deadbeef.zip  # downloaded server version package
│   ├── HytaleServer.jar
│   ├── HytaleServer.aot
│   ├── Assets.zip
│   └── server/
│       └── mods/
│           └── your-plugin.jar  # deployed before each server run
├── src/
│   └── main/
│       ├── java/
│       └── resources/
│           └── manifest.json    # generated by createManifest
└── build.gradle.kts
```

---

## Full example `build.gradle.kts`

```kotlin
plugins {
  java
  id("net.esieben.hybuild") version "<version>"
}

group = "com.example"
version = "1.0.0"

hytale {
  authors = listOf("Your Name")
  description = "A plugin that does something cool"
  website = "https://github.com/you/your-plugin"
}
```

That's it. `Main` is derived as `com.example.YourPlugin` and `ServerVersion` is read from the
downloaded server zip automatically.

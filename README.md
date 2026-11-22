# HTMLGO-M

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Status: EOL](https://img.shields.io/badge/Status-v2.0R%20(EOL)-orange.svg)](#status--end-of-life-notice)
[![Platform: Android & Wear OS](https://img.shields.io/badge/Platform-Android%20%7C%20Wear%20OS-3DDC84.svg)](#project-structure)

**HTMLGO-M** (`com.absolutemango.htmlgom`) is an offline-first Web Application Runner, Local Workspace Engine, and AI-assisted HTML/WebGL sandbox built natively for Android and Wear OS.

Designed to run standalone `.html` files, `.zip` web bundles, WebGL shader demos, Markdown/LaTeX documents, and browser-based games directly from local storage without requiring an external web server.

---

## Status & End-of-Life Notice

This repository represents the **final archived source release (`v2.0R - EOL / Build 4`)** of HTMLGO-M. Active feature development has concluded, and the complete codebase is published here under the **GNU General Public License v3.0 (GPL-3.0)** as a permanent, open-source archive for the community.

---

## Key Features

### Workspace & Web Execution
- **Offline Workspace Launcher:** Searchable recent-files history supporting local HTML files, pasted snippets, and external web bundles with quick file-type badges.
- **Dual Rendering Engine Architecture:** Switch between hardware-accelerated system `WebView` (fast startup, lightweight footprint) and embedded **Mozilla GeckoView** (full desktop-grade HTML5, CSS, and WebGL compatibility), alongside on-the-fly **DOM Storage** and **Hardware Acceleration** toggles.
- **Built-in Archive Tools:** Integrated **Compressor** and **Decompressor** utilities to package HTML projects into `.zip` bundles or extract `.zip` archives straight into playable workspace folders.

### Creation & System Suite
- **AI Forge Studio:** Built-in AI web-app generator with live HTML previews, instant **Play** execution, and a hardware-encrypted **Bring Your Own Key (BYOK) Vault** supporting primary/backup API key failover, custom system prompts (*Mr. Forger* mode), custom JSON payloads, and multi-model selection.
- **Workspace File Editor:** Dedicated local `/workspace` file manager and **Sora Editor** integration with TextMate/Monarch syntax highlighting for creating and editing HTML, CSS, JS, and JSON files on-device.
- **Emergency Browser:** Lightweight built-in web browser with liquid tabs, bottom address bar, and one-tap quick shortcuts.

### Customization & Wear OS Sync
- **Wear OS Companion (`HTMLGO-W`):** Dedicated watch connection dashboard for pairing with **HTMLGO-W** and sending HTML apps or `.zip` bundles directly to your smartwatch over the Wear OS Data Layer.
- **Dynamic Monet & Expressive Themes:** Live Material 3 Expressive theming computed from your system wallpaper across **9 color schemes** (*Dynamic Monet*, *Tonal Spot*, *Neutral*, *Vibrant*, *Expressive*, *Fidelity*, *Content*, *Monochrome*, *Rainbow*, and *Fruit Salad*).
- **Extra Black Mode & Shader Controls:** Pure `#000000` AMOLED dark theme with a toggleable **Solid Gradient Mode** to disable blur/backdrop effects for maximum battery life and older devices.

---

## Release Variants

Prebuilt signed APKs are available on the [Releases](https://github.com/Absolute-Mango-co/HTMLGO-M/releases) page:

| Variant | Renderer | Best For |
| :--- | :--- | :--- |
| **HTMLGO-M Lite** | Hardware System `WebView` | Lightweight installs, fast startup, standard HTML5 apps |
| **HTMLGO-M Standard** | Hardware `WebView` + Mozilla `GeckoView` | Heavy WebGL shaders, complex JS engines, standalone rendering |
| **HTMLGO-W** | Wear OS Standalone Companion | Running web bundles & receiving files on Wear OS smartwatches |

---

## Project Structure

```text
HTMLGO-M/
├── app/htmlgom/      # Primary Android application & Rust JNI core (rust-core)
├── htmlgow/          # Standalone Wear OS companion application module
├── editor/           # Embedded Sora Editor module
├── backdrop/         # AGSL Liquid Glass refractive backdrop & shader module (Apache-2.0)
├── expressive-ui/    # Custom Material 3 Expressive UI components
├── artworks/         # Brand assets, icons, and vector graphics
└── gradle/           # Gradle Wrapper & version catalog (libs.versions.toml)

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

- **Dual Rendering Engine Architecture:**
  - **Lite Variant:** Uses Android's hardware-accelerated system `WebView` for ultra-fast cold starts and a compact APK footprint.
  - **Standard Variant:** Combines hardware `WebView` with an embedded **Mozilla GeckoView** runtime for full desktop-grade HTML5, CSS, and WebGL compatibility.
- **Offline Workspace, FTP & Bundle Loader:** Open single HTML documents or packaged `.zip` web projects directly from device storage, backed by a native Rust core (`rust-core`) and embedded Apache FTP server for local asset transfer.
- **Built-in Code Editor & AI Workflow:** Integrated **Sora Editor** (`:editor`) with TextMate/Monarch syntax highlighting, live previewing, Markdown + LaTeX math rendering, and Gemini API integration.
- **Refractive Liquid Glass UI:** Built with **Jetpack Compose**, **Material 3 Expressive**, **Haze**, and **AGSL shaders (`AGLS-BackdropV5.6`)** featuring dynamic Material You (Monet) theming and AMOLED-friendly dark surfaces.
- **Wear OS Companion (`htmlgow`):** Includes a standalone Wear OS module with an edge-sliding drawer, rotating-bezel navigation, and embedded GeckoView support for running web apps on smartwatches.

---

## Release Variants

Prebuilt signed APKs are available on the [Releases](https://github.com/Absolute-Mango-co/HTMLGO-M/releases) page:

| Variant | Renderer | Best For |
| :--- | :--- | :--- |
| **HTMLGO-M Lite** | Hardware System `WebView` | Lightweight installs, fast startup, standard HTML5 apps |
| **HTMLGO-M Standard** | Hardware `WebView` + Mozilla `GeckoView` | Heavy WebGL shaders, complex JS engines, standalone rendering |

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

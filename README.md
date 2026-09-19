# PilcrowMD Desktop — Community Edition

An unofficial, community-built desktop edition of **PilcrowMD** for Windows and Linux.

PilcrowMD Desktop brings the private, distraction-free Markdown reading and editing experience of the original [PilcrowMD](https://github.com/pilcrowmd/pilcrow) Android application to desktop platforms.

> **Community project:** This is an independent, unofficial desktop adaptation of PilcrowMD. It is not an official release of, affiliated with, or endorsed by the original PilcrowMD project or its developer.

## ✨ Features

- **Windows & Linux** — Native desktop packages for both platforms.
- **Reader & Editor Modes** — Switch seamlessly between editing raw Markdown and reading beautifully rendered documents.
- **Markdown Support** — Headings, lists, tables, code blocks, strikethrough, and other common Markdown features.
- **Syntax Highlighting** — Read and edit code blocks with syntax-aware formatting.
- **In-document Search** — Search documents with match highlighting and navigation.
- **PDF Export** — Export Markdown documents to paginated, print-friendly PDFs.
- **Customizable Typography** — Independent font scaling for the reader and editor.
- **Dark & Light Themes** — Choose between a dark theme and warm-cream light theme.
- **Native File Integration** — Open `.md` files directly from your operating system.
- **Offline & Private** — Your Markdown files remain on your computer.

## 📸 Screenshots

| Reader Mode | Editor Mode |
|:---:|:---:|
| <img src="docs/screenshots/desktop-reader.png" width="400" alt="Pilcrow Desktop Reader Mode"> | <img src="docs/screenshots/desktop-editor.png" width="400" alt="Pilcrow Desktop Editor Mode"> |

## 🚀 Download & Install

Download the latest Community Edition release from the [Releases](https://github.com/abuhamza-git/pilcrow-desktop/releases) page.

### Windows

Download the `.msi` installer from the latest release.

### Linux

Packages are available in formats including:

- `.deb` — Debian / Ubuntu
- `.rpm` — Fedora / Red Hat
- `.AppImage` — Portable Linux package

## 🛠️ Tech Stack & Architecture

PilcrowMD Desktop — Community Edition builds upon the open-source PilcrowMD project while introducing a dedicated desktop application built with **JetBrains Compose Desktop**.

Shared pure-Kotlin functionality from the original project is used where appropriate, while the desktop interface and desktop-specific functionality are developed separately.

| Area | Technology |
| --- | --- |
| Language | Kotlin 2.3 |
| UI Framework | JetBrains Compose Desktop |
| Markdown Parsing | `commonmark-java` with GFM extensions |
| PDF Generation | `openhtmltopdf` |
| State Management | Coroutines & Flow |
| Persistence | JSON-backed `StorageManager` |
| Build Tool | Gradle (Kotlin DSL) |

## 🏗️ Build It Yourself

### Requirements

JDK 17+

Clone the repository:

```bash
git clone https://github.com/abuhamza-git/pilcrow-desktop.git
cd pilcrow-desktop
```

Run the desktop application:

```bash
./gradlew :desktop-app:run
```

Package a native distribution for your current operating system:

```bash
./gradlew :desktop-app:packageDistributionForCurrentOS
```

## 🌱 Community Edition

This project exists to explore and provide a community-maintained desktop experience based on PilcrowMD.

The desktop edition may develop features, UI patterns, integrations, and workflows specifically designed for Windows and Linux while preserving the core philosophy that makes PilcrowMD useful: a focused, private, offline Markdown experience.

Issues, ideas, bug reports, and contributions related to the **Community Edition** are welcome in this repository.

## 💙 Original PilcrowMD

This project is based on the open-source **PilcrowMD** Android application created by **pleree**.

Original project:

**https://github.com/pilcrowmd/pilcrow**

PilcrowMD is a private, native Markdown reader and editor for Android with no ads, tracking, or required network access for reading local files.

All credit for the original PilcrowMD project and its Android implementation belongs to its original author and contributors.

This Community Edition is maintained independently and should not be confused with an official PilcrowMD desktop release.

## 📜 License

PilcrowMD Desktop — Community Edition is distributed under the **GNU General Public License v3.0 or later (GPL-3.0-or-later)**, consistent with the license of the original PilcrowMD project.

Copyright for code originating from PilcrowMD remains with its respective original copyright holders.

New code and modifications introduced by contributors to this Community Edition remain attributed to their respective authors.

Third-party dependencies, fonts, and other bundled components may be subject to their own licenses. See [`LICENSES.md`](LICENSES.md) for details.

See [`LICENSE`](LICENSE) for the full GPL license terms.

---

### Trademark & Affiliation Notice

**PilcrowMD** is the name used by the original PilcrowMD project.

**PilcrowMD Desktop — Community Edition is an independent community project and is not an official desktop release of PilcrowMD. It is not affiliated with, sponsored by, or endorsed by the original PilcrowMD developer.**

The “Community Edition” designation is used to distinguish this project from the original application and any official PilcrowMD desktop software that may be released in the future.
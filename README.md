# 🌊 AvatarSMP

[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Paper](https://img.shields.io/badge/Paper-1.20%2B-blue.svg)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-All_Rights_Reserved-red.svg)](#-license)
[![Modrinth](https://img.shields.io/badge/Modrinth-Download-00AF5C.svg)](https://modrinth.com/plugin/avatarsmp)
[![Spigot](https://img.shields.io/badge/SpigotMC-Download-ED8106.svg)](https://www.spigotmc.org/resources/avatarsmp.137247/)

**AvatarSMP** is a feature-rich Minecraft plugin that brings element bending, ability progression, custom GUI management, and Chi blocking mechanics to your Paper/Spigot server!

---

## ✨ Features

- ** Elemental Bending:** Choose between core elements (Water, Earth, Fire, Air) with unique abilities.
- ** Specializations:** Unlock advanced elemental masteries.
- ** Chi Blocking Mechanics:** Neutralize opponents' bending abilities temporarily in combat.
- ** Interactive GUIs:** Built-in graphical menus for selecting elements, specializations, and binding abilities.
- ** Progression System:** Level up your bending with an integrated XP system.
- ** Full Multi-Language Support:** Easily switch between languages (`messages_pl.yml`, `messages_en.yml`) with modern **Adventure MiniMessage** formatting.
- ** Auto-Updater:** Keep your plugin up to date directly from GitHub releases.

---

## 📥 Installation

1. Download the latest `.jar` file from [Modrinth](https://modrinth.com/plugin/avatarsmp) or [SpigotMC](https://www.spigotmc.org/resources/avatarsmp.137247/).
2. Place the `.jar` file into your server's `plugins/` directory.
3. Ensure your server is running **Paper / Purpur 1.20+** (Java 17 or higher required).
4. Restart your server to generate default configuration files.
5. Set your preferred language in `config.yml` (`language: "pl"` or `language: "en"`).

---

## 📜 Commands & Permissions

| Command | Description | Permission | Default |
| :--- | :--- | :--- | :--- |
| `/avatar` | Opens the main Avatar element selection GUI | `avatarsmp.use` | Everyone |
| `/avatar bind` | Opens the ability binding menu | `avatarsmp.bind` | Everyone |
| `/avatar admin reload` | Reloads `config.yml` and language files | `avatarsmp.admin` | OP |
| `/avatar admin setxp <player> <amount>` | Sets a player's bending XP | `avatarsmp.admin` | OP |
| `/avatar admin addxp <player> <amount>` | Adds bending XP to a player | `avatarsmp.admin` | OP |
| `/avatar admin resetxp <player>` | Resets a player's XP and level | `avatarsmp.admin` | OP |
| `/avatar admin revoke <player>` | Strips all elemental powers and resets progress | `avatarsmp.admin` | OP |

---

## 🌐 Localization & Customization

AvatarSMP fully supports Kyori's **MiniMessage** format, allowing rich gradient text, hex colors, and hover events.

Change language in `config.yml`:
```yaml
# Supported out-of-the-box: "pl", "en"
language: "en"

## 📄 License

Copyright © 2026 **PIWK0__bip**. All Rights Reserved.

This project is proprietary software. You are **not** permitted to redistribute, modify, re-upload, or sell any part of this plugin (source code or compiled `.jar` files) without explicit written permission from the author.

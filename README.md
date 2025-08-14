# Infinity Dungeons Stats

![License](https://img.shields.io/badge/License-MIT-blue.svg) ![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21+-green.svg)

A comprehensive statistics and leaderboard system for the **Infinity Dungeons** map. Track player high scores, monster kills, and total playtime with in-game leaderboards and a powerful RESTful API for developers.

---

This plugin is designed to bring a competitive edge to your Infinity Dungeons rogue-like server. It adds a robust statistics system to:
*   **Track Player Progress:** Automatically records each player's highest dungeon level, total kills, and total playtime.
*   **Foster Competition:** Display dynamic leaderboards in your world using floating text (holograms) to show off the top players.
*   **Engage Your Community:** Players can check their own stats or compare themselves against the server's best.
*   **Provide Developer Tools:** A built-in RESTful API allows you to pull stats for use on websites, Discord bots, or other custom integrations.

## ✨ Features

*   **Player Statistics Tracking:** Monitors and saves crucial data for every player's run in the dungeon.
    *   Highest Dungeon Level Reached
    *   Total Monster Kills
    *   Total Playtime
*   **In-Game Leaderboards:**
    *   Highly configurable floating text displays (holograms) for top kills, playtime, and max level.
    *   Choose between a single, rotating hologram or multiple static ones.
    *   Customize titles, colors, and formats.
*   **Player Commands:**
    *   `/dun stats [player]` - View your own or another player's statistics.
    *   `/dun killtop` - Display the top players by monster kills in chat.
    *   `/dun playtimetop` - Display the top players by playtime in chat.
    *   `/dun maxleveltop` - Display the top players by max level in chat.
*   **RESTful API:**
    *   An optional, built-in API server to expose player and server stats via simple HTTP requests. (Requires opening a port on your server).
*   **Lightweight & Efficient:** Designed specifically for Infinity Dungeons to be as performant as possible.

## ⚙️ Configuration

The plugin is fully configurable via `config.yml`. You can enable/disable features, set hologram locations, and customize all messages.

```yaml
# DungeonStats Plugin Configuration
database: "data.yml"
api-server:
  enabled: true
  port: 8080
log-checker:
  enabled: true
  interval-ticks: 100
holograms:
  enabled: false
  refresh-interval-seconds: 10
  # 'SINGLE': Use one text_display entity, rotating between different leaderboards.
  # 'MULTIPLE': Use a separate text_display entity for each leaderboard.
  display-mode: 'SINGLE'
  single-display:
    location: "world,0.5,100.5,0.5" # Format: "world_name,x,y,z"
    # How long (in seconds) to display each leaderboard before switching to the next.
    rotation-interval-seconds: 5
  multiple-displays:
    killtop:
      location: "world,5.5,101.5,0.5"
    playtimetop:
      location: "world,0.5,101.5,5.5"
    maxleveltop:
      location: "world,-4.5,101.5,0.5"
messages:
  title-kills: "&6&l--- Kills Leaderboard ---"
  title-playtime: "&6&l--- Playtime Leaderboard ---"
  title-maxlevel: "&6&l--- Max Level Leaderboard ---"
  # Available Placeholders: {rank}, {player_name}, {value}
  rank-entry: "#{rank} &b{player_name}: &f{value}"
  rank-color-1: "&e" # Gold for Rank 1
  rank-color-2: "&f" # Silver for Rank 2
  rank-color-3: "&6" # Bronze for Rank 3
  rank-color-default: "&7" # Default color for other ranks
  command-usage: "&cUsage: /dun <stats|killtop|playtimetop|maxleveltop>"
  command-player-not-found: "&cCould not find data for player {player_name}"
  command-no-data: "&7No data available yet"
  stats-title: "&6--- Stats for {player_name} ---"
  stats-line-maxlevel: "&eHighest Dungeon Level: &f{value}"
  stats-line-kills: "&eTotal Kills: &f{value}"
  stats-line-playtime: "&eTotal Playtime: &f{value}"
```

## 🔌 RESTful API Documentation

If `api-server.enabled` is set to `true`, the plugin will host a simple API server on the configured port. **This discloses game data to a remote server connection and must be firewalled appropriately if you do not want it to be public.**

<details>
<summary><b>Click to view API Endpoints</b></summary>

### GET /players

Returns a list of online players and their current status.

**Example Response:**
```json
[
  {
    "name": "Steve3184",
    "status": "waiting",
    "health": 20.0,
    "armor": 0.0
  },
  {
    "name": "AdLambXD",
    "status": "ingame",
    "health": 15.0,
    "armor": 4.0
  },
  {
    "name": "Mark_Q",
    "status": "spectator",
    "health": 20.0,
    "armor": 0.0
  }
]
```

### GET /stats

Returns a log of completed dungeon runs.
*   **Optional Query Parameter:** `limit` (e.g., `/stats?limit=10`) - Sets the maximum number of records to return. Defaults to `0` (unlimited).

**Example Response:**
```json
[
  {
    "recordId": 6,
    "level": 63,
    "doorsOpened": 63,
    "enemiesKilled": 203,
    "bossesDefeated": 9,
    "durationSeconds": 3686
  }
]
```

### GET /playerstats

Returns the lifetime statistics for a single player.
*   **Required Query Parameter:** `name` (e.g., `/playerstats?name=Steve3184`)

**Success Response:**
```json
{
  "playerName": "Steve3184",
  "kills": 6,
  "playtimeSeconds": 38,
  "maxLevel": 1
}
```

**Error Responses:**
```json
{
  "error": "Player not found."
}
```

```json
{
  "error": "Player name query parameter is required."
}
```

### GET /killtop

Returns the top 100 players by monster kills.

**Example Response:**
```json
[
  {
    "playerName": "Steve3184",
    "kills": 6
  }
]
```

### GET /playtimetop

Returns the top 100 players by total playtime.

**Example Response:**
```json
[
  {
    "playerName": "Steve3184",
    "playtimeSeconds": 38
  }
]
```

### GET /maxleveltop

Returns the top 100 players by highest dungeon level reached.

**Example Response:**
```json
[
  {
    "playerName": "Steve3184",
    "maxLevel": 1
  }
]
```

</details>

## 📋 Installation

1.  Download the latest version of the plugin.
2.  Place the `.jar` file into your server's `plugins` folder.
3.  Restart your server.
4.  Configure the plugin by editing `plugins/RougeStats/config.yml`.
5.  Restart the server again to apply changes.

## ❗ Dependencies

*   **Required:** Your server must be running the **Infinity Dungeons** map. This plugin is specifically designed to work with its mechanics.
*   **Holograms:** This plugin uses built-in `text_display` entities and does not require a separate hologram plugin.


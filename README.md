# PlatformChat

PlatformChat is a simple Minecraft Bukkit/Spigot plugin that provides enhanced chat formatting with support for Bedrock players (via Floodgate) and PlaceholderAPI integration. It allows configurable chat prefixes and color-coded messages for both Java and Bedrock editions.

---

## Features

- Different chat formats for Java and Bedrock players
- PlaceholderAPI support for dynamic placeholders
- Configurable prefixes and color codes via `config.yml`
- Supports Floodgate API to detect Bedrock players
- Uses color codes with `&` symbols translated into Minecraft colors

---

## Requirements

- Java 17+ (matches your server's Java version)
- Bukkit/Spigot/Paper Minecraft server (1.16+ recommended)
- [Floodgate plugin](https://github.com/GeyserMC/Floodgate) (for Bedrock player detection)
- [PlaceholderAPI plugin](https://github.com/PlaceholderAPI/PlaceholderAPI) (optional, for placeholder support)
- [FloodgatePlaceholders](https://github.com/rtm516/FloodgatePlaceholders) (recommended to get Floodgate placeholders working)

---

## Installation

1. Download or build the `PlatformChat.jar`.
2. Place the JAR file into your server's `plugins` folder.
3. Start the server once to generate the default config file.
4. Modify `plugins/PlatformChat/config.yml` to your liking (see Configuration section).
5. Restart or reload your server.

---

## Configuration (`config.yml`)

```yaml
use-placeholderapi: true
prefix: '&6[PlatformChat] '
bedrock-format: '&b[Bedrock] &a%player_name% &e%floodgate_device%&f: %message%'
java-format: '&7[Java] &a%player_name%&f: %message%'
````

* `use-placeholderapi`: Enable or disable PlaceholderAPI support.
* `prefix`: A string prefix prepended to all messages.
* `bedrock-format`: Chat format for Bedrock players. Use `%player_name%`, `%message%`, and Floodgate placeholders like `%floodgate_device%`.
* `java-format`: Chat format for Java players. Use `%player_name%` and `%message%`.

---

## Compilation

### Prerequisites

* JDK 17+
* Maven or Gradle (example below uses Maven)

### Steps with Maven

1. Place the source code in `src/main/java/com/aspaulding/platformchat/PlatformChat.java`.
2. Create a `pom.xml` including dependencies for Bukkit API, Floodgate API, and PlaceholderAPI.
3. Run:

```bash
mvn clean package
```

4. The compiled JAR will be in `target/PlatformChat.jar`.

---

## Usage

* Chat messages from Java and Bedrock players will be formatted differently based on your configuration.
* Placeholders from PlaceholderAPI and FloodgatePlaceholders will be parsed if enabled.
* Messages are color-coded according to the config and Minecraft’s color codes.

---

## Related Links

* [GeyserMC (Bedrock Proxy)](https://geysermc.org/)
* [PaperMC (Server software)](https://papermc.io/)
* [Floodgate (Bedrock auth plugin)](https://github.com/GeyserMC/Floodgate)
* [PlaceholderAPI (Placeholder plugin)](https://github.com/PlaceholderAPI/PlaceholderAPI)
* [FloodgatePlaceholders (Floodgate placeholders)](https://github.com/rtm516/FloodgatePlaceholders)

---

## Support

Found a bug or want to request a feature? Please open an issue on the GitHub repository.

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.


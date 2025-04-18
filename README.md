# Minecraft Public IP Alternative

This project provides a simple way to share your Minecraft server's public IP address with friends, even if your IP changes frequently (e.g., when using ngrok). It consists of:

- **A Python FastAPI server** that exposes your current ngrok tunnel address via a simple HTTP endpoint.
- **A Minecraft Fabric mod** that fetches the server's public IP from the FastAPI endpoint and automatically updates the multiplayer server list in your Minecraft client.

---

## Features

- **No more sharing new ngrok links every time you restart your server!**
- Automatically updates the Minecraft multiplayer list with the current public IP.
- Easy configuration via in-game GUI (requires [Cloth Config](https://www.curseforge.com/minecraft/mc-mods/cloth-config) and [ModMenu](https://www.curseforge.com/minecraft/mc-mods/modmenu)).
- Supports custom server names and fetch URLs.
- Multi-language support (English, Czech).

---

## Requirements

### Server (Python)

- Python 3.8+
- [fastapi](https://fastapi.tiangolo.com/)
- [uvicorn](https://www.uvicorn.org/)
- [pyngrok](https://github.com/alexdlaird/pyngrok)
- An [ngrok](https://ngrok.com/) account

Install dependencies:
```sh
pip install -r Server/requirements.txt
```

**Before running the server, add your ngrok auth token to your environment:**

1. Log in to your ngrok account and copy your auth token from the [ngrok dashboard](https://dashboard.ngrok.com/get-started/your-authtoken).
2. In your terminal, run:
   ```sh
   ngrok config add-authtoken "your-ngrok-auth-token"
   ```
   - Restart your terminal or computer if needed so the environment variable is available.

### Client (Minecraft Mod)

- Minecraft **1.21.1**
- [Fabric Loader](https://fabricmc.net/) **0.16.13+**
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Cloth Config API](https://www.curseforge.com/minecraft/mc-mods/cloth-config) **15.0.140+**
- [ModMenu](https://www.curseforge.com/minecraft/mc-mods/modmenu) **11.0.0+** (optional, for in-game config button)
- Java **21+**

---

## Usage

### 1. Start the Python Server

1. **Configure ngrok:**
   - Set a static ngrok domain in `Server/server.py` by editing `NGROK_STATIC_DOMAIN`.
   - Set your ngrok region if needed.

2. **Run the server:**
   ```sh
   cd Server
   python server.py
   ```
   - The server will print a public URL (e.g., `https://your-ngrok-domain.ngrok-free.app/ip`).

### 2. Build and Install the Minecraft Mod or Download it from the Releases Page

1. **Build the mod:**
   ```sh
   cd Client
   ./gradlew build
   ```
   - The mod JAR will be in `Client/build/libs/`.

2. **Install the mod:**
   - Place the JAR in your Minecraft `mods` folder.
   - Make sure you have Fabric Loader, Fabric API, Cloth Config, and ModMenu installed.

### 3. Configure the Mod In-Game

- Launch Minecraft.
- Go to Mods → Server Fetcher → Configure (requires ModMenu).
- Set:
  - **Server IP Fetch URL:** The URL printed by your Python server (e.g., `https://your-ngrok-domain.ngrok-free.app/ip`)
  - **Server Name:** The name you want to appear in your multiplayer list.

### 4. Play!

- Open the Multiplayer screen.
- The mod will automatically fetch the latest IP and update/add the server entry.
- Click to join your server—no more manual IP updates!

---

## File Structure

```
MinecraftPublicIPAlternative/
├── Client/        # Minecraft Fabric mod source
│   ├── src/
│   ├── build.gradle
│   └── ...
├── Server/        # Python FastAPI server
│   ├── server.py
│   └── requirements.txt
└── README.md
```

---

## Notes

- The mod supports English and Czech translations.
- The Python server uses ngrok to expose your local Minecraft server to the internet.
- For best results, use an ngrok static domain (requires a free ngrok account).
- The mod is open source under [CC0 1.0 Universal](https://creativecommons.org/publicdomain/zero/1.0/).

---

## Troubleshooting

- **Mod not updating server list?**
  - Check that the fetch URL is correct and accessible from your client.
  - Make sure the Python server is running and ngrok tunnel is active.
- **Python server not starting?**
  - Ensure all dependencies are installed.
  - Check your ngrok configuration.

---

## Credits

- Mod by [Tomesh](https://github.com/Thomioo)
- Uses [FabricMC](https://fabricmc.net/), [Cloth Config](https://github.com/shedaniel/cloth-config), [ModMenu](https://github.com/TerraformersMC/ModMenu), [FastAPI](https://fastapi.tiangolo.com/), [pyngrok](https://github.com/alexdlaird/pyngrok)

---

## License

This project is licensed under [CC0 1.0 Universal](Client/LICENSE).

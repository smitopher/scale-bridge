# scale-bridge

Bridges an **Adam CPWplus 75** bench scale to **OpenHAB** via MQTT.

Runs as a Podman container on a Raspberry Pi Zero 2 W, managed by systemd via a quadlet. The scale connects over USB via an FT232RL USB-serial adapter. Weight readings are published to a Mosquitto MQTT broker whenever the stable weight changes.

## MQTT topics

| Topic | Value |
|---|---|
| `scale/cpwplus75/weight` | Numeric weight (e.g. `"1.750"`) |
| `scale/cpwplus75/unit` | Unit (e.g. `"kg"`) |
| `scale/cpwplus75/status` | `"online"` / `"offline"` (LWT) |

The topic prefix is configurable.

## Quick start (local)

```bash
cp scale-bridge.properties.example scale-bridge.properties
# Edit scale-bridge.properties — set mqtt.host at minimum
./gradlew run
```

## Build & publish

Requires JDK 25. Authenticate to ghcr.io before publishing.

```bash
./gradlew build   # compile + test
./gradlew jib     # build linux/arm64 image and push to ghcr.io
```

See [CLAUDE.md](CLAUDE.md) for deployment and architecture details.

## License

CC0 1.0 Universal — public domain.

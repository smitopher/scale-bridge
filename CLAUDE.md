# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

scale-bridge is a Java daemon that reads weight data from an **Adam CPWplus 75** bench scale via a USB-serial adapter (FT232RL → `/dev/ttyUSB0`) and publishes readings to **Mosquitto MQTT** for consumption by **OpenHAB**. It runs as a Podman container on a Raspberry Pi Zero 2 W, managed by systemd via a Podman quadlet.

**Subprojects:**
- `app/` — the main scale-bridge service
- `proxy/` — development tool: exposes the serial port over TCP so the scale can be observed and tested remotely

## Build & Run

```bash
./gradlew build          # compile, test, Spotless check, SpotBugs (all subprojects)
./gradlew test           # tests only
./gradlew :app:run       # run scale-bridge locally (expects scale-bridge.properties in CWD)
./gradlew spotlessApply  # auto-format all Java source files
```

Run a single test class:
```bash
./gradlew test --tests "com.cjssolutions.scalebridge.SerialReaderTest"
```

Publish images to ghcr.io (requires prior `podman login ghcr.io`):
```bash
# Jib does not support Gradle configuration cache; --no-configuration-cache is required.
./gradlew :app:jib --no-configuration-cache
./gradlew :proxy:jib --no-configuration-cache
```

## Architecture

### app

```
App.java               Main loop: open scale → parse → on-change publish to MQTT
├── Config.java        Loads scale-bridge.properties
├── ScaleReader.java   Interface implemented by both reader types
├── SerialReader.java  jSerialComm reads /dev/ttyUSB0; parses CPWplus ASCII lines
├── TcpScaleReader.java  Connects to scale-bridge-proxy over TCP (dev mode)
│   └── WeightReading.java  Record: (value, unit, stable, raw)
└── MqttPublisher.java  Eclipse Paho client; publishes weight/unit/status topics
```

**Data flow:** The CPWplus streams ~20-char ASCII lines in continuous mode (e.g. `ASNG/W+ 1.75 kg`). `SerialReader.parse()` extracts value, unit, and stability via regex. Only stable readings that differ from the last published value by at least `bridge.change_threshold` are forwarded to MQTT.

**MQTT topics** (under `mqtt.topic_prefix`, default `scale/cpwplus75`):
- `/weight` — numeric value as a string (e.g. `"1.750"`)
- `/unit` — unit reported by scale (e.g. `"kg"`)
- `/status` — `"online"` / `"offline"` (LWT)

**TCP dev mode:** Set `serial.mode=tcp` and `serial.tcp.host=<rpi-ip>` to connect to the proxy instead of a local serial port. Allows full end-to-end dev testing without hardware attached to the dev machine.

### proxy

```
ProxyApp.java    Entry point
ProxyConfig.java Loads scale-bridge-proxy.properties
ScaleProxy.java  TCP server; bridges serial ↔ network (bidirectional, one client at a time)
```

Connect with `telnet <rpi-ip> 4567` or `nc <rpi-ip> 4567` to observe raw scale output and send commands (P, G, T). The proxy and main scale-bridge cannot run simultaneously — both need exclusive serial port access.

**Container:** `ghcr.io/smitopher/scale-bridge-proxy`, port 4567. Deploy via `scale-bridge-proxy.container` quadlet.

## Package

`com.cjssolutions.scalebridge`

## Configuration

Copy `scale-bridge.properties.example` → `scale-bridge.properties` and fill in `mqtt.host`. Set `serial.mode=tcp` and `serial.tcp.host` for dev mode via the proxy.

Copy `scale-bridge-proxy.properties.example` → `scale-bridge-proxy.properties` for the proxy.

## Releasing

```bash
git tag v1.2.3
git push origin v1.2.3
```

The release GitHub Actions workflow runs automatically: builds the image, pushes it to ghcr.io tagged as `1.2.3` and `latest`, and creates a GitHub release with auto-generated notes.

## Deployment (Pi)

```bash
# One-time setup
mkdir -p ~/.config/containers/systemd ~/scale-bridge
cp scale-bridge.properties ~/scale-bridge/scale-bridge.properties
cp scale-bridge.container ~/.config/containers/systemd/
systemctl --user daemon-reload && systemctl --user start scale-bridge

# For dev: swap to the proxy instead
cp scale-bridge-proxy.properties ~/scale-bridge/scale-bridge-proxy.properties
cp scale-bridge-proxy.container ~/.config/containers/systemd/
systemctl --user daemon-reload && systemctl --user start scale-bridge-proxy

# Updates
podman auto-update
```

The user running the container must be in the `dialout` group for `/dev/ttyUSB0` access.

## Static analysis

- **Spotless** — uses Google Java Format; run `./gradlew spotlessApply` to auto-format, `spotlessCheck` runs as part of `build`
- **SpotBugs** — config in `config/spotbugs/exclude.xml`; runs at `max` effort, `medium` report level; test failures are suppressed

## Key Dependencies

- `com.fazecast:jSerialComm` — cross-platform serial port access
- `org.eclipse.paho:org.eclipse.paho.client.mqttv3` — MQTT client
- `org.slf4j:slf4j-api` + `ch.qos.logback:logback-classic` — logging
- `com.google.cloud.tools.jib` — builds and publishes container images
- Java 25 toolchain / `eclipse-temurin:25-jre` base image

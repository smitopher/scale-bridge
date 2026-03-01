# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

scale-bridge is a Java daemon that reads weight data from an **Adam CPWplus 75** bench scale via a USB-serial adapter (FT232RL → `/dev/ttyUSB0`) and publishes readings to **Mosquitto MQTT** for consumption by **OpenHAB**. It runs as a Podman container on a Raspberry Pi Zero 2 W, managed by systemd via a Podman quadlet.

## Build & Run

```bash
./gradlew build          # compile, test, checkstyle, SpotBugs
./gradlew test           # tests only
./gradlew run            # run locally (expects scale-bridge.properties in CWD)
./gradlew check          # static analysis without compiling tests
```

Run a single test class:
```bash
./gradlew test --tests "com.cjssolutions.scalebridge.SerialReaderTest"
```

Publish image to ghcr.io (requires prior `podman login ghcr.io` or `docker login ghcr.io`):
```bash
./gradlew jib                        # tags with project version + latest
./gradlew jib -Pversion=1.2.3        # override version
```

## Architecture

```
App.java              Main loop: open serial → parse → on-change publish to MQTT
├── Config.java       Loads scale-bridge.properties
├── SerialReader.java jSerialComm reads /dev/ttyUSB0; parses CPWplus ASCII lines
│   └── WeightReading.java  Record: (value, unit, stable, raw)
└── MqttPublisher.java      Eclipse Paho client; publishes weight/unit/status topics
```

**Data flow:** The CPWplus streams ~20-char ASCII lines in continuous mode (e.g. `ASNG/W+ 1.75 kg`). `SerialReader.parse()` extracts value, unit, and stability via regex. Only stable readings that differ from the last published value by at least `bridge.change_threshold` are forwarded to MQTT.

**MQTT topics** (under `mqtt.topic_prefix`, default `scale/cpwplus75`):
- `/weight` — numeric value as a string (e.g. `"1.750"`)
- `/unit` — unit reported by scale (e.g. `"kg"`)
- `/status` — `"online"` / `"offline"` (LWT)

**Container:** Jib builds a `linux/arm64` OCI image from `eclipse-temurin:25-jre` and pushes to `ghcr.io/smitopher/scale-bridge`. The config file is mounted at `/config/scale-bridge.properties` at runtime.

## Package

`com.cjssolutions.scalebridge`

## Configuration

Copy `scale-bridge.properties.example` → `scale-bridge.properties` and fill in `mqtt.host`. All other defaults work for a stock CPWplus 75.

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
# edit ~/scale-bridge/scale-bridge.properties
cp scale-bridge.container ~/.config/containers/systemd/
systemctl --user daemon-reload

# Start
systemctl --user start scale-bridge

# Updates: after publishing a new image from dev machine
podman auto-update
```

The user running the container must be in the `dialout` group for `/dev/ttyUSB0` access.

## Static analysis

- **Checkstyle** — config in `config/checkstyle/checkstyle.xml`; enforces naming, import hygiene, line length (120), no tabs, braces required
- **SpotBugs** — config in `config/spotbugs/exclude.xml`; runs at `max` effort, `medium` report level; test failures are suppressed

## Key Dependencies

- `com.fazecast:jSerialComm` — cross-platform serial port access
- `org.eclipse.paho:org.eclipse.paho.client.mqttv3` — MQTT client
- `org.slf4j:slf4j-api` + `ch.qos.logback:logback-classic` — logging
- `com.google.cloud.tools.jib` — builds and publishes the container image
- Java 25 toolchain / `eclipse-temurin:25-jre` base image

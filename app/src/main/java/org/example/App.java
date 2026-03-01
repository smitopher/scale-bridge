/*
 * SPDX-License-Identifier: CC0-1.0
 */
package org.example;

import java.util.logging.Logger;

/**
 * scale-bridge — bridges an Adam CPWplus 75 scale (via USB-serial) to MQTT.
 *
 * <p>Usage: {@code scale-bridge [/path/to/scale-bridge.properties]}
 * Defaults to {@code scale-bridge.properties} in the working directory.
 */
public class App {

    private static final Logger LOG            = Logger.getLogger(App.class.getName());
    private static final int    RETRY_DELAY_MS = 5_000;

    public static void main(String[] args) {
        String configPath = args.length > 0 ? args[0] : "scale-bridge.properties";

        Config config;
        try {
            config = new Config(configPath);
        } catch (Exception e) {
            System.err.println("Failed to load config from " + configPath + ": " + e.getMessage());
            System.exit(1);
            return;
        }

        try (MqttPublisher publisher = new MqttPublisher(config)) {
            publisher.connect();
            runBridge(config, publisher);
        } catch (Exception e) {
            LOG.severe("Fatal error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void runBridge(Config config, MqttPublisher publisher) {
        Double lastValue = null;

        while (true) {
            try (SerialReader reader = new SerialReader(config)) {
                reader.open();

                while (true) {
                    WeightReading reading = reader.readLine();

                    if (reading == null) {
                        continue;
                    }
                    if (config.stableOnly && !reading.stable()) {
                        LOG.fine(() -> "Skipping unstable: " + reading.raw());
                        continue;
                    }

                    boolean changed = lastValue == null
                            || Math.abs(reading.value() - lastValue) >= config.changeThreshold;

                    if (changed) {
                        publisher.publishReading(reading);
                        lastValue = reading.value();
                    }
                }

            } catch (Exception e) {
                LOG.warning("Serial error: %s — retrying in %ds".formatted(
                        e.getMessage(), RETRY_DELAY_MS / 1000));
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}

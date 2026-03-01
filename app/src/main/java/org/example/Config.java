/*
 * SPDX-License-Identifier: CC0-1.0
 */
package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Application configuration loaded from a .properties file.
 * All fields are immutable after construction.
 */
public class Config {

    // Serial port
    public final String serialPort;
    public final int    serialBaud;

    // MQTT broker
    public final String mqttHost;
    public final int    mqttPort;
    public final String mqttUsername;
    public final String mqttPassword;
    public final String mqttClientId;
    public final String mqttTopicPrefix;
    public final boolean mqttRetain;

    // Bridge behaviour
    public final double  changeThreshold;
    public final boolean stableOnly;

    public Config(String path) throws IOException {
        Properties p = new Properties();
        try (var in = new FileInputStream(path)) {
            p.load(in);
        }

        serialPort  = p.getProperty("serial.port",  "/dev/ttyUSB0");
        serialBaud  = Integer.parseInt(p.getProperty("serial.baud", "9600"));

        mqttHost        = p.getProperty("mqtt.host",         "localhost");
        mqttPort        = Integer.parseInt(p.getProperty("mqtt.port", "1883"));
        mqttUsername    = p.getProperty("mqtt.username",     "");
        mqttPassword    = p.getProperty("mqtt.password",     "");
        mqttClientId    = p.getProperty("mqtt.client_id",    "scale-bridge");
        mqttTopicPrefix = p.getProperty("mqtt.topic_prefix", "scale/cpwplus75");
        mqttRetain      = Boolean.parseBoolean(p.getProperty("mqtt.retain", "true"));

        changeThreshold = Double.parseDouble(p.getProperty("bridge.change_threshold", "0.01"));
        stableOnly      = Boolean.parseBoolean(p.getProperty("bridge.stable_only",    "true"));
    }
}

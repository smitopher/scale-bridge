/*
 * SPDX-License-Identifier: CC0-1.0
 */
package com.cjssolutions.scalebridge;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes weight readings to an MQTT broker using Eclipse Paho.
 *
 * <p>Topics published (all under {@code mqtt.topic_prefix}):
 *
 * <ul>
 *   <li>{prefix}/weight — numeric weight value as a string
 *   <li>{prefix}/unit — unit reported by the scale (e.g. "kg")
 *   <li>{prefix}/status — "online" while running; LWT publishes "offline"
 * </ul>
 */
public class MqttPublisher implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(MqttPublisher.class);

  private final Config config;
  private final MqttClient client;

  public MqttPublisher(Config config) throws MqttException {
    this.config = config;
    String brokerUrl = "tcp://%s:%d".formatted(config.mqttHost, config.mqttPort);
    client = new MqttClient(brokerUrl, config.mqttClientId);
  }

  /** Connects to the broker. Automatic reconnection is enabled. */
  public void connect() throws MqttException {
    MqttConnectOptions opts = new MqttConnectOptions();
    opts.setAutomaticReconnect(true);
    opts.setCleanSession(true);

    if (!config.mqttUsername.isBlank()) {
      opts.setUserName(config.mqttUsername);
      opts.setPassword(config.mqttPassword.toCharArray());
    }

    // Last-will: mark as offline if the connection drops unexpectedly.
    opts.setWill(statusTopic(), "offline".getBytes(), 1, true);

    client.connect(opts);
    LOG.info("MQTT connected to {}:{}", config.mqttHost, config.mqttPort);

    publish(statusTopic(), "online", 1, true);
  }

  /** Publishes a weight reading (weight + unit topics). */
  public void publishReading(WeightReading reading) throws MqttException {
    String weightStr = "%.3f".formatted(reading.value());
    publish(config.mqttTopicPrefix + "/weight", weightStr, 0, config.mqttRetain);
    publish(config.mqttTopicPrefix + "/unit", reading.unit(), 0, config.mqttRetain);
    LOG.info("Published: {} {}", weightStr, reading.unit());
  }

  private void publish(String topic, String payload, int qos, boolean retain) throws MqttException {
    MqttMessage msg = new MqttMessage(payload.getBytes());
    msg.setQos(qos);
    msg.setRetained(retain);
    client.publish(topic, msg);
  }

  private String statusTopic() {
    return config.mqttTopicPrefix + "/status";
  }

  @Override
  public void close() {
    try {
      if (client.isConnected()) {
        publish(statusTopic(), "offline", 1, true);
        client.disconnect();
      }
      client.close();
    } catch (MqttException e) {
      LOG.warn("Error closing MQTT client: {}", e.getMessage());
    }
  }
}

/*
 * SPDX-License-Identifier: CC0-1.0
 */
package com.cjssolutions.scalebridge.proxy;

/**
 * scale-bridge-proxy — exposes the CPWplus scale serial port over TCP.
 *
 * <p>Usage: {@code scale-bridge-proxy [/path/to/scale-bridge-proxy.properties]}
 */
public class ProxyApp {

  public static void main(String[] args) {
    String configPath = args.length > 0 ? args[0] : "scale-bridge-proxy.properties";

    ProxyConfig config;
    try {
      config = new ProxyConfig(configPath);
    } catch (Exception e) {
      System.err.println("Failed to load config from " + configPath + ": " + e.getMessage());
      System.exit(1);
      return;
    }

    try {
      new ScaleProxy(config).run();
    } catch (Exception e) {
      System.err.println("Fatal error: " + e.getMessage());
      System.exit(1);
    }
  }
}

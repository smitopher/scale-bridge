/*
 * SPDX-License-Identifier: CC0-1.0
 */
package com.cjssolutions.scalebridge.proxy;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/** Configuration for the scale proxy, loaded from a .properties file. */
public final class ProxyConfig {

  public final String serialPort;
  public final int serialBaud;
  public final int proxyPort;

  public ProxyConfig(String path) throws IOException {
    Properties p = new Properties();
    try (var in = new FileInputStream(path)) {
      p.load(in);
    }

    serialPort = p.getProperty("serial.port", "/dev/ttyUSB0");
    serialBaud = Integer.parseInt(p.getProperty("serial.baud", "9600"));
    proxyPort = Integer.parseInt(p.getProperty("proxy.port", "4567"));
  }
}

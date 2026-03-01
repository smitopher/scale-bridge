/*
 * SPDX-License-Identifier: CC0-1.0
 */
package com.cjssolutions.scalebridge.proxy;

import com.fazecast.jSerialComm.SerialPort;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP server that bridges a CPWplus scale serial port to the network.
 *
 * <p>Accepts one client at a time. All bytes from the scale are forwarded to the connected client,
 * and all bytes from the client are forwarded to the scale. This allows remote observation and
 * control (e.g. sending P/G/T commands) over a plain TCP connection.
 *
 * <p>Connect with: {@code telnet <rpi-ip> 4567} or {@code nc <rpi-ip> 4567}
 */
public class ScaleProxy {

  private static final Logger LOG = LoggerFactory.getLogger(ScaleProxy.class);
  private static final int BUFFER_SIZE = 256;

  private final ProxyConfig config;

  public ScaleProxy(ProxyConfig config) {
    this.config = config;
  }

  /** Runs the proxy, blocking indefinitely. Accepts one client at a time. */
  public void run() throws IOException {
    try (ServerSocket server = new ServerSocket(config.proxyPort)) {
      LOG.info("Scale proxy listening on port {}", config.proxyPort);
      LOG.info("Connect with: telnet <host> {}", config.proxyPort);

      while (true) {
        Socket client = server.accept();
        LOG.info("Client connected: {}", client.getRemoteSocketAddress());
        try {
          bridge(client);
        } catch (Exception e) {
          LOG.info("Client disconnected: {}", e.getMessage());
        } finally {
          client.close();
        }
      }
    }
  }

  private void bridge(Socket client) throws IOException, InterruptedException {
    SerialPort serial = SerialPort.getCommPort(config.serialPort);
    serial.setBaudRate(config.serialBaud);
    serial.setNumDataBits(8);
    serial.setParity(SerialPort.NO_PARITY);
    serial.setNumStopBits(SerialPort.ONE_STOP_BIT);
    serial.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 2000, 0);

    if (!serial.openPort()) {
      throw new IOException("Failed to open serial port: " + config.serialPort);
    }
    LOG.info("Opened {} at {} baud", config.serialPort, config.serialBaud);

    try {
      InputStream serialIn = serial.getInputStream();
      OutputStream serialOut = serial.getOutputStream();
      InputStream clientIn = client.getInputStream();
      OutputStream clientOut = client.getOutputStream();

      // Forward scale → client on a virtual thread.
      Thread scaleToClient =
          Thread.ofVirtual()
              .start(
                  () -> {
                    try {
                      byte[] buf = new byte[BUFFER_SIZE];
                      int n;
                      while ((n = serialIn.read(buf)) != -1) {
                        clientOut.write(buf, 0, n);
                        clientOut.flush();
                        LOG.debug(
                            "scale → client: {}",
                            new String(buf, 0, n, StandardCharsets.US_ASCII).stripTrailing());
                      }
                    } catch (IOException e) {
                      LOG.debug("scale → client thread ending: {}", e.getMessage());
                    }
                  });

      // Forward client → scale on the current thread.
      try {
        byte[] buf = new byte[BUFFER_SIZE];
        int n;
        while ((n = clientIn.read(buf)) != -1) {
          serialOut.write(buf, 0, n);
          LOG.debug(
              "client → scale: {}",
              new String(buf, 0, n, StandardCharsets.US_ASCII).stripTrailing());
        }
      } finally {
        scaleToClient.interrupt();
        scaleToClient.join(1000);
      }

    } finally {
      serial.closePort();
      LOG.info("Closed serial port");
    }
  }
}

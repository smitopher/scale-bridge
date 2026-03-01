/*
 * SPDX-License-Identifier: CC0-1.0
 */
package com.cjssolutions.scalebridge;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads weight data from a scale-bridge-proxy over TCP, for use during development when the scale
 * is connected to a remote Raspberry Pi.
 *
 * <p>Configure with {@code serial.mode=tcp}, {@code serial.tcp.host}, and {@code serial.tcp.port}.
 * Lines are parsed using the same {@link SerialReader#parse} logic as the direct serial reader.
 */
public class TcpScaleReader implements ScaleReader {

  private static final Logger LOG = LoggerFactory.getLogger(TcpScaleReader.class);

  private final String host;
  private final int port;
  private Socket socket;
  private BufferedReader reader;

  public TcpScaleReader(String host, int port) {
    this.host = host;
    this.port = port;
  }

  @Override
  public void open() throws Exception {
    socket = new Socket(host, port);
    reader =
        new BufferedReader(
            new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
    LOG.info("Connected to scale proxy at {}:{}", host, port);
  }

  @Override
  public WeightReading readLine() throws Exception {
    String line = reader.readLine();
    if (line == null || line.isBlank()) {
      return null;
    }
    return SerialReader.parse(line);
  }

  @Override
  public void close() throws Exception {
    if (socket != null && !socket.isClosed()) {
      socket.close();
    }
  }
}

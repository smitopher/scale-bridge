/*
 * SPDX-License-Identifier: CC0-1.0
 */
package com.cjssolutions.scalebridge;

import com.fazecast.jSerialComm.SerialPort;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads lines from an Adam CPWplus scale via a USB-serial adapter and parses them into {@link
 * WeightReading} instances.
 *
 * <p>The CPWplus continuous output format is a ~20-character ASCII string, e.g.:
 *
 * <pre>
 *   ASNG/W+ 1.75 kg
 * </pre>
 *
 * The leading status characters indicate mode and stability; a 'U' in that prefix indicates an
 * unstable reading. A regex extracts sign, numeric value, and unit regardless of exact framing.
 */
public class SerialReader implements ScaleReader {

  private static final Logger LOG = LoggerFactory.getLogger(SerialReader.class);

  /** Matches optional sign, a decimal number, and a weight unit. */
  private static final Pattern WEIGHT_RE =
      Pattern.compile("([+-]?)\\s*(\\d+\\.?\\d*)\\s*(kg|lb|g|oz)\\b", Pattern.CASE_INSENSITIVE);

  private final Config config;
  private SerialPort port;
  private BufferedReader reader;

  public SerialReader(Config config) {
    this.config = config;
  }

  /** Opens the serial port. Call before {@link #readLine()}. */
  public void open() {
    port = SerialPort.getCommPort(config.serialPort);
    port.setBaudRate(config.serialBaud);
    port.setNumDataBits(8);
    port.setParity(SerialPort.NO_PARITY);
    port.setNumStopBits(SerialPort.ONE_STOP_BIT);
    // Semi-blocking read: returns after data arrives or 2-second timeout.
    port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 2000, 0);

    if (!port.openPort()) {
      throw new RuntimeException("Failed to open serial port: " + config.serialPort);
    }

    reader =
        new BufferedReader(new InputStreamReader(port.getInputStream(), StandardCharsets.US_ASCII));

    LOG.info("Opened {} at {} baud", config.serialPort, config.serialBaud);
  }

  /**
   * Reads the next line from the serial port and attempts to parse it.
   *
   * @return a {@link WeightReading}, or {@code null} if the line timed out or could not be parsed.
   * @throws Exception on unrecoverable I/O errors.
   */
  public WeightReading readLine() throws Exception {
    String line = reader.readLine();
    if (line == null || line.isBlank()) {
      return null;
    }
    return parse(line);
  }

  /** Parses a raw CPWplus line into a {@link WeightReading}. Package-private for unit testing. */
  static WeightReading parse(String line) {
    Matcher m = WEIGHT_RE.matcher(line);
    if (!m.find()) {
      LOG.debug("No weight found in: {}", line);
      return null;
    }

    double value;
    try {
      value = Double.parseDouble(m.group(2));
    } catch (NumberFormatException e) {
      return null;
    }
    if ("-".equals(m.group(1))) {
      value = -value;
    }

    String unit = m.group(3).toLowerCase();

    // Stability heuristic: 'U' in the status prefix indicates unstable.
    String prefix = line.substring(0, m.start()).toUpperCase();
    boolean stable = !prefix.contains("U") && !prefix.contains("?");

    return new WeightReading(value, unit, stable, line);
  }

  @Override
  public void close() {
    if (port != null && port.isOpen()) {
      port.closePort();
    }
  }
}

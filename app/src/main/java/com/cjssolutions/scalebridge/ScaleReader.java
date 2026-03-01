/*
 * SPDX-License-Identifier: CC0-1.0
 */
package com.cjssolutions.scalebridge;

/**
 * Common interface for reading weight data from a CPWplus scale, regardless of whether the
 * connection is a local serial port or a remote TCP proxy.
 */
public interface ScaleReader extends AutoCloseable {

  /** Opens the underlying connection. Must be called before {@link #readLine()}. */
  void open() throws Exception;

  /**
   * Reads and parses the next weight line.
   *
   * @return a {@link WeightReading}, or {@code null} on timeout or unparseable input.
   * @throws Exception on unrecoverable connection errors.
   */
  WeightReading readLine() throws Exception;
}

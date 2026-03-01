/*
 * SPDX-License-Identifier: CC0-1.0
 */
package com.cjssolutions.scalebridge;

/**
 * A single weight reading parsed from the CPWplus serial output.
 *
 * @param value  Numeric weight value (negative for below-zero).
 * @param unit   Unit string as reported by the scale (e.g. "kg", "lb").
 * @param stable True if the scale reported a stable reading.
 * @param raw    The original line received from the serial port.
 */
public record WeightReading(double value, String unit, boolean stable, String raw) {}

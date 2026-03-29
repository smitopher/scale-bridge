package com.cjssolutions.scalebridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SerialReaderTest {

  @Test
  void parsesStableKgReading() {
    WeightReading r = SerialReader.parse("ASNG/W+ 1.75 kg");
    assertNotNull(r);
    assertEquals(1.75, r.value(), 0.001);
    assertEquals("kg", r.unit());
    assertTrue(r.stable());
  }

  @Test
  void parsesNegativeReading() {
    WeightReading r = SerialReader.parse("ASNG/W- 0.50 kg");
    assertNotNull(r);
    assertEquals(-0.50, r.value(), 0.001);
  }

  @Test
  void detectsUnstableReading() {
    WeightReading r = SerialReader.parse("AUNG/W+ 1.75 kg");
    assertNotNull(r);
    assertFalse(r.stable());
  }

  @Test
  void parsesLbUnit() {
    WeightReading r = SerialReader.parse("ASNG/W+ 3.86 lb");
    assertNotNull(r);
    assertEquals("lb", r.unit());
  }

  @Test
  void parsesOzUnitWithIntegerValue() {
    WeightReading r = SerialReader.parse("ASNG/W+    12  oz");
    assertNotNull(r);
    assertEquals(12.0, r.value(), 0.001);
    assertEquals("oz", r.unit());
    assertTrue(r.stable());
  }

  @Test
  void returnsNullForGarbage() {
    assertNull(SerialReader.parse("no weight here"));
  }

  @Test
  void returnsNullForBlankLine() {
    assertNull(SerialReader.parse("   "));
  }
}

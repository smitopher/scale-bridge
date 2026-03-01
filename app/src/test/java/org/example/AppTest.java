package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
    void returnsNullForGarbage() {
        assertNull(SerialReader.parse("no weight here"));
    }

    @Test
    void returnsNullForBlankLine() {
        assertNull(SerialReader.parse("   "));
    }
}

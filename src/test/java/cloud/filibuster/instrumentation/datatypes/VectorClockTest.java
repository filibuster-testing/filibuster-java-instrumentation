package cloud.filibuster.instrumentation.datatypes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class VectorClockTest {
    private VectorClock vc;

    @BeforeEach
    public void setUp() {
        vc = new VectorClock();
    }

    @Test
    @DisplayName("Test incrementing a clock.")
    public void testIncrementClock() {
        String key = "chris";
        vc.incrementClock(key);
        assertEquals(1, vc.get(key));
    }

    @Test
    @DisplayName("Test incrementing a clock twice.")
    public void testIncrementClockTwice() {
        String key = "chris";
        vc.incrementClock(key);
        assertEquals(1, vc.get(key));
        vc.incrementClock(key);
        assertEquals(2, vc.get(key));
    }

    @Test
    @DisplayName("Test get non-incremented key.")
    public void testGetNonIncrementedKey() {
        String key = "chris";
        assertEquals(0, vc.get(key));
    }

    @Test
    @DisplayName("Test key merge.")
    public void testKeyMerge() {
        VectorClock vc1 = new VectorClock();
        VectorClock vc2 = new VectorClock();

        vc1.incrementClock("chris");
        vc1.incrementClock("bob");
        vc2.incrementClock("chris");
        vc2.incrementClock("alice");
        vc2.incrementClock("chris");

        vc = VectorClock.merge(vc1, vc2);
        assertEquals(2, vc.get("chris"));
        assertEquals(1, vc.get("alice"));
        assertEquals(1, vc.get("bob"));
    }

    @Test
    @DisplayName("Test incrementing and string generation.")
    public void testToString() {
        String key = "chris";
        vc.incrementClock(key);
        assertEquals("{\"chris\":1}", vc.toString());
    }

    @Test
    @DisplayName("Test parse.")
    public void testFromString() {
        String key = "chris";
        String serialized = "{\"chris\":1}";
        vc.fromString(serialized);
        assertEquals(1, vc.get(key));
    }

    @Test
    @DisplayName("Test parse.")
    public void testDescends() {
        VectorClock descendedVc = new VectorClock();
        descendedVc.incrementClock("chris");
        assertTrue(VectorClock.descends(vc, descendedVc));
        assertFalse(VectorClock.descends(descendedVc, vc));
        assertFalse(VectorClock.descends(vc, vc));
        assertFalse(VectorClock.descends(descendedVc, descendedVc));
        vc.incrementClock("chris");
        descendedVc.incrementClock("chris");
        assertTrue(VectorClock.descends(vc, descendedVc));
        assertFalse(VectorClock.descends(descendedVc, vc));
        assertTrue(VectorClock.descends(null, vc));
        assertFalse(VectorClock.descends(vc, null));
        assertFalse(VectorClock.descends(null, null));
        assertFalse(VectorClock.descends(vc, vc));
    }
}

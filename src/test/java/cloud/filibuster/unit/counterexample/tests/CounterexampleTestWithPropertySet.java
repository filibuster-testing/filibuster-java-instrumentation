package cloud.filibuster.unit.counterexample.tests;

import cloud.filibuster.instrumentation.helpers.Property;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static cloud.filibuster.instrumentation.helpers.Counterexample.canLoadCounterexample;
import static cloud.filibuster.instrumentation.helpers.Counterexample.loadCounterexampleAsJSONObjectFromEnvironment;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CounterexampleTestWithPropertySet {
    @BeforeEach
    public void setCounterexampleProperty() {
        Property.setInstrumentationCounterexampleFileProperty("counterexample.json");
    }

    @AfterEach
    public void unsetCounterexampleProperty() {
        Property.setInstrumentationCounterexampleFileProperty("");
    }

    @Test
    @DisplayName("Test loading a counterexample to JSON from the environment.")
    public void testCounterexampleJSONObjectLoadFromEnvironment() {
        JSONObject counterexample = loadCounterexampleAsJSONObjectFromEnvironment();
        assertNotNull(counterexample);
    }

    @Test
    @DisplayName("Test counterexample helper.")
    public void testCanLoadCounterexample() {
        assertTrue(canLoadCounterexample());
    }
}

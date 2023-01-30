package cloud.filibuster.unit.counterexample.tests;

import cloud.filibuster.instrumentation.exceptions.EnvironmentMissingCounterexampleException;
import cloud.filibuster.instrumentation.helpers.Property;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static cloud.filibuster.instrumentation.helpers.Counterexample.canLoadCounterexample;
import static cloud.filibuster.instrumentation.helpers.Counterexample.loadCounterexampleAsJSONObjectFromEnvironment;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CounterexampleTestWithPropertyUnset {
    @BeforeEach
    public void setCounterexampleProperty() {
        Property.setInstrumentationCounterexampleFileProperty("");
    }

    @Test
    @DisplayName("Test fail loading a counterexample to JSON from the environment.")
    public void testFailedCounterexampleJSONObjectLoadFromEnvironment() {
        assertThrows(EnvironmentMissingCounterexampleException.class, () -> {
            loadCounterexampleAsJSONObjectFromEnvironment();
        });
    }

    @Test
    @DisplayName("Test fail counterexample helper.")
    public void testFailCanLoadCounterexample() {
        assertFalse(canLoadCounterexample());
    }
}

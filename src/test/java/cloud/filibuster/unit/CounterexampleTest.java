package cloud.filibuster.unit;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static cloud.filibuster.instrumentation.helpers.Counterexample.loadCounterexampleAsJSONObject;
import static cloud.filibuster.instrumentation.helpers.Counterexample.loadTestExecutionFromCounterexample;
import static cloud.filibuster.instrumentation.helpers.Counterexample.shouldFailRequestWith;
import static cloud.filibuster.instrumentation.helpers.Counterexample.shouldFailRequestWithOrDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CounterexampleTest {
    private static final String validDistributedExecutionIndex = "[[\"befdb58c6e8809aeb1a0e954bc665ca3\", 1], [\"5c172d74ea9e37459ad3bb923e92c6c2\", 1]]";

    private static final String invalidDistributedExecutionIndex = "[[\"befdb58c6e8809aeb1a0e954bc665ca3\", 1]]";

    @Test
    @DisplayName("Test loading a counterexample to JSON.")
    public void testCounterexampleJSONObjectLoad() {
        JSONObject counterexample = loadCounterexampleAsJSONObject("counterexample.json");
        assertNotNull(counterexample);
    }

    @Test
    @DisplayName("Test loading a test execution from a counterexample.")
    public void testTestExecutionLoadFromCounterexample() {
        JSONObject counterexample = loadCounterexampleAsJSONObject("counterexample.json");
        JSONObject testExecution = loadTestExecutionFromCounterexample(counterexample);
        assertNotNull(testExecution);
    }

    @Test
    @DisplayName("Test loading a value from the test execution in the counterexample.")
    public void testShouldFailRequestWithValidDistributedExecutionIndex() {
        JSONObject counterexample = loadCounterexampleAsJSONObject("counterexample.json");
        JSONObject testExecution = loadTestExecutionFromCounterexample(counterexample);
        JSONObject response = shouldFailRequestWith(validDistributedExecutionIndex, testExecution);

        assertNotNull(response);

        JSONObject forcedException = response.getJSONObject("forced_exception");
        assertEquals("requests.exceptions.ConnectionError", forcedException.getString("name"));

        assertEquals(validDistributedExecutionIndex, response.getString("execution_index"));
    }

    @Test
    @DisplayName("Test failing to load a value from the test execution in the counterexample.")
    public void testShouldFailRequestWithInvalidDistributedExecutionIndex() {
        JSONObject counterexample = loadCounterexampleAsJSONObject("counterexample.json");
        JSONObject testExecution = loadTestExecutionFromCounterexample(counterexample);
        JSONObject response = shouldFailRequestWith(invalidDistributedExecutionIndex, testExecution);
        assertNull(response);
    }

    @Test
    @DisplayName("Test failing to load a value from the test execution in the counterexample with default.")
    public void testShouldFailRequestWithInvalidDistributedExecutionIndexAndDefault() {
        JSONObject counterexample = loadCounterexampleAsJSONObject("counterexample.json");
        JSONObject testExecution = loadTestExecutionFromCounterexample(counterexample);
        JSONObject response = shouldFailRequestWithOrDefault(invalidDistributedExecutionIndex, testExecution);

        assertNotNull(response);
        assertEquals(invalidDistributedExecutionIndex, response.getString("execution_index"));
    }
}

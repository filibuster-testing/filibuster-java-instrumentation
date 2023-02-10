package cloud.filibuster.unit;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.dei.implementations.DistributedExecutionIndexV1;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.junit.server.core.test_executions.AbstractTestExecution;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TestExecutionTest {
    @Test
    public void testEqualityOfEmpty() {
        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();
        assertEquals(pe1, pe2);
    }

    // Base tests.

    @Test
    public void testSameDistributedExecutionIndexWithSameJSONObject() {
        Callsite callsite = new Callsite("service", "klass", "theMethodName", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex = new DistributedExecutionIndexV1();
        distributedExecutionIndex.push(callsite);

        JSONObject jsonObject = new JSONObject();

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, jsonObject);
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, jsonObject);
        assertEquals(pe1, pe2);
    }

    @Test
    public void testSimilarDistributedExecutionIndexWithSameJSONObject() {
        Callsite callsite = new Callsite("service", "klass", "theMethodName", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex1 = new DistributedExecutionIndexV1();
        distributedExecutionIndex1.push(callsite);

        DistributedExecutionIndex distributedExecutionIndex2 = new DistributedExecutionIndexV1();
        distributedExecutionIndex2.push(callsite);

        assertEquals(distributedExecutionIndex1, distributedExecutionIndex2);

        JSONObject jsonObject = new JSONObject();

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex1, jsonObject);
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex2, jsonObject);
        assertEquals(pe1, pe2);
    }

    @Test
    public void testSameDistributedExecutionIndexWithSimilarJSONObject() {
        Callsite callsite = new Callsite("service", "klass", "theMethodName", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex = new DistributedExecutionIndexV1();
        distributedExecutionIndex.push(callsite);

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, new JSONObject());
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, new JSONObject());
        assertEquals(pe1, pe2);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("a", "b");

        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, jsonObject);
        assertNotEquals(pe1, pe2);
    }

    @Test
    public void testSimilarDistributedExecutionIndexWithSimilarJSONObject() {
        Callsite callsite = new Callsite("service", "klass", "theMethodName", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex1 = new DistributedExecutionIndexV1();
        distributedExecutionIndex1.push(callsite);

        DistributedExecutionIndex distributedExecutionIndex2 = new DistributedExecutionIndexV1();
        distributedExecutionIndex2.push(callsite);

        assertEquals(distributedExecutionIndex1, distributedExecutionIndex2);

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex1, new JSONObject());
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex2, new JSONObject());
        assertEquals(pe1, pe2);
    }

    @Test
    public void testMultipleSameDistributedExecutionIndexWithSameJSONObject() {
        Callsite callsite1 = new Callsite("service", "klass", "methodOne", "deadbeef");
        Callsite callsite2 = new Callsite("service", "klass", "methodTwo", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex = new DistributedExecutionIndexV1();
        distributedExecutionIndex.push(callsite1);
        distributedExecutionIndex.push(callsite2);

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        JSONObject jsonObject = new JSONObject();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, jsonObject);
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, jsonObject);
        assertEquals(pe1, pe2);
    }

    @Test
    public void testMultipleSameDistributedExecutionIndexWithSimilarJSONObject() {
        Callsite callsite1 = new Callsite("service", "klass", "methodOne", "deadbeef");
        Callsite callsite2 = new Callsite("service", "klass", "methodTwo", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex = new DistributedExecutionIndexV1();
        distributedExecutionIndex.push(callsite1);
        distributedExecutionIndex.push(callsite2);

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, new JSONObject());
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, new JSONObject());
        assertEquals(pe1, pe2);
    }

    @Test
    public void testMultipleSimilarDistributedExecutionIndexWithSameJSONObject() {
        Callsite callsite1 = new Callsite("service", "klass", "methodOne", "deadbeef");
        Callsite callsite2 = new Callsite("service", "klass", "methodTwo", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex1 = new DistributedExecutionIndexV1();
        distributedExecutionIndex1.push(callsite1);
        distributedExecutionIndex1.push(callsite2);

        DistributedExecutionIndex distributedExecutionIndex2 = new DistributedExecutionIndexV1();
        distributedExecutionIndex2.push(callsite1);
        distributedExecutionIndex2.push(callsite2);

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        JSONObject jsonObject = new JSONObject();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex1, jsonObject);
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex2, jsonObject);
        assertEquals(pe1, pe2);
    }

    @Test
    public void testMultipleSimilarDistributedExecutionIndexWithSimilarJSONObject() {
        Callsite callsite1 = new Callsite("service", "klass", "methodOne", "deadbeef");
        Callsite callsite2 = new Callsite("service", "klass", "methodTwo", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex1 = new DistributedExecutionIndexV1();
        distributedExecutionIndex1.push(callsite1);
        distributedExecutionIndex1.push(callsite2);

        DistributedExecutionIndex distributedExecutionIndex2 = new DistributedExecutionIndexV1();
        distributedExecutionIndex2.push(callsite1);
        distributedExecutionIndex2.push(callsite2);

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex1, new JSONObject());
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex2, new JSONObject());
        assertEquals(pe1, pe2);
    }

    // Including faults to inject as well as dei.

    @Test
    public void testSameDistributedExecutionIndexWithSameJSONObjectWithFaultsToInject() {
        Callsite callsite = new Callsite("service", "klass", "theMethodName", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex = new DistributedExecutionIndexV1();
        distributedExecutionIndex.push(callsite);

        JSONObject jsonObject = new JSONObject();

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, jsonObject);
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, jsonObject);

        pe1.addFaultToInject(distributedExecutionIndex, jsonObject);
        pe2.addFaultToInject(distributedExecutionIndex, jsonObject);

        assertEquals(pe1, pe2);
    }

    @Test
    public void testSimilarDistributedExecutionIndexWithSameJSONObjectWithFaultsToInject() {
        Callsite callsite = new Callsite("service", "klass", "theMethodName", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex1 = new DistributedExecutionIndexV1();
        distributedExecutionIndex1.push(callsite);

        DistributedExecutionIndex distributedExecutionIndex2 = new DistributedExecutionIndexV1();
        distributedExecutionIndex2.push(callsite);

        assertEquals(distributedExecutionIndex1, distributedExecutionIndex2);

        JSONObject jsonObject = new JSONObject();

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex1, jsonObject);
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex2, jsonObject);

        pe1.addFaultToInject(distributedExecutionIndex1, jsonObject);
        pe2.addFaultToInject(distributedExecutionIndex2, jsonObject);

        assertEquals(pe1, pe2);
    }

    @Test
    public void testSameDistributedExecutionIndexWithSimilarJSONObjectWithFaultsToInject() {
        Callsite callsite = new Callsite("service", "klass", "theMethodName", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex = new DistributedExecutionIndexV1();
        distributedExecutionIndex.push(callsite);

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, new JSONObject());
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, new JSONObject());

        pe1.addFaultToInject(distributedExecutionIndex, new JSONObject());
        pe2.addFaultToInject(distributedExecutionIndex, new JSONObject());

        assertEquals(pe1, pe2);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("a", "b");

        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, jsonObject);
        assertNotEquals(pe1, pe2);
    }

    @Test
    public void testSimilarDistributedExecutionIndexWithSimilarJSONObjectWithFaultsToInject() {
        Callsite callsite = new Callsite("service", "klass", "theMethodName", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex1 = new DistributedExecutionIndexV1();
        distributedExecutionIndex1.push(callsite);

        DistributedExecutionIndex distributedExecutionIndex2 = new DistributedExecutionIndexV1();
        distributedExecutionIndex2.push(callsite);

        assertEquals(distributedExecutionIndex1, distributedExecutionIndex2);

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex1, new JSONObject());
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex2, new JSONObject());

        pe1.addFaultToInject(distributedExecutionIndex1, new JSONObject());
        pe2.addFaultToInject(distributedExecutionIndex2, new JSONObject());

        assertEquals(pe1, pe2);
    }

    @Test
    public void testMultipleSameDistributedExecutionIndexWithSameJSONObjectWithFaultsToInject() {
        Callsite callsite1 = new Callsite("service", "klass", "methodOne", "deadbeef");
        Callsite callsite2 = new Callsite("service", "klass", "methodTwo", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex = new DistributedExecutionIndexV1();
        distributedExecutionIndex.push(callsite1);
        distributedExecutionIndex.push(callsite2);

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        JSONObject jsonObject = new JSONObject();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, jsonObject);
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, jsonObject);

        pe1.addFaultToInject(distributedExecutionIndex, jsonObject);
        pe2.addFaultToInject(distributedExecutionIndex, jsonObject);

        assertEquals(pe1, pe2);
    }

    @Test
    public void testMultipleSameDistributedExecutionIndexWithSimilarJSONObjectWithFaultsToInject() {
        Callsite callsite1 = new Callsite("service", "klass", "methodOne", "deadbeef");
        Callsite callsite2 = new Callsite("service", "klass", "methodTwo", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex = new DistributedExecutionIndexV1();
        distributedExecutionIndex.push(callsite1);
        distributedExecutionIndex.push(callsite2);

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, new JSONObject());
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, new JSONObject());

        pe1.addFaultToInject(distributedExecutionIndex, new JSONObject());
        pe2.addFaultToInject(distributedExecutionIndex, new JSONObject());

        assertEquals(pe1, pe2);
    }

    @Test
    public void testMultipleSimilarDistributedExecutionIndexWithSameJSONObjectWithFaultsToInject() {
        Callsite callsite1 = new Callsite("service", "klass", "methodOne", "deadbeef");
        Callsite callsite2 = new Callsite("service", "klass", "methodTwo", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex1 = new DistributedExecutionIndexV1();
        distributedExecutionIndex1.push(callsite1);
        distributedExecutionIndex1.push(callsite2);

        DistributedExecutionIndex distributedExecutionIndex2 = new DistributedExecutionIndexV1();
        distributedExecutionIndex2.push(callsite1);
        distributedExecutionIndex2.push(callsite2);

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        JSONObject jsonObject = new JSONObject();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex1, jsonObject);
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex2, jsonObject);

        pe1.addFaultToInject(distributedExecutionIndex1, jsonObject);
        pe2.addFaultToInject(distributedExecutionIndex2, jsonObject);

        assertEquals(pe1, pe2);
    }

    @Test
    public void testMultipleSimilarDistributedExecutionIndexWithSimilarJSONObjectWithFaultsToInject() {
        Callsite callsite1 = new Callsite("service", "klass", "methodOne", "deadbeef");
        Callsite callsite2 = new Callsite("service", "klass", "methodTwo", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex1 = new DistributedExecutionIndexV1();
        distributedExecutionIndex1.push(callsite1);
        distributedExecutionIndex1.push(callsite2);

        DistributedExecutionIndex distributedExecutionIndex2 = new DistributedExecutionIndexV1();
        distributedExecutionIndex2.push(callsite1);
        distributedExecutionIndex2.push(callsite2);

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex1, new JSONObject());
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex2, new JSONObject());

        pe1.addFaultToInject(distributedExecutionIndex1, new JSONObject());
        pe2.addFaultToInject(distributedExecutionIndex2, new JSONObject());

        assertEquals(pe1, pe2);
    }

    // Variation with JSON bodies.

    @Test
    public void testSameDistributedExecutionIndexWithSimilarJSONObjectWithBody() {
        Callsite callsite = new Callsite("service", "klass", "theMethodName", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex = new DistributedExecutionIndexV1();
        distributedExecutionIndex.push(callsite);

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put("thing", "thing");
        JSONObject jsonObject2 = new JSONObject();
        jsonObject2.put("thing", "thing");

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, jsonObject1);
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, jsonObject2);
        assertEquals(pe1, pe2);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("a", "b");

        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, jsonObject);
        assertNotEquals(pe1, pe2);
    }

    @Test
    public void testSimilarDistributedExecutionIndexWithSimilarJSONObjectWithBody() {
        Callsite callsite = new Callsite("service", "klass", "theMethodName", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex1 = new DistributedExecutionIndexV1();
        distributedExecutionIndex1.push(callsite);

        DistributedExecutionIndex distributedExecutionIndex2 = new DistributedExecutionIndexV1();
        distributedExecutionIndex2.push(callsite);

        assertEquals(distributedExecutionIndex1, distributedExecutionIndex2);

        AbstractTestExecution pe1 = new AbstractTestExecution();
        AbstractTestExecution pe2 = new AbstractTestExecution();

        JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put("thing", "thing");
        JSONObject jsonObject2 = new JSONObject();
        jsonObject2.put("thing", "thing");

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex1, jsonObject1);
        pe2.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex2, jsonObject2);
        assertEquals(pe1, pe2);
    }
}

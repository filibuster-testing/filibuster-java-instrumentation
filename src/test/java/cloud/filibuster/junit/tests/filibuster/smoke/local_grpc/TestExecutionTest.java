package cloud.filibuster.junit.tests.filibuster.smoke.local_grpc;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.dei.implementations.DistributedExecutionIndexV1;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.junit.server.core.test_executions.PartialTestExecution;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TestExecutionTest {
    @Test
    public void testEqualityOfEmpty() {
        PartialTestExecution pe1 = new PartialTestExecution();
        PartialTestExecution pe2 = new PartialTestExecution();
        assertEquals(pe1, pe2);
    }

    @Test
    public void testSameDistributedExecutionIndexWithSameJSONObject() {
        Callsite callsite = new Callsite("service", "klass", "theMethodName", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex = new DistributedExecutionIndexV1();
        distributedExecutionIndex.push(callsite);

        JSONObject jsonObject = new JSONObject();

        PartialTestExecution pe1 = new PartialTestExecution();
        PartialTestExecution pe2 = new PartialTestExecution();

        pe1.addDistributedExecutionIndexWithPayload(distributedExecutionIndex, jsonObject);
        pe2.addDistributedExecutionIndexWithPayload(distributedExecutionIndex, jsonObject);
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

        PartialTestExecution pe1 = new PartialTestExecution();
        PartialTestExecution pe2 = new PartialTestExecution();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithPayload(distributedExecutionIndex1, new JSONObject());
        pe2.addDistributedExecutionIndexWithPayload(distributedExecutionIndex2, new JSONObject());
        assertEquals(pe1, pe2);
    }

    @Test
    public void testSameDistributedExecutionIndexWithSimilarJSONObject() {
        Callsite callsite = new Callsite("service", "klass", "theMethodName", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex = new DistributedExecutionIndexV1();
        distributedExecutionIndex.push(callsite);

        PartialTestExecution pe1 = new PartialTestExecution();
        PartialTestExecution pe2 = new PartialTestExecution();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithPayload(distributedExecutionIndex, new JSONObject());
        pe2.addDistributedExecutionIndexWithPayload(distributedExecutionIndex, new JSONObject());
        assertEquals(pe1, pe2);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("a", "b");

        pe1.addDistributedExecutionIndexWithPayload(distributedExecutionIndex, jsonObject);
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

        PartialTestExecution pe1 = new PartialTestExecution();
        PartialTestExecution pe2 = new PartialTestExecution();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithPayload(distributedExecutionIndex1, new JSONObject());
        pe2.addDistributedExecutionIndexWithPayload(distributedExecutionIndex2, new JSONObject());
        assertEquals(pe1, pe2);
    }

    @Test
    public void testMultipleSameDistributedExecutionIndexWithSameJSONObject() {
        Callsite callsite1 = new Callsite("service", "klass", "methodOne", "deadbeef");
        Callsite callsite2 = new Callsite("service", "klass", "methodTwo", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex = new DistributedExecutionIndexV1();
        distributedExecutionIndex.push(callsite1);
        distributedExecutionIndex.push(callsite2);

        PartialTestExecution pe1 = new PartialTestExecution();
        PartialTestExecution pe2 = new PartialTestExecution();

        JSONObject jsonObject = new JSONObject();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithPayload(distributedExecutionIndex, jsonObject);
        pe2.addDistributedExecutionIndexWithPayload(distributedExecutionIndex, jsonObject);
        assertEquals(pe1, pe2);
    }

    @Test
    public void testMultipleSameDistributedExecutionIndexWithSimilarJSONObject() {
        Callsite callsite1 = new Callsite("service", "klass", "methodOne", "deadbeef");
        Callsite callsite2 = new Callsite("service", "klass", "methodTwo", "deadbeef");

        DistributedExecutionIndex distributedExecutionIndex = new DistributedExecutionIndexV1();
        distributedExecutionIndex.push(callsite1);
        distributedExecutionIndex.push(callsite2);

        PartialTestExecution pe1 = new PartialTestExecution();
        PartialTestExecution pe2 = new PartialTestExecution();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithPayload(distributedExecutionIndex, new JSONObject());
        pe2.addDistributedExecutionIndexWithPayload(distributedExecutionIndex, new JSONObject());
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

        PartialTestExecution pe1 = new PartialTestExecution();
        PartialTestExecution pe2 = new PartialTestExecution();

        JSONObject jsonObject = new JSONObject();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithPayload(distributedExecutionIndex1, jsonObject);
        pe2.addDistributedExecutionIndexWithPayload(distributedExecutionIndex2, jsonObject);
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

        PartialTestExecution pe1 = new PartialTestExecution();
        PartialTestExecution pe2 = new PartialTestExecution();

        // If this fails, we know the difference has to be in the JSONObject comparison, since the keys are the same.
        pe1.addDistributedExecutionIndexWithPayload(distributedExecutionIndex1, new JSONObject());
        pe2.addDistributedExecutionIndexWithPayload(distributedExecutionIndex2, new JSONObject());
        assertEquals(pe1, pe2);
    }

    // TODO: probably need some negative tests too.
    // TODO: probably need to have json bodies too.
    // TODO: need to also assert faultsToInject and not just the RPC helper.
    // TODO: negative test for DEI?
}

package cloud.filibuster.unit;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.dei.implementations.DistributedExecutionIndexV1;
import cloud.filibuster.exceptions.distributed_execution_index.DistributedExecutionIndexSerializationException;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.CallsiteArguments;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class DistributedExecutionIndexV1Test {
    private static Callsite generateCallsite() {
        return new Callsite("service", "klass", "theMethodName", new CallsiteArguments(Object.class, "deadbeef"));
    }

    private static DistributedExecutionIndex createInstance() {
        return new DistributedExecutionIndexV1();
    }

    private static DistributedExecutionIndex createInstanceFromSerialized(@Nullable String serialized) {
        return new DistributedExecutionIndexV1().deserialize(serialized);
    }

    @Test
    @DisplayName("Test updating an execution index.")
    public void testUpdate() {
        assertDoesNotThrow(() -> {
            DistributedExecutionIndex ei = createInstance();
            ei.push(generateCallsite());
            ei.pop();
        });
    }

    @Test
    @DisplayName("Test cloning an execution index.")
    public void testClone() {
        DistributedExecutionIndex ei1 = createInstance();
        ei1.push(generateCallsite());

        DistributedExecutionIndex ei2 = (DistributedExecutionIndex) ei1.clone();
        assertEquals(ei1.toString(), ei2.toString());
        assertEquals(ei1, ei2);
        ei2.push(generateCallsite());
        assertNotEquals(ei1.toString(), ei2.toString());
        assertNotEquals(ei1, ei2);

        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-56d9b2760d767f5a999ba411f3628cbc5dd47b7c-00aa7adca5809bf3003b7469bdf22140ac380041\", 1]]", ei1.toString());
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-56d9b2760d767f5a999ba411f3628cbc5dd47b7c-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], " + "[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-236aa4fb94f4e4067889dfef9c616b4f44cdb181-00aa7adca5809bf3003b7469bdf22140ac380041\", 1]]", ei2.toString());
    }

    @Test
    @DisplayName("Test double pop of execution index.")
    public void testDoublePop() {
        DistributedExecutionIndex ei = createInstance();

        ei.push(generateCallsite());
        ei.pop();

        assertThrows(IndexOutOfBoundsException.class, ei::pop);

        ei.push(generateCallsite());
        ei.push(generateCallsite());
        ei.pop();
        ei.pop();

        assertThrows(IndexOutOfBoundsException.class, ei::pop);
    }

    @Test
    @DisplayName("Test updating an execution index and toString (single entry.)")
    public void testUpdateToStringSingleEntry() {
        DistributedExecutionIndex ei = createInstance();
        ei.push(generateCallsite());
        String toStringResult = ei.toString();
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-e229408cf2ebeb0cc0b98c2769317e7cb87129c6-00aa7adca5809bf3003b7469bdf22140ac380041\", 1]]", toStringResult);
    }

    @Test
    @DisplayName("Test serialization cycle.")
    public void testSerializeDeserialize() {
        String serializedSingleEntry = "[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-70f63ff27998e4f2e543923f159fd29fbf3b8672-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1]]";
        DistributedExecutionIndex ei1 = createInstanceFromSerialized(serializedSingleEntry);
        assertEquals(serializedSingleEntry, ei1.toString());

        String serializedDoubleEntry = "[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-70f63ff27998e4f2e543923f159fd29fbf3b8672-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-9eb0944bac7a7aca21bc65b1b8f8d1bab8638957-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1]]";
        DistributedExecutionIndex ei2 = createInstanceFromSerialized(serializedDoubleEntry);
        assertEquals(serializedDoubleEntry, ei2.toString());

        assertNotEquals(ei1.toString(), ei2.toString());
    }

    @Test
    @DisplayName("Test malformed serialization cycle")
    public void testMalformedSerializeCycle() {
        String serializedSingleEntry = "[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-70f63ff27998e4f2e543923f159fd29fbf3b8672-f49cf6381e322b147053b74e4500af8533ac1e4c\"]]";
        DistributedExecutionIndex ei1 = createInstanceFromSerialized(serializedSingleEntry);
        assertEquals("[]", ei1.toString());

        String serializedDoubleEntry = "[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-70f63ff27998e4f2e543923f159fd29fbf3b8672-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-9eb0944bac7a7aca21bc65b1b8f8d1bab8638957-f49cf6381e322b147053b74e4500af8533ac1e4c\"]]";
        DistributedExecutionIndex ei2 = createInstanceFromSerialized(serializedDoubleEntry);
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-70f63ff27998e4f2e543923f159fd29fbf3b8672-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1]]", ei2.toString());
    }

    @Test
    @DisplayName("Test updating an execution index and toString (two entries.)")
    public void testUpdateToStringTwoEntries() {
        DistributedExecutionIndex ei = createInstance();
        ei.push(generateCallsite());
        ei.push(generateCallsite());
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-2c9974f8c168d3abde8a06bb45e5b432cd5b8196-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-6978ab33722303fc1bb13aad57b261c92107b548-00aa7adca5809bf3003b7469bdf22140ac380041\", 1]]", ei.toString());
    }

    @Test
    @DisplayName("Test deserialize empty execution index.")
    public void testDeserializeEmptyIndex() {
        assertDoesNotThrow(() -> createInstanceFromSerialized("[]"));
    }

    @Test
    @DisplayName("Test deserialize empty execution string.")
    public void testDeserializeEmptyString() {
        assertDoesNotThrow(() -> createInstanceFromSerialized(""));
    }

    @Test
    @DisplayName("Test deserialize null execution string.")
    public void testDeserializeNullString() {
        assertThrows(DistributedExecutionIndexSerializationException.class, () -> createInstanceFromSerialized(null));
    }

    @Test
    @DisplayName("Test deserialize, push, serialize cycle.")
    public void testDeserializePushSerialize() {
        String startingSerializedExecutionIndex = "[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-a3f941d8eb410bc37b5bde1c55effc134b3afbe8-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-1019228bce1928ade94fc03e5fcbf7f048e6fe9a-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1]]";
        String endingSerializedExecutionIndex = "[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-a3f941d8eb410bc37b5bde1c55effc134b3afbe8-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-1019228bce1928ade94fc03e5fcbf7f048e6fe9a-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-ffb95605c9bfbb90f86d1eebd35c9d3e2410ab1d-00aa7adca5809bf3003b7469bdf22140ac380041\", 1]]";
        DistributedExecutionIndex ei = createInstanceFromSerialized(startingSerializedExecutionIndex);
        assertEquals(startingSerializedExecutionIndex, ei.toString());
        ei.push(generateCallsite());
        assertEquals(endingSerializedExecutionIndex, ei.toString());
    }

    @Test
    @DisplayName("Test push/pop for same callsite to verify hashCode/equals.")
    public void testPushPop() {
        DistributedExecutionIndex ei = createInstance();
        Callsite callsite = generateCallsite();
        ei.push(callsite);
        ei.pop();
        ei.push(callsite);
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-572c339240d2ef65496a1cc48f38bd95c18f2458-eb1e7e8219cff88635d68a832300e07cd75bfebc-00aa7adca5809bf3003b7469bdf22140ac380041\", 2]]", ei.toString());
    }
}





package cloud.filibuster.dei;

import cloud.filibuster.dei.implementations.DistributedExecutionIndexV1;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class DistributedExecutionIndexV1Test {
    private static final String firstServiceIdentifier = "2e6dd8a63bcbb6654503105911cc945c";
    private static final String firstServiceIdentifierExpectedResult = "[[\"" + firstServiceIdentifier + "\", 1]]";
    private static final String secondServiceIdentifier = "c3213ac944f45bcc3d9d89c89a4badca";
    private static final String combinedServiceIdentifierExpectedResult = "[[\"" + firstServiceIdentifier + "\", 1], [\"" + secondServiceIdentifier + "\", 1]]";

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
            ei.push(firstServiceIdentifier);
            ei.pop();
        });
    }

    @Test
    @DisplayName("Test cloning an execution index.")
    public void testClone() {
        DistributedExecutionIndex ei1 = createInstance();
        ei1.push(firstServiceIdentifier);

        DistributedExecutionIndex ei2 = (DistributedExecutionIndex) ei1.clone();
        assertEquals(ei1.toString(), ei2.toString());
        ei2.push(secondServiceIdentifier);
        assertNotEquals(ei1.toString(), ei2.toString());

        assertEquals(firstServiceIdentifierExpectedResult, ei1.toString());
        assertEquals(combinedServiceIdentifierExpectedResult, ei2.toString());
    }

    @Test
    @DisplayName("Test double pop of execution index.")
    public void testDoublePop() {
        DistributedExecutionIndex ei = createInstance();

        ei.push(firstServiceIdentifier);
        ei.pop();

        assertThrows(IndexOutOfBoundsException.class, ei::pop);

        ei.push(firstServiceIdentifier);
        ei.push(firstServiceIdentifier);
        ei.pop();
        ei.pop();

        assertThrows(IndexOutOfBoundsException.class, ei::pop);
    }

    @Test
    @DisplayName("Test updating an execution index and toString (single entry.)")
    public void testUpdateToStringSingleEntry() {
        DistributedExecutionIndex ei = createInstance();
        ei.push(firstServiceIdentifier);
        String toStringResult = ei.toString();
        assertEquals(firstServiceIdentifierExpectedResult, toStringResult);
    }

    @Test
    @DisplayName("Test serialization cycle.")
    public void testSerializeDeserializeSingleEntry() {
        DistributedExecutionIndex ei = createInstanceFromSerialized(firstServiceIdentifierExpectedResult);
        assertEquals(firstServiceIdentifierExpectedResult, ei.toString());
    }

    @Test
    @DisplayName("Test updating an execution index and toString (two entries.)")
    public void testUpdateToStringTwoEntries() {
        DistributedExecutionIndex ei = createInstance();
        ei.push(firstServiceIdentifier);
        ei.push(secondServiceIdentifier);
        assertEquals(combinedServiceIdentifierExpectedResult, ei.toString());
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
        assertThrows(UnsupportedOperationException.class, () -> createInstanceFromSerialized(null));
    }

    @Test
    @DisplayName("Test deserialize, push, serialize cycle.")
    public void testDeserializePushSerialize() {
        String startingSerializedExecutionIndex = "[[\"bbd9da5aaa9b75599aaca2b2d555b6ed\", 1], [\"hello-FilibusterService.java-91\", 1], [\"b4b9c85a52af1621c2ec794727b8eaf5\", 1]]";
        String endingSerializedExecutionIndex = "[[\"bbd9da5aaa9b75599aaca2b2d555b6ed\", 1], [\"hello-FilibusterService.java-91\", 1], [\"b4b9c85a52af1621c2ec794727b8eaf5\", 1], [\"hello-FilibusterService.java-91\", 1]]";
        DistributedExecutionIndex ei = createInstanceFromSerialized(startingSerializedExecutionIndex);
        assertEquals(startingSerializedExecutionIndex, ei.toString());
        ei.push("hello-FilibusterService.java-91");
        assertEquals(endingSerializedExecutionIndex, ei.toString());
    }
}





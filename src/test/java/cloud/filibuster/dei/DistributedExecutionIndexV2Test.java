package cloud.filibuster.dei;

import cloud.filibuster.dei.implementations.DistributedExecutionIndexV2;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DistributedExecutionIndexV2Test {
    private static Callsite generateCallsite() {
        Callsite callsite = new Callsite(
                "IdentityService",
                "IdentityService",
                "IdentityService/getUser",
                "deadbeef");
        return callsite;
    }

    @Test
    public void testGenerateRpcSignatureFromCallsite() {
        String rpcSignature = DistributedExecutionIndexV2.Components.generateRpcSignatureFromCallsite(generateCallsite(), /* shouldCreateDigest= */ false);
        assertEquals("[IdentityService,IdentityService/getUser,()]", rpcSignature);
    }

    @Test
    public void testGenerateRpcSynchronousComponentFromCallsite() {
        String rpcSynchronousComponent = DistributedExecutionIndexV2.Components.generateRpcSynchronousComponentFromCallsite(generateCallsite(), /* shouldCreateDigest= */ false);
        assertEquals("[DistributedExecutionIndexV2Test.java,33,cloud.filibuster.dei.DistributedExecutionIndexV2Test.testGenerateRpcSynchronousComponentFromCallsite(DistributedExecutionIndexV2Test.java:33)]", rpcSynchronousComponent);
    }

    @Test
    public void testGenerateRpcAsynchronousComponentFromCallsite() {
        String rpcAsynchronousComponent = DistributedExecutionIndexV2.Components.generateRpcAsynchronousComponentFromCallsite(generateCallsite(), /* shouldCreateDigest= */ false);
        assertEquals("[deadbeef]", rpcAsynchronousComponent);
    }

    @Test
    public void testCallsiteToDistributedExecutionIndexKeyFromCallsite() {
        String distributedExecutionIndexKey = DistributedExecutionIndexV2.Components.generateDistributedExecutionIndexKey(generateCallsite(), /* shouldCreateDigest= */ false);
        assertEquals("[IdentityService,IdentityService/getUser,()]-[DistributedExecutionIndexV2Test.java,45,cloud.filibuster.dei.DistributedExecutionIndexV2Test.testCallsiteToDistributedExecutionIndexKeyFromCallsite(DistributedExecutionIndexV2Test.java:45)]-[deadbeef]", distributedExecutionIndexKey);
    }

    @Test
    public void testGenerateRpcSignatureFromCallsiteWithDigest() {
        String rpcSignature = DistributedExecutionIndexV2.Components.generateRpcSignatureFromCallsite(generateCallsite(), /* shouldCreateDigest= */ true);
        assertEquals("09dc9ad707b75b40cf9930deb7c9b36fbf62f751", rpcSignature);
    }

    @Test
    public void testGenerateRpcSynchronousComponentFromCallsiteWithDigest() {
        String rpcSynchronousComponent = DistributedExecutionIndexV2.Components.generateRpcSynchronousComponentFromCallsite(generateCallsite(), /* shouldCreateDigest= */ true);
        assertEquals("67b59b82de847153b602309f7789c30bda5bfb6d", rpcSynchronousComponent);
    }

    @Test
    public void testGenerateRpcAsynchronousComponentFromCallsiteWithDigest() {
        String rpcAsynchronousComponent = DistributedExecutionIndexV2.Components.generateRpcAsynchronousComponentFromCallsite(generateCallsite(), /* shouldCreateDigest= */ true);
        assertEquals("f49cf6381e322b147053b74e4500af8533ac1e4c", rpcAsynchronousComponent);
    }

    @Test
    public void testCallsiteToDistributedExecutionIndexKeyFromCallsiteWithDigest() {
        String distributedExecutionIndexKey = DistributedExecutionIndexV2.Components.generateDistributedExecutionIndexKey(generateCallsite(), /* shouldCreateDigest= */ true);
        assertEquals("09dc9ad707b75b40cf9930deb7c9b36fbf62f751-ff1693c53c51a1c9618124e958b9865846514562-f49cf6381e322b147053b74e4500af8533ac1e4c", distributedExecutionIndexKey);
    }

    // New DEI.

    @Test
    public void testOneCallsite() {
        DistributedExecutionIndex ei = createInstance();
        ei.push(generateCallsite());
        assertEquals("[[\"09dc9ad707b75b40cf9930deb7c9b36fbf62f751-20ff4219fa9698dce141f9976325c70005a4ec5d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1]]", ei.toString());
    }

    @Test
    public void testTwoCallsite() {
        DistributedExecutionIndex ei = createInstance();
        ei.push(generateCallsite());
        assertEquals("[[\"09dc9ad707b75b40cf9930deb7c9b36fbf62f751-a5c195451621675da5cc4dad8762332eb58e90e2-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1]]", ei.toString());
        ei.push(generateCallsite());
        assertEquals("[[\"09dc9ad707b75b40cf9930deb7c9b36fbf62f751-a5c195451621675da5cc4dad8762332eb58e90e2-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"09dc9ad707b75b40cf9930deb7c9b36fbf62f751-340e9b2d03a491b7ee4a21286226008dec5d94b0-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1]]", ei.toString());
    }

    @Test
    public void testTwoCallsiteSameCallsite() {
        Callsite callsite = generateCallsite();
        DistributedExecutionIndex ei = createInstance();
        ei.push(callsite);
        assertEquals("[[\"09dc9ad707b75b40cf9930deb7c9b36fbf62f751-bba4a918222fd2215b0165fe5b314e7b89bdba14-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1]]", ei.toString());
        ei.pop();
        ei.push(callsite);
        assertEquals("[[\"09dc9ad707b75b40cf9930deb7c9b36fbf62f751-bba4a918222fd2215b0165fe5b314e7b89bdba14-f49cf6381e322b147053b74e4500af8533ac1e4c\", 2]]", ei.toString());
        DistributedExecutionIndex eiDeserialized = createInstance().deserialize(ei.toString());
        assertEquals(ei.toString(), eiDeserialized.toString());
    }

    // V1 API compatibility.

    private static final String firstServiceIdentifier = "2e6dd8a63bcbb6654503105911cc945c";
    private static final String firstServiceIdentifierExpectedResult = "[[\"" + firstServiceIdentifier + "\", 1]]";
    private static final String secondServiceIdentifier = "c3213ac944f45bcc3d9d89c89a4badca";
    private static final String combinedServiceIdentifierExpectedResult = "[[\"" + firstServiceIdentifier + "\", 1], [\"" + secondServiceIdentifier + "\", 1]]";

    private static DistributedExecutionIndex createInstance() {
        return new DistributedExecutionIndexV2();
    }

    private static DistributedExecutionIndex createInstanceFromSerialized(@Nullable String serialized) {
        return new DistributedExecutionIndexV2().deserialize(serialized);
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





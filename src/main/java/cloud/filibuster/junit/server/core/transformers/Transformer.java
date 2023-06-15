package cloud.filibuster.junit.server.core.transformers;

/**
 * @param <PAYLOAD> Type of the object to be transformed
 * @param <CONTEXT> Type used in the accumulator to identify current iteration (e.g., Integer representing mutated
 *                  idx of mutated char in a string)
 */
public interface Transformer<PAYLOAD, CONTEXT> {
    /**
     * @param payload     Value to be transformed
     * @param accumulator Accumulator of current iteration
     * @return Transformer instance containing the payload after applying the transformation
     */
    Transformer<PAYLOAD, CONTEXT> transform(PAYLOAD payload, Accumulator<PAYLOAD, CONTEXT> accumulator);

    /**
     * @return Whether current iteration has a successor
     */
    boolean hasNext();

    /**
     * @return Class of the payload
     */
    Class<PAYLOAD> getPayloadType();

    /**
     * @return Class of the context
     */
    Class<CONTEXT> getContextType();

    /**
     * @return Payload after applying the transformation.
     * The transform method should be called at least once before this method.
     */
    PAYLOAD getResult();

    /**
     * @return Accumulator used in current transformation
     */
    Accumulator<PAYLOAD, CONTEXT> getAccumulator();

    /**
     * @return Initial accumulator used in the first transformation
     */
    Accumulator<PAYLOAD, CONTEXT> getInitialAccumulator();

    /**
     * @return Accumulator of next transformation
     */
    Accumulator<PAYLOAD, CONTEXT> getNextAccumulator();
}
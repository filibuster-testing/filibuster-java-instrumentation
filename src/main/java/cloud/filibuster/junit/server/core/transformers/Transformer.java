package cloud.filibuster.junit.server.core.transformers;

import java.lang.reflect.Type;

/**
 * Returns a generic transformer interface that can be implemented in type-specific classes (e.g., StringTransformer).
 *
 * @param <PAYLOAD> Type of the object to be transformed
 * @param <CONTEXT> Type used in the accumulator to identify current iteration (e.g., Integer representing idx
 *                  of mutated char in a string)
 */
public interface Transformer<PAYLOAD, CONTEXT> {
    /**
     * Returns transformer instance containing the payload after applying the transformation.
     *
     * @param payload     Value to be transformed
     * @param accumulator Accumulator of current iteration
     * @return Transformer
     */
    Transformer<PAYLOAD, CONTEXT> transform(PAYLOAD payload, Accumulator<PAYLOAD, CONTEXT> accumulator);

    /**
     * Returns whether current iteration has a successor.
     *
     * @return boolean
     */
    boolean hasNext();

    /**
     * Returns Type of the payload.
     *
     * @return Type of the payload
     */
    Type getPayloadType();

    /**
     * Returns Type representation of the accumulator.
     * This method is used to serialize the accumulator in Gson.
     *
     * @return Type representation of the accumulator
     */
    Type getAccumulatorType();

    /**
     * Returns payload after applying the transformation.
     * The transform method should be called at least once before this method.
     *
     * @return Payload
     */
    PAYLOAD getResult();

    /**
     * Returns initial accumulator used in the first transformation.
     *
     * @return Initial accumulator
     */
    Accumulator<PAYLOAD, CONTEXT> getInitialAccumulator();

    /**
     * Returns accumulator of next transformation.
     *
     * @return Generic accumulator
     */
    Accumulator<PAYLOAD, CONTEXT> getNextAccumulator();
}
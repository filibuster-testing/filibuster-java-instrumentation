package cloud.filibuster.instrumentation.datatypes;

import cloud.filibuster.exceptions.vector_clock.VectorClockCloneException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.Iterator;

/**
 * Vector Clock.
 */
public class VectorClock implements Cloneable {
    private JSONObject obj;

    /**
     * Build a new vector clock.
     */
    public VectorClock() {
        obj = new JSONObject();
    }

    @Override
    public VectorClock clone() {
        VectorClock newVectorClock;

        try {
            newVectorClock = (VectorClock) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new VectorClockCloneException("cloning not supported for vector clock", e);
        }

        // Deep clone member fields here
        newVectorClock.obj = new JSONObject(this.obj.toString());

        return newVectorClock;
    }

    /**
     * Get a value from the vector clock.
     *
     * @param key actor identifier.
     * @return integer representing the number of actions for that actor.
     */
    public int get(String key) {
        if (obj.has(key)) {
            return obj.getInt(key);
        } else {
            return 0;
        }
    }

    /**
     * Increment the vector clock for an actor.
     *
     * @param key actor identifier.
     */
    public void incrementClock(String key) {
        // Get current value.
        int currentValue = 0;

        if (obj.has(key)) {
            currentValue = obj.getInt(key);
        }

        // Increment clock.
        int newValue = currentValue + 1;

        // Replace value.
        obj.remove(key);
        obj.put(key, newValue);
    }

    /**
     * Serialize a vector clock as JSON.
     *
     * @return serialized vector clock.
     */
    public JSONObject toJSONObject() { return obj; }

    /**
     * Serialize a vector clock as a string.
     *
     * @return serialized vector clock.
     */
    @Override
    public String toString() {
        return obj.toString();
    }

    /**
     * Deserialize a vector clock.
     *
     * @param jsonString serialized vector clock.
     */
    public void fromString(String jsonString) {
        obj = new JSONObject(jsonString);
    }

    /**
     * Merge two vector clocks and return a merged vector clock.
     *
     * @param vc1 vector clock.
     * @param vc2 vector clock.
     * @return merge vector clock.
     */
    public static VectorClock merge(VectorClock vc1, VectorClock vc2) {
        VectorClock vc = new VectorClock();

        for (Iterator<String> it = vc1.obj.keys(); it.hasNext(); ) {
            String key = it.next();
            vc.obj.put(key, vc1.obj.getInt(key));
        }

        for (Iterator<String> it = vc2.obj.keys(); it.hasNext(); ) {
            String key = it.next();

            if (vc.obj.has(key)) {
                int currentValue = vc.obj.getInt(key);
                int newValue = Integer.max(currentValue, vc2.obj.getInt(key));
                vc.obj.put(key, newValue);
            } else {
                vc.obj.put(key, vc2.obj.get(key));
            }
        }

        return vc;
    }

    /**
     * Determines if one vector clock descends from another.
     *
     * @param vc1 vector clock.
     * @param vc2 vector clock.
     * @return boolean if vc2 descends vc1.
     */
    public static boolean descends(@Nullable VectorClock vc1, @Nullable VectorClock vc2) {
        boolean atLeastOneKeyGreater = false;

        if (vc1 == null) {
            return vc2 != null;
        }

        if (vc2 == null) {
            return false;
        }

        for (String key: vc1.obj.keySet()) {
            // vc2 has to have at least all the keys vc1 has.
            if (!vc2.obj.has(key)) {
                return false;
            } else {
                // vc2's value has to be equal or greater.
                if (vc2.obj.getInt(key) < vc1.obj.getInt(key)) {
                    return false;
                } else {
                    // keep track of whether it's greater.
                    if (vc2.obj.getInt(key) > vc1.obj.getInt(key)) {
                        atLeastOneKeyGreater = true;
                    }
                }
            }
        }

        // if not, vc2 has to have at least one new key.
        // otherwise, we aren't.
        if (atLeastOneKeyGreater) {
            // if we've seen all the same events and at least one more event
            // from those participants, then we're good.
            return true;
        } else {
            return vc2.obj.length() > vc1.obj.length();
        }
    }
}

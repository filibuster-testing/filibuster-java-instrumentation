package cloud.filibuster.dei;

import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class DistributedExecutionIndexBase implements Cloneable {
    protected HashMap<DistributedExecutionIndexKey, Integer> counters = new HashMap<>();
    protected ArrayList<Map.Entry<DistributedExecutionIndexKey, Integer>> callstack = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DistributedExecutionIndexBase)) {
            return false;
        }
        DistributedExecutionIndexBase dei = (DistributedExecutionIndexBase) o;
        return Objects.equals(this.counters, dei.counters) && Objects.equals(this.callstack, dei.callstack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.counters, this.callstack);
    }

    public void push(Callsite callsite) {
        DistributedExecutionIndex dei = (DistributedExecutionIndex) this;
        DistributedExecutionIndexKey key = dei.convertCallsiteToDistributedExecutionIndexKey(callsite);

        int currentValue = 0;

        if(counters.containsKey(key)) {
            currentValue = counters.get(key);
            counters.replace(key, currentValue + 1);
        } else {
            counters.put(key, currentValue + 1);
        }

        callstack.add(Pair.of(key, currentValue + 1));
    }

    public void pop() {
        int lastIndex = callstack.size() - 1;
        callstack.remove(lastIndex);
    }

    public DistributedExecutionIndex deserialize(String serialized) {
        if (serialized == null) {
            throw new UnsupportedOperationException();
        }

        if (serialized.equals("")) {
            return (DistributedExecutionIndex) this;
        }

        String normalized = serialized.substring(1, serialized.length() - 1);
        String[] parts = normalized.split(",");
        String key = null;

        for (int counter = 0; counter < parts.length; counter++) {
            if (!parts[0].isEmpty()) {
                if (key == null) {
                    if (counter == 0) {
                        // Remove beginning bracket and quote.
                        key = parts[counter].substring(2);
                    }

                    if (counter > 0) {
                        // Remove leading space, beginning bracket and quote.
                        key = parts[counter].substring(3);
                    }

                    // Remove end quote.
                    key = key.substring(0, key.length() - 1);
                } else {
                    // Remove end quote.
                    String value = parts[counter].substring(0, parts[counter].length() - 1);
                    // Remove leading space.
                    value = value.substring(1);
                    callstack.add(Pair.of(DistributedExecutionIndexKey.deserialize(key), Integer.valueOf(value)));
                    key = null;
                }
            }
        }

        return (DistributedExecutionIndex) this;
    }

    private String serialize() {
        ArrayList<String> entryList = new ArrayList<>();

        for (Map.Entry<DistributedExecutionIndexKey, Integer> stringIntegerEntry : callstack) {
            ArrayList<String> innerOutput = new ArrayList<>();
            innerOutput.add("[\"");
            innerOutput.add(stringIntegerEntry.getKey().serialize());
            innerOutput.add("\", ");
            innerOutput.add(String.valueOf(stringIntegerEntry.getValue()));
            innerOutput.add("]");
            String innerOutputString = String.join("", innerOutput);
            entryList.add(innerOutputString);
        }

        return "[" + String.join(", ", entryList) + "]";
    }

    @Override
    public Object clone() {
        DistributedExecutionIndexBase newDistributedExecutionIndex;

        try {
            newDistributedExecutionIndex = (DistributedExecutionIndexBase) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new UnsupportedOperationException(e);
        }

        // Deep clone member fields here
        newDistributedExecutionIndex.counters = new HashMap<>(this.counters);
        newDistributedExecutionIndex.callstack = new ArrayList<>(this.callstack);

        return newDistributedExecutionIndex;
    }

    @Override
    public String toString() {
        return serialize();
    }
}

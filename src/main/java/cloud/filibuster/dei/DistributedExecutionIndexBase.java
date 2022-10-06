package cloud.filibuster.dei;

import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class DistributedExecutionIndexBase implements Cloneable {
    protected HashMap<String, Integer> counters = new HashMap<>();
    protected ArrayList<Map.Entry<String, Integer>> callstack = new ArrayList<>();

    public void push(Callsite callsite) {
        DistributedExecutionIndex dei = (DistributedExecutionIndex) this;
        String serializedCallsite = dei.serializeCallsite(callsite);
        push(serializedCallsite);
    }

    public void push(String entry) {
        int currentValue = 0;

        if(counters.containsKey(entry)) {
            currentValue = counters.get(entry);
            counters.replace(entry, currentValue + 1);
        } else {
            counters.put(entry, currentValue + 1);
        }

        callstack.add(Pair.of(entry, currentValue + 1));
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
                    callstack.add(Pair.of(key, Integer.valueOf(value)));
                    key = null;
                }
            }
        }

        return (DistributedExecutionIndex) this;
    }

    private String serialize() {
        ArrayList<String> entryList = new ArrayList<>();

        for (Map.Entry<String, Integer> stringIntegerEntry : callstack) {
            ArrayList<String> innerOutput = new ArrayList<>();
            innerOutput.add("[\"");
            innerOutput.add(stringIntegerEntry.getKey());
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

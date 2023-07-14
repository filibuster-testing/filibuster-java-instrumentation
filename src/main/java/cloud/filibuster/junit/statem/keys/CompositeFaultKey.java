package cloud.filibuster.junit.statem.keys;

import cloud.filibuster.junit.statem.CompositeFaultSpecification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CompositeFaultKey implements FaultKey {
    private List<SingleFaultKey> faultKeys = new ArrayList<>();

    public CompositeFaultKey(CompositeFaultSpecification compositeFaultSpecification) {
        this.faultKeys = compositeFaultSpecification.getFaultKeys();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CompositeFaultKey)) {
            return false;
        }

        CompositeFaultKey cfk = (CompositeFaultKey) other;

        if (this.faultKeys.size() != cfk.faultKeys.size()) {
            return false;
        }

        for (SingleFaultKey faultKey : this.faultKeys) {
            if (!cfk.faultKeys.contains(faultKey)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder toStringResult = new StringBuilder();

        for (SingleFaultKey faultKey : this.faultKeys) {
            toStringResult.append(faultKey.toString());
        }

        return toStringResult.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.toString());
    }
}

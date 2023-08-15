package cloud.filibuster.junit.statem.keys;

import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestInternalRuntimeException;
import cloud.filibuster.junit.statem.CompositeFaultSpecification;
import io.grpc.Status;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CompositeFaultKey implements FaultKey {
    private final List<SingleFaultKey> faultKeys;

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

    @Override
    public int size() {
        return this.faultKeys.size();
    }

    @Nullable
    public static FaultKey findMatchingFaultKey(
            Map<FaultKey, Runnable> assertionsByFaultKey,
            List<JSONObject> rpcsWhereFaultsInjected
    ) {
        List<List<SingleFaultKey>> rpcsWhereFaultsInjectedFaultKeys = rpcsWhereFaultsInjected.stream()
                .map(SingleFaultKey::generateFaultKeysInDecreasingGranularity)
                .collect(Collectors.toList());

        // Populate starting list of keys that match.
        List<FaultKey> previousIterationMatching = new ArrayList<>();

        for (Map.Entry<FaultKey, Runnable> assertionByFaultKey : assertionsByFaultKey.entrySet()) {
            previousIterationMatching.add(assertionByFaultKey.getKey());
        }

        List<FaultKey> iterationMatching = performIterableMatching(previousIterationMatching, rpcsWhereFaultsInjectedFaultKeys, rpcsWhereFaultsInjected);

        // Return the key or null.
        // (Throw if there are too many found, which is probably a bug.)
        if (iterationMatching.size() == 1) {
            return iterationMatching.get(0);
        } else if (iterationMatching.size() > 1) {
            throw new FilibusterGrpcTestInternalRuntimeException("iterationMatching.size() > 1; this indicates there is a problem!");
        } else {
            return null;
        }
    }

    @Nullable
    public static List<FaultKey> findMatchingFaultKeys(
            Map<FaultKey, Map.Entry<Status.Code, String>> assertionsByFaultKey,
            List<JSONObject> rpcsWhereFaultsInjected,
            Status.Code code
    ) {
        List<List<SingleFaultKey>> rpcsWhereFaultsInjectedFaultKeys = rpcsWhereFaultsInjected.stream()
                .map(SingleFaultKey::generateFaultKeysInDecreasingGranularity)
                .collect(Collectors.toList());

        // Populate starting list of keys that match.
        List<FaultKey> previousIterationMatching = new ArrayList<>();

        for (Map.Entry<FaultKey, Map.Entry<Status.Code, String>> assertionByFaultKey : assertionsByFaultKey.entrySet()) {
            if (assertionByFaultKey.getValue().getKey().equals(code)) {
                previousIterationMatching.add(assertionByFaultKey.getKey());
            }
        }

        return performIterableMatching(previousIterationMatching, rpcsWhereFaultsInjectedFaultKeys, rpcsWhereFaultsInjected);
    }

    public static List<FaultKey> performIterableMatching(
            List<FaultKey> previousIterationMatching,
            List<List<SingleFaultKey>> rpcsWhereFaultsInjectedFaultKeys,
            List<JSONObject> rpcsWhereFaultsInjected
    ) {
        List<FaultKey> iterationMatching;

        // These are each component of the composite key -- each fault and all the ways it can be represented.
        for (List<SingleFaultKey> singleFaultKeys : rpcsWhereFaultsInjectedFaultKeys) {
            iterationMatching = new ArrayList<>();

            // These are each individual key, sorted in granularity order.
            for (SingleFaultKey singleFaultKey : singleFaultKeys) {
                String sSingleFaultKey = singleFaultKey.toString();

                // These are each of the faults we have behavior for.
                for (FaultKey assertionByFaultKey : previousIterationMatching) {
                    if (assertionByFaultKey.toString().contains(sSingleFaultKey)) {
                        iterationMatching.add(assertionByFaultKey);
                    }
                }
            }

            previousIterationMatching = new ArrayList<>(iterationMatching);
        }

        // Eliminate any superset keys, that contain *more* than the specified faults.
        iterationMatching = new ArrayList<>();

        for (FaultKey faultKey : previousIterationMatching) {
            if (faultKey.size() == rpcsWhereFaultsInjected.size()) {
                iterationMatching.add(faultKey);
            }
        }

        return iterationMatching;
    }
}

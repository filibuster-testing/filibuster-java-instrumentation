package cloud.filibuster.junit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterDefaultAnalysisConfigurationFile;
import cloud.filibuster.junit.exceptions.NoopException;
import cloud.filibuster.junit.extensions.FilibusterTestExtension;

import cloud.filibuster.junit.server.FilibusterServerBackend;
import cloud.filibuster.junit.server.backends.FilibusterDockerServerBackend;
import org.apiguardian.api.API;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * Filibuster test annotation for JUnit 5.
 *
 * <p>This annotation can be used to automatically enable a JUnit5 test, annotated with a {@link Test} annotation for use
 * with Filibuster.
 *
 * <p>Performs the following lifecycle operations:
 * <ul>
 *     <li>Start the Filibuster server process (using the Filibuster Python server.)</li>
 *     <li>Communicate with the server to provide conditional assertions and test generation automatically with JUnit.</li>
 *     <li>Terminate the Filibuster server after execution of the test.</li>
 * </ul>
 *
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = API.Status.STABLE, since = "5.0")
@ExtendWith(FilibusterTestExtension.class)
@TestTemplate
@Isolated
public @interface FilibusterTest {
    /**
     * Placeholder variable for the test name.
     */
    String DISPLAY_NAME_PLACEHOLDER = "{displayName}";

    /**
     * Placeholder variable for current iteration.
     */
    String CURRENT_ITERATION_PLACEHOLDER = "{currentIteration}";

    /**
     * Placeholder variable for total (max) iterations.
     */
    String TOTAL_ITERATIONS_PLACEHOLDER = "{totalIterations}";

    /**
     * Placeholder name used for generated tests.
     */
    String SHORT_DISPLAY_NAME = "Filibuster generated test #" + CURRENT_ITERATION_PLACEHOLDER; // + " of #" + TOTAL_ITERATIONS_PLACEHOLDER + "";

    /**
     * Name of the generated test.
     *
     * @return name of the generated test.
     */
    String name() default SHORT_DISPLAY_NAME;

    /**
     * Maximum number of tests that will be run by Filibuster.
     *
     * <p>
     * JUnit5 doesn't allow for the dynamic generation of tests as tests are executed: the stream of test executions
     * must be materialized before execution starts.  Therefore, set an upper bound on the number of tests that will be run.
     * </p>
     *
     * <p>Details:
     *     <ul>
     *         <li>Test execution 1 always runs: this is the fault-free execution.</li>
     *         <li>Test execution n conditionally runs, if necessary, based on communication with Filibuster server.  It will conditionally invoke the beforeEach/afterEach *only* if the test is necessary.</li>
     *         <li>Test execution (maxIteration + 1) runs to perform finalization of the test process, but bypasses beforeEach/afterEach, and test body.  Therefore, it appears to run, is shown as a test in IntelliJ, but doesn't execute the test.</li>
     *     </ul>
     *
     * <p>Caveats:
     *     <ul>
     *         <li>If maxIteration is not set large enough, Filibuster will fail to explore the entire failure space.</li>
     *     </ul>
     *
     * @return upper bound on possible Filibuster tests that will be run.
     */
    int maxIterations() default 99;

    /**
     * Should this configuration use dynamic reduction?
     *
     * <p>
     * Dynamic reduction incurs overhead and is useless unless testing graphs where depth greater than 2 will be explored.
     * This probably is not the case for most functional tests that use mocks, stubs, or similar and is only useful
     * when standing up deep graphs of multiple services.
     *
     * @return whether dynamic reduction should be used.
     */
    boolean dynamicReduction() default false;

    /**
     * Should this configuration avoid exploring combinations of faults?
     *
     * @return whether combinations of faults will be suppressed from test.
     */
    boolean suppressCombinations() default false;

    /**
     * Does this test configuration contain data nondeterminism?
     *
     * <p>Specifically, this only refers to whether the RPC messages can be affected across executions
     * from data nondeterminism.
     *
     * <p>Use of this option prevents optimizations for dealing with scheduling nondeterminism, as accounting for data
     * nondeterminism effectively prevents dealing with scheduling nondeterminism by omitting invocation details specific
     * to a particular invocation of an RPC.
     *
     * <p>For reference:
     * <ul>
     * <li>Data nondeterminism: use of timestamps, unique identifiers that differ each time the test is run.
     * <li>Scheduling nondeterminism: loops that issue multiple RPCs to the same RPC method on the same service that differ only by message contents.
     * </ul>
     *
     * @return whether data nondeterminism is present in the test.
     */
    boolean dataNondeterminism() default false;

    /**
     * Analysis file that should be used for this configuration of Filibuster.
     *
     * <p>When supplied, {@code analysisConfigurationFile()} is ignored.
     *
     * <p>When not specified, a default analysis file will be generated to test common faults for Google's gRPC.
     * (i.e., DEADLINE_EXCEEDED, UNAVAILABLE)
     *
     * @return absolute path of the analysis file to use.
     */
    String analysisFile() default "";

    /**
     * Analysis configuration file for Filibuster.
     *
     * <p>Will be written out to a file and supplied as a command line argument to the Filibuster server.
     *
     * <p>When supplied, will be used to override the default analysis.  When {@code analysisFile()} used, it is ignored.
     *
     * @return custom analysis configuration file.
     */
    Class<? extends FilibusterAnalysisConfigurationFile> analysisConfigurationFile() default FilibusterDefaultAnalysisConfigurationFile.class;

    /**
     * Server backend to use for Filibuster.
     *
     * @return server backend module.
     */
    Class<? extends FilibusterServerBackend> serverBackend() default FilibusterDockerServerBackend.class;

    /**
     * Docker image to use for the Docker server backend.
     *
     * @return string of image name
     */
    String dockerImageName() default "";

    /**
     * If the Filibuster server is unavailable, should the system degrade to just running fault-free tests?
     *
     * @return boolean
     */
    boolean degradeWhenServerInitializationFails() default false;

    /**
     * Whether we expect this all generated tests to throw this exception.
     *
     * @return throwable or runtime exception
     */
    Class<? extends RuntimeException> expected() default NoopException.class;
}
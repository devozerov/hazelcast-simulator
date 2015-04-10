package com.hazelcast.simulator.tests.slow;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.map.MapInterceptor;
import com.hazelcast.simulator.test.TestContext;
import com.hazelcast.simulator.test.TestRunner;
import com.hazelcast.simulator.test.annotations.RunWithWorker;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.test.annotations.Teardown;
import com.hazelcast.simulator.test.annotations.Verify;
import com.hazelcast.simulator.test.annotations.Warmup;
import com.hazelcast.simulator.tests.helpers.KeyLocality;
import com.hazelcast.simulator.worker.selector.OperationSelectorBuilder;
import com.hazelcast.simulator.worker.tasks.AbstractWorker;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static com.hazelcast.simulator.tests.helpers.HazelcastTestUtils.getOperationCountInformation;
import static com.hazelcast.simulator.tests.helpers.HazelcastTestUtils.getOperationService;
import static com.hazelcast.simulator.tests.helpers.HazelcastTestUtils.waitClusterSize;
import static com.hazelcast.simulator.tests.helpers.KeyUtils.generateIntKeys;
import static com.hazelcast.simulator.utils.CommonUtils.sleepSeconds;
import static com.hazelcast.simulator.utils.ReflectionUtils.getObjectFromField;
import static com.hazelcast.simulator.utils.TestUtils.assertEqualsStringFormat;
import static java.lang.String.format;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This test invokes slowed down map operations on a Hazelcast instance to provoke slow operation logs.
 *
 * In the verification phase we check for the correct number of slow operation logs (one per operation type).
 *
 * @since Hazelcast 3.5
 */
public class SlowOperationMapTest {

    private static final ILogger LOGGER = Logger.getLogger(SlowOperationMapTest.class);

    private enum Operation {
        PUT,
        GET
    }

    // properties
    public String basename = SlowOperationMapTest.class.getSimpleName();
    public int keyLength = 10;
    public int valueLength = 10;
    public int keyCount = 100;
    public int valueCount = 100;
    public KeyLocality keyLocality = KeyLocality.RANDOM;
    public int minNumberOfMembers = 0;
    public double putProb = 0.5;
    public int recursionDepth = 10;

    private final OperationSelectorBuilder<Operation> operationSelectorBuilder = new OperationSelectorBuilder<Operation>();
    private final AtomicLong putCounter = new AtomicLong(0);
    private final AtomicLong getCounter = new AtomicLong(0);

    private HazelcastInstance hazelcastInstance;
    private IMap<Integer, Integer> map;
    private Object slowOperationDetector;
    private int[] keys;

    @Setup
    public void setUp(TestContext testContext) throws Exception {
        hazelcastInstance = testContext.getTargetInstance();
        map = hazelcastInstance.getMap(basename);

        operationSelectorBuilder
                .addOperation(Operation.PUT, putProb)
                .addDefaultOperation(Operation.GET);

        // try to find the slowOperationDetector instance (since Hazelcast 3.5)
        slowOperationDetector = getObjectFromField(getOperationService(hazelcastInstance), "slowOperationDetector");
        if (slowOperationDetector == null) {
            fail("This test needs Hazelcast 3.5 or newer");
        }
    }

    @Teardown
    public void tearDown() throws Exception {
        map.destroy();
        LOGGER.info(getOperationCountInformation(hazelcastInstance));
    }

    @Warmup(global = false)
    public void warmup() throws InterruptedException {
        waitClusterSize(LOGGER, hazelcastInstance, minNumberOfMembers);
        keys = generateIntKeys(keyCount, Integer.MAX_VALUE, keyLocality, hazelcastInstance);

        Random random = new Random();
        for (int key : keys) {
            int value = random.nextInt(Integer.MAX_VALUE);
            map.put(key, value);
        }

        // add the interceptor after the warmup, otherwise this stage will take ages
        map.addInterceptor(new SlowMapInterceptor(recursionDepth));
    }

    @Verify(global = true)
    public void verify() throws Exception {
        long putCount = putCounter.get();
        long getCount = getCounter.get();
        long operationCount = putCount + getCount;
        assertTrue("Expected at least one completed operations, but was " + operationCount, operationCount > 0);

        Map<Integer, Object> slowOperationLogs = getObjectFromField(slowOperationDetector, "slowOperationLogs");
        if (slowOperationLogs == null) {
            fail("Could not retrieve slow operation logs");
        }

        int actual = slowOperationLogs.size();
        int expected = (int) (Math.min(putCount, 1) + Math.min(getCount, 1));
        LOGGER.info(format("Found %d/%d slow operation logs after completing %d operations (%d put, %d get).",
                actual, expected, operationCount, putCount, getCount));

        assertEqualsStringFormat("Expected %d slow operation logs, but was %d", expected, actual);
    }

    @RunWithWorker
    public Worker createWorker() {
        return new Worker();
    }

    private class Worker extends AbstractWorker<Operation> {

        public Worker() {
            super(operationSelectorBuilder);
        }

        @Override
        protected void timeStep(Operation operation) {
            int key = randomKey();

            switch (operation) {
                case PUT:
                    map.put(key, randomValue());
                    putCounter.incrementAndGet();
                    break;
                case GET:
                    map.get(key);
                    getCounter.incrementAndGet();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        private int randomKey() {
            return keys[randomInt(keys.length)];
        }

        private int randomValue() {
            return randomInt(Integer.MAX_VALUE);
        }
    }

    private static class SlowMapInterceptor implements MapInterceptor {
        private final int recursionDepth;

        public SlowMapInterceptor(int recursionDepth) {
            this.recursionDepth = recursionDepth;
        }

        @Override
        public Object interceptGet(Object value) {
            return null;
        }

        @Override
        public void afterGet(Object value) {
            sleepRecursion(recursionDepth, 15);
        }

        @Override
        public Object interceptPut(Object oldValue, Object newValue) {
            return null;
        }

        @Override
        public void afterPut(Object value) {
            sleepRecursion(recursionDepth, 20);
        }

        @Override
        public Object interceptRemove(Object removedValue) {
            return null;
        }

        @Override
        public void afterRemove(Object removedValue) {
        }

        private void sleepRecursion(int recursionDepth, int sleepSeconds) {
            if (recursionDepth == 0) {
                sleepSeconds(sleepSeconds);
                return;
            }
            sleepRecursion(recursionDepth - 1, sleepSeconds);
        }
    }

    public static void main(String[] args) throws Throwable {
        SlowOperationMapTest test = new SlowOperationMapTest();
        new TestRunner<SlowOperationMapTest>(test).run();
    }
}

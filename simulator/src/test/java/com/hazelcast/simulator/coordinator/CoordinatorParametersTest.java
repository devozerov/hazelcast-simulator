package com.hazelcast.simulator.coordinator;

import com.hazelcast.simulator.common.SimulatorProperties;
import com.hazelcast.simulator.protocol.registry.TargetType;
import org.junit.Test;

import static com.hazelcast.simulator.testcontainer.TestPhase.LOCAL_TEARDOWN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class CoordinatorParametersTest {

    @Test
    public void testConstructor() {
        SimulatorProperties properties = mock(SimulatorProperties.class);

        CoordinatorParameters coordinatorParameters = new CoordinatorParameters(
                null,
                properties,
                "workerClassPath",
                false,
                true,
                TargetType.PREFER_CLIENT,
                5,
                LOCAL_TEARDOWN,
                0,
                true,
                null);

        assertEquals(properties, coordinatorParameters.getSimulatorProperties());
        assertEquals("workerClassPath", coordinatorParameters.getWorkerClassPath());
        assertFalse(coordinatorParameters.isVerifyEnabled());
        assertTrue(coordinatorParameters.isParallel());
        assertEquals(TargetType.CLIENT, coordinatorParameters.getTargetType(true));
        assertEquals(TargetType.MEMBER, coordinatorParameters.getTargetType(false));
        assertEquals(5, coordinatorParameters.getTargetCount());
        assertEquals(LOCAL_TEARDOWN, coordinatorParameters.getLastTestPhaseToSync());
    }
}

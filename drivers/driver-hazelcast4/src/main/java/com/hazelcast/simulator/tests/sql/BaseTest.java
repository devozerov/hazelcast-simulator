package com.hazelcast.simulator.tests.sql;

import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.IndexType;
import com.hazelcast.config.MapConfig;
import com.hazelcast.map.IMap;
import com.hazelcast.simulator.hz.HazelcastTest;
import com.hazelcast.simulator.test.annotations.Prepare;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.test.annotations.Teardown;
import com.hazelcast.simulator.worker.loadsupport.Streamer;
import com.hazelcast.simulator.worker.loadsupport.StreamerFactory;

public abstract class BaseTest extends HazelcastTest {

    public int count = 10_000;
    public String format = InMemoryFormat.BINARY.name();

    protected IMap<Long, Object> map;
    protected long[] keys;
    protected SqlPerson[] values;

    @Setup
    public void setup() {
        try {
            Class.forName("com.hazelcast.sql.impl.client.SqlPageMarker");
        } catch (Exception e) {
            throw new IllegalStateException("The new HZ 4.2 is not in the classpath");
        }

        if (count < 100) {
            throw new IllegalArgumentException("count must be greater than 100");
        }

        InMemoryFormat format0 = InMemoryFormat.valueOf(format);

        if (format0 != InMemoryFormat.BINARY) {
            targetInstance.getConfig().addMapConfig(new MapConfig(name).setInMemoryFormat(format0));
        }

        map = targetInstance.getMap(name);
        map.addIndex(IndexType.SORTED, "id_indexed");

        keys = new long[count];
        values = new SqlPerson[count];

        for (int i = 0; i < values.length; i++) {
            keys[i] = i;
            values[i] = new SqlPerson(i, i, "first-" + i, "last-" + i);
        }

        setup0();
    }

    protected void setup0() {
        // No-op
    }

    @Prepare
    public void prepare() {
        Streamer<Long, Object> streamer = StreamerFactory.getInstance(map);

        for (int i = 0; i < keys.length; i++) {
            streamer.pushEntry(keys[i], values[i]);
        }

        streamer.await();
    }

    @Teardown
    public void tearDown() {
        map.destroy();
    }
}

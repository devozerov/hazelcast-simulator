package com.hazelcast.simulator.tests.sql;

import com.hazelcast.simulator.test.BaseThreadState;
import com.hazelcast.simulator.test.annotations.TimeStep;
import com.hazelcast.sql.SqlResult;
import com.hazelcast.sql.SqlRow;
import com.hazelcast.sql.SqlService;

public class SqlValueTest extends BaseTest {

    private SqlService sqlService;

    @Override
    protected void setup0() {
        sqlService = targetInstance.getSql();
    }

    @TimeStep(prob = 0.0d)
    public void scanAll(ThreadState state) {
        execute(
            count,
            "SELECT this FROM " + name
        );
    }

    @TimeStep(prob = 0.0d)
    public void scan1(ThreadState state) {
        execute(
            1,
            "SELECT this FROM " + name + " WHERE id=?",
            state.randomKey()
        );
    }

    @TimeStep(prob = 0.0d)
    public void scan100(ThreadState state) {
        long start = state.randomKey100();
        long end = start + 100;

        execute(
            100,
            "SELECT this FROM " + name + " WHERE id>=? AND id<?",
            start,
            end
        );
    }

    @TimeStep(prob = 0.0d)
    public void index1(ThreadState state) {
        execute(
            1,
            "SELECT this FROM " + name + " WHERE id_indexed=?",
            state.randomKey()
        );
    }

    @TimeStep(prob = 0.0d)
    public void index100(ThreadState state) {
        long start = state.randomKey100();
        long end = start + 100;

        execute(
            100,
            "SELECT this FROM " + name + " WHERE id_indexed>=? AND id_indexed<?",
            start,
            end
        );
    }

    private void execute(int expected, String sql, Object... params) {
        int actual = 0;

        try (SqlResult result = sqlService.execute(sql, params)) {
            for (SqlRow row : result) {
                Object value = row.getObject(0);

                if (!(value instanceof SqlPerson)) {
                    throw new IllegalStateException("Returned object is not " + SqlPerson.class.getSimpleName() + ": " + value);
                }

                actual++;
            }
        }

        if (actual != expected) {
            throw new IllegalArgumentException("Invalid count [expected=" + expected + ", actual=" + actual + "]");
        }
    }

    public class ThreadState extends BaseThreadState {
        private long randomKey() {
            return keys[randomInt(keys.length)];
        }

        private long randomKey100() {
            return keys[randomInt(keys.length - 100)];
        }
    }
}

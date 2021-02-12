package com.hazelcast.simulator.tests.sql;

import com.hazelcast.query.Predicates;
import com.hazelcast.simulator.test.BaseThreadState;
import com.hazelcast.simulator.test.annotations.TimeStep;

import java.util.Collection;

public class PredicateValueTest extends BaseTest {
    @TimeStep(prob = 0.0d)
    public void scanAll(ThreadState state) {
        execute(
            map.values(),
            count
        );
    }

    @TimeStep(prob = 0.0d)
    public void scan1(ThreadState state) {
        execute(
            map.values(Predicates.equal("id", state.randomKey())),
            1
        );
    }

    @TimeStep(prob = 0.0d)
    public void scan100(ThreadState state) {
        long start = state.randomKey100();
        long end = start + 100;

        execute(
            map.values(Predicates.and(Predicates.greaterEqual("id", start), Predicates.lessThan("id", end))),
            100
        );
    }

    @TimeStep(prob = 0.0d)
    public void index1(ThreadState state) {
        execute(
            map.values(Predicates.equal("id_indexed", state.randomKey())),
            1
        );
    }

    @TimeStep(prob = 0.0d)
    public void index100(ThreadState state) {
        long start = state.randomKey100();
        long end = start + 100;

        execute(
            map.values(Predicates.and(Predicates.greaterEqual("id_indexed", start), Predicates.lessThan("id_indexed", end))),
            100
        );
    }

    private <V> void execute(Collection<V> values, int expected) {
        int actual = 0;

        for (V value : values) {
            if (!(value instanceof SqlPerson)) {
                throw new IllegalStateException("Returned object is not " + SqlPerson.class.getSimpleName() + ": " + value);
            }

            actual++;
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

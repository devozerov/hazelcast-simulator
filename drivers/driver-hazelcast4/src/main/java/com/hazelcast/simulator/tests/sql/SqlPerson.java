package com.hazelcast.simulator.tests.sql;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

import java.io.IOException;

public class SqlPerson implements DataSerializable {

    public long id;
    public long id_indexed;
    public String first_name;
    public String last_name;

    public SqlPerson() {
        // No-op
    }

    public SqlPerson(long id, long id_indexed, String first_name, String last_name) {
        this.id = id;
        this.id_indexed = id_indexed;
        this.first_name = first_name;
        this.last_name = last_name;
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeLong(id);
        out.writeLong(id_indexed);
        out.writeUTF(first_name);
        out.writeUTF(last_name);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        id = in.readLong();
        id_indexed = in.readLong();
        first_name = in.readUTF();
        last_name = in.readUTF();
    }
}

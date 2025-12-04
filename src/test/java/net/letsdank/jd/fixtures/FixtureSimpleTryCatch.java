package net.letsdank.jd.fixtures;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;

public class FixtureSimpleTryCatch {

    public int readIntOrMinusOne(DataInput in) throws IOException {
        try {
            return in.readInt();
        } catch (EOFException e) {
            return -1;
        }
    }
}

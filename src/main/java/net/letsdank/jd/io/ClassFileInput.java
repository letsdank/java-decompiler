package net.letsdank.jd.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Обертка над InputStream для чтения unsigned значений
 * из class-файла.
 */
public final class ClassFileInput implements AutoCloseable {
    private final InputStream in;

    public ClassFileInput(InputStream in) {
        this.in = in;
    }

    public int readU1() throws IOException {
        int b = in.read();
        if (b == -1) {
            throw new IOException("Unexpected EOF while reading u1");
        }
        return b & 0xFF;
    }

    public int readU2() throws IOException {
        int hi = readU1();
        int lo = readU1();
        return (hi << 8) | lo;
    }

    public long readU4() throws IOException {
        long b1 = readU1();
        long b2 = readU1();
        long b3 = readU1();
        long b4 = readU1();
        return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
    }

    @Override
    public void close() throws Exception {
        in.close();
    }
}

package net.letsdank.jd.model;

/**
 * Минимальное представление class-файла.
 */
public final class ClassFile {
    private final int minorVersion;
    private final int majorVersion;

    public ClassFile(int minorVersion, int majorVersion) {
        this.minorVersion = minorVersion;
        this.majorVersion = majorVersion;
    }

    public int minorVersion() {
        return minorVersion;
    }

    public int majorVersion() {
        return majorVersion;
    }

    @Override
    public String toString() {
        return "ClassFile{" +
                "minorVersion=" + minorVersion +
                ", majorVersion=" + majorVersion +
                '}';
    }
}
